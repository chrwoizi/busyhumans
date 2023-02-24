package com.c5000.mongopa

import com.mongodb.client.MongoDatabase
import internal._

import java.lang.reflect.Modifier
import scala.collection.JavaConverters._
import collection.mutable.{Buffer, HashMap}
import com.mongodb.client.model.{Filters, UpdateOptions, Updates}
import com.mongodb.{BasicDBList, BasicDBObject, MongoClientSettings}
import org.bson.{BsonDocument, BsonDocumentReader, BsonDocumentWriter, Document}
import org.bson.codecs.{Codec, DecoderContext, EncoderContext}
import org.bson.codecs.configuration.CodecRegistries
import org.bson.codecs.pojo.PojoCodecProvider
import org.bson.conversions.Bson
import org.bson.types.Binary
import org.slf4j.LoggerFactory

import java.nio.ByteBuffer
import java.util
import java.util.UUID
import scala.collection.convert.ImplicitConversions.`map AsScala`
import scala.reflect.ClassTag
import scala.reflect._

/**
 * Persists POJOs in a Cassandra database.
 * Only public non-transient fields will be persisted.
 * An optional Column-Annotation can be used to define a custom column name for a field.
 *
 * You must set the public db field and register the object types before you can persist objects.
 */
class Repository {

    private val log = LoggerFactory.getLogger(getClass)
    private var db: MongoDatabase = null
    private var classDescs = new HashMap[Class[_], ClassDesc]
    private var codecRegistry: org.bson.codecs.configuration.CodecRegistry = null

    def this(db: MongoDatabase, classes: Iterable[Class[_]]) {
        this()
        this.db = db
        classes.foreach(register(_))

        val pojoCodecProvider = PojoCodecProvider.builder().automatic(true).build();
        this.codecRegistry = CodecRegistries.fromRegistries(MongoClientSettings.getDefaultCodecRegistry(), CodecRegistries.fromProviders(pojoCodecProvider))
    }

    /**
     * Preloads the public field types of a class for better performance of the persistence methods.
     */
    private def register(clazz: Class[_]) {
        val cdesc: ClassDesc = new ClassDesc
        cdesc.clazz = clazz
        val columnFamily: MpaModel = clazz.getAnnotation(classOf[MpaModel])
        cdesc.dbCollection = columnFamily.collection()
        val columns = Buffer[FieldDesc]()
        for (field <- ReflectionHelper.getAllFields(clazz)) {
            val fieldModifiers = field.getModifiers
            val setter = field.getDeclaringClass.getMethod(field.getName)
            val setterModifiers = if (setter != null) setter.getModifiers else 0
            if (fieldModifiers == Modifier.PUBLIC || setterModifiers == Modifier.PUBLIC) {
                val ignore = field.getAnnotation(classOf[MpaIgnore]) != null
                if (!ignore) {
                    val fdesc: FieldDesc = new FieldDesc
                    fdesc.field = field
                    fdesc.getter = ReflectionHelper.findGetter(field)
                    fdesc.setter = ReflectionHelper.findSetter(field)
                    if (field.getAnnotation(classOf[MpaKey]) != null) {
                        cdesc.key = fdesc
                    }
                    if (field.getAnnotation(classOf[MpaDeletedFlag]) != null) {
                        if (!(field.getType == classOf[Boolean])) throw new RuntimeException("Cannot use the field " + cdesc.clazz.getName + "." + fdesc.field.getName + " with type " + fdesc.field.getType + " as a deleted-flag. Define the field as boolean or remove the DeleteFlag annotation. ")
                        cdesc.deletedFlag = fdesc
                    }
                    val columnAnnotation: MpaField = field.getAnnotation(classOf[MpaField])
                    if (columnAnnotation != null) {
                        fdesc.dbField = columnAnnotation.value
                    }
                    else {
                        fdesc.dbField = field.getName
                    }
                    val listAnnotation: MpaListField = field.getAnnotation(classOf[MpaListField])
                    if (listAnnotation != null) {
                        if (!classOf[MpaList[_]].isAssignableFrom(field.getType))
                            throw new RuntimeException("Field " + field.getName + " has the " + classOf[MpaListField].getName + " annotation but is not of type " + classOf[MpaList[_]].getName)
                        fdesc.listItemType = listAnnotation.itemType
                    }
                    else {
                        if (classOf[MpaList[_]].isAssignableFrom(field.getType))
                            throw new RuntimeException("Field " + field.getName + " must have the " + classOf[MpaListField].getName + " annotation")
                    }
                    fdesc.isIterable = classOf[Iterable[_]].isAssignableFrom(field.getType)
                    columns.asJava.add(fdesc)
                }
            }
        }
        cdesc.columns = columns.toList
        classDescs.put(clazz, cdesc)
    }

    private def getClassDesc(clazz: Class[_]): ClassDesc = {
        if (db == null) throw new RuntimeException("DB not set.")
        val cdesc = classDescs.get(clazz)
        if (cdesc.isEmpty) throw new RuntimeException("Class " + clazz.getName + " has not been registered.")
        return cdesc.get
    }

    /**
     * Saves an object of a registered class.
     */
    def save[Entity <: Object](entity: Entity) {
        val cdesc: ClassDesc = getClassDesc(entity.getClass)
        if (cdesc.dbCollection.isEmpty)
            throw new RuntimeException("Class " + cdesc.clazz.getName + " cannot be saved because the DB collection name is undefined")

        val obj = convertToDbObject(entity, cdesc)
        val key = ReflectionHelper.getFieldValue(cdesc.key, entity)
        val q = Filters.eq(cdesc.key.dbField, key)
        val options = new UpdateOptions();
        options.upsert(true);
        db.getCollection(cdesc.dbCollection).updateOne(q, new BasicDBObject("$set", obj), options)
    }

    /**
     * Loads an object of a registered class.
     *
     * @param clazz          class of the entity to load
     * @param includeDeleted true: the entity will be loaded even if it's deleted flag is set
     */
    def load[Key, Entity <: Object](clazz: Class[Entity], key: Key, includeDeleted: Boolean = false): Entity = {
        if (key == null) return null.asInstanceOf[Entity]
        log.trace("loading " + clazz.getName + " " + key.toString)
        try {
            val cdesc: ClassDesc = getClassDesc(clazz)
            if (cdesc.dbCollection.isEmpty)
                throw new RuntimeException("Class " + cdesc.clazz.getName + " cannot be loaded because the DB collection name is undefined")

            val q = Filters.eq(cdesc.key.dbField, key)
            val dbObj = db.getCollection(cdesc.dbCollection).find(q).first()
            if (dbObj == null) return null.asInstanceOf[Entity]

            if (!includeDeleted) {
                if (cdesc.deletedFlag != null) {
                    val deleted = dbObj.get(cdesc.deletedFlag.dbField).asInstanceOf[Boolean]
                    if (deleted) return null.asInstanceOf[Entity]
                }
            }

            return convertToEntity[Entity](dbObj, cdesc)
        }
        catch {
            case e: InstantiationException => {
                try {
                    return clazz.newInstance()
                    throw new RuntimeException("Error while instantiating " + clazz.getName, e)
                }
                catch {
                    case e1: SecurityException => {
                        throw new RuntimeException("Cannot access parameterless constructor of " + clazz.getName, e1)
                    }
                    case e1: NoSuchMethodException => {
                        throw new RuntimeException("No parameterless constructor found for " + clazz.getName, e1)
                    }
                }
            }
            case e: NoSuchElementException => {
                return null.asInstanceOf[Entity]
            }
        }
    }

    /**
     * returns true if there is at least one column in the column family for the given key.
     *
     * @param clazz          type of the entity
     * @param includeDeleted true: the entity will be loaded even if it's deleted flag is set
     */
    def exists[Key, Entity <: Object](clazz: Class[Entity], key: Key, includeDeleted: Boolean = false): Boolean = {
        val cdesc: ClassDesc = getClassDesc(clazz)
        val q = Filters.eq(cdesc.key.dbField, key)
        val dBObj = db.getCollection(cdesc.dbCollection).find(q).first()
        if (dBObj != null) {
            if (!includeDeleted) {
                if (cdesc.deletedFlag != null) {
                    val deleted = dBObj.get(cdesc.deletedFlag.dbField).asInstanceOf[Boolean]
                    if (deleted) return false
                }
            }
            return true
        }
        else return false
    }

    /**
     * Runs an action for each element in the database
     * @param clazz type of the entity
     * @param action action to run for each element
     * @param includeDeleted true: the action will also run for deleted entities
     */
    def forEach[Entity <: Object](clazz: Class[Entity], includeDeleted: Boolean = false)(action: Entity => Unit) {
        val cdesc: ClassDesc = getClassDesc(clazz)
        db.getCollection(cdesc.dbCollection).find().asScala.foreach(dbObj => {
            var continue = false
            if (!includeDeleted) {
                if (cdesc.deletedFlag != null) {
                    val deleted = dbObj.get(cdesc.deletedFlag.dbField).asInstanceOf[Boolean]
                    if (deleted) continue = true
                }
            }
            if (!continue) {
                val entity: Entity = convertToEntity[Entity](dbObj, cdesc)
                if (entity != null)
                    action(entity)
            }
        })
    }

    def loadAndSaveAll[Entity <: Object](clazz: Class[Entity]) {
        val cdesc: ClassDesc = getClassDesc(clazz)
        db.getCollection(cdesc.dbCollection).find().asScala.foreach(dbObj => {
            val entity: Entity = convertToEntity[Entity](dbObj, cdesc)
            val obj = convertToDbObject(entity, cdesc)
            val q = Filters.eq("_id", dbObj.get("_id"))
            db.getCollection(cdesc.dbCollection).updateOne(q, new BasicDBObject("$set", obj))
        })
    }

    def getType(fdesc: FieldDesc): Class[_] = {
        if (fdesc.getter != null) {
            return fdesc.getter.getReturnType
        }
        else {
            return fdesc.field.getType
        }
    }

    private def convertToDbObject[Entity <: Object](entity: Entity, cdesc: ClassDesc): BasicDBObject = {
        val builder = new BasicDBObject()
        for (fdesc <- cdesc.columns) {
            val value = ReflectionHelper.getFieldValue(fdesc, entity)
            if (fdesc.isIterable && value != null) {
                val list: BasicDBList = new BasicDBList()
                for (item <- value.asInstanceOf[Iterable[_ <: Object]]) {
                    list.asScala += item
                }
                builder.addOne(fdesc.dbField, list)
            }
            else if (fdesc.listItemType != null && value != null) {
                val itemClassDesc = getClassDesc(fdesc.listItemType)
                val list = new BasicDBList()
                for (item <- value.asInstanceOf[MpaList[_ <: Object]].allItems) {
                    list.asScala += convertToDbObject(item, itemClassDesc)
                }
                builder.addOne(fdesc.dbField, list)
            }
            else if (classDescs.get(getType(fdesc)).isDefined) {
                if (value != null) {
                    val inner = convertToDbObject(value, getClassDesc(getType(fdesc)))
                    builder.addOne(fdesc.dbField, inner)
                }
                else {
                    builder.addOne(fdesc.dbField, null)
                }
            }
            else {
                builder.addOne(fdesc.dbField, value)
            }
        }
        return builder
    }

    private def convertToEntity[Entity <: Object](dbObj: Document, cdesc: ClassDesc): Entity = {
        val entity = cdesc.clazz.newInstance.asInstanceOf[Entity]
        cdesc.columns.foreach(fdesc => {
            val value = dbObj.get(fdesc.dbField)
            if (value != null && value.isInstanceOf[Binary]) {
                ReflectionHelper.setFieldValue(fdesc, entity, UUID.nameUUIDFromBytes(value.asInstanceOf[Binary].getData))
            }
            else if (fdesc.listItemType != null && value != null) {
                val itemClassDesc = getClassDesc(fdesc.listItemType)
                val list = Buffer[Object]()
                for (item <- value.asInstanceOf[util.ArrayList[Document]].asScala) {
                    val itemEntity = convertToEntity[Object](item.asInstanceOf[Document], itemClassDesc)
                    if (itemEntity != null)
                        list += itemEntity
                }
                ReflectionHelper.setFieldValue(fdesc, entity, new MpaList[MpaModelBase](list.asInstanceOf[Iterable[MpaModelBase]]))
            }
            else if (fdesc.isIterable && value != null) {
                val iterable: Iterable[_] = value.asInstanceOf[util.ArrayList[_]].asScala
                ReflectionHelper.setFieldValue(fdesc, entity, iterable)
            }
            else if (classDescs.get(getType(fdesc)).isDefined) {
                if (value != null) {
                    val inner: Object = convertToEntity(value.asInstanceOf[Document], getClassDesc(getType(fdesc)))
                    ReflectionHelper.setFieldValue(fdesc, entity, inner)
                }
                else {
                    ReflectionHelper.setFieldValue(fdesc, entity, null)
                }
            }
            else {
                if(value != null) {
                    log.trace("set field " + fdesc.field.getName + " of type " + getType(fdesc).toString() + " to value " + value.toString + " of type " + value.getClass);
                    ReflectionHelper.setFieldValue(fdesc, entity, value)
                }
            }
        })
        return entity
    }

    def convertToDbObject[Entity <: Object](entity: Entity): Bson = {
        val cdesc = getClassDesc(entity.getClass)
        return convertToDbObject(entity, cdesc)
    }

    def convertToEntity[Entity <: Object](dbObj: Document, entityClass: Class[Entity]): Entity = {
        val cdesc = getClassDesc(entityClass)
        return convertToEntity(dbObj, cdesc)
    }
}