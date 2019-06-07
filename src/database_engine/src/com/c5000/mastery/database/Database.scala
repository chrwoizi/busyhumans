package com.c5000.mastery.database

import models.UniqueIdModelBase
import org.reflections.Reflections
import org.slf4j.LoggerFactory
import scala.collection.JavaConversions._
import com.mongodb.casbah.{MongoDB, MongoConnection}
import com.mongodb.{DBCursor, DBObject}
import com.c5000.mongopa.{MpaModelBase, MpaModel, Repository}
import java.io.InputStream
import java.util.UUID
import com.mongodb.casbah.gridfs.{GridFSDBFile, GridFS}
import collection.mutable
import com.mongodb.casbah.commons.MongoDBObject

object Database {

    private val log = LoggerFactory.getLogger(getClass)

    private val HOST = "localhost"
    private val PORT = 27017
    private val NAME = "mastery"
    private val USER = "mastery-app"
    private val PASS = "***REMOVED***"

    private var connection: MongoConnection = null
    private var db: MongoDB = null
    private val knownColumnFamilies = mutable.Buffer[String]()
    private var models: Set[Class[_]] = null

    private var repository: Repository = null

    def mongodb = db

    def init() {
        log.info("Initializing the database...")

        connection = MongoConnection(HOST, PORT)
        db = connection.getDB(NAME)

        if (!db.isAuthenticated) {
            if (!db.authenticate(USER, PASS)) {
                throw new RuntimeException("Could not authenticate at database.")
            }
        }
        else {
            log.warn("No authentication needed for the database.")
        }

        val reflections = new Reflections(classOf[UniqueIdModelBase].getPackage.getName)
        models = reflections.getTypesAnnotatedWith(classOf[MpaModel]).toSet
        models.foreach(it => {
            log.trace("initializing " + it.getName + "...")
            val anno = it.getAnnotation(classOf[MpaModel])
            val collection = anno.collection
            if (!collection.isEmpty)
                assertCollection(collection)
        })

        repository = new Repository(db, models)

        log.info("Database initialized.")
    }

    def assertCollection(name: String) {
        if (!knownColumnFamilies.contains(name))
            knownColumnFamilies += name
        if (db.getCollection(name) != null) return
        throw new RuntimeException("Could not create Collection '" + name + "'")
    }

    /**
     * Saves an object of a registered class.
     */
    def save[Entity <: Object](entity: Entity) {
        repository.save(entity)
    }

    /**
     * Loads an object of a registered class.
     *
     * @param clazz          class of the entity to load
     * @param includeDeleted true: the entity will be loaded even if it's deleted flag is set
     */
    def load[Key, Entity <: Object](clazz: Class[Entity], key: Key, includeDeleted: Boolean = false): Entity = {
        return repository.load(clazz, key, includeDeleted)
    }

    /**
     * returns true if there is at least one column in the column family for the given key.
     *
     * @param clazz          type of the entity
     * @param includeDeleted true: the entity will be loaded even if it's deleted flag is set
     */
    def exists[Key, Entity <: Object](clazz: Class[Entity], key: Key, includeDeleted: Boolean = false): Boolean = {
        return repository.exists(clazz, key, includeDeleted)
    }

    /**
     * converts an entity to a database object
     *
     * @param entity         the entity
     */
    def convertToDbObject[Entity <: MpaModelBase](entity: Entity): DBObject = {
        return repository.convertToDbObject(entity)
    }

    def convertMongoFile(file: GridFSDBFile): DatabaseFile = {
        val filename = file.underlying.getFilename
        val result = new DatabaseFile
        result.id = UUID.fromString(filename.substring(0, 36))
        result.part = if (filename.length > 36) filename.substring(37) else null
        result.stream = file.inputStream
        result.size = file.underlying.getLength.asInstanceOf[Int]
        result.contentType = file.underlying.getContentType
        return result
    }

    def filename(id: UUID, part: String): String = {
        if(part != null)
            return id.toString + "-" + part
        else
            return id.toString
    }

    def saveFile(id: UUID, part: String, fileStream: InputStream, contentType: String) {
        val gridfs = GridFS(Database.mongodb)
        gridfs(fileStream) {
            fh =>
                fh.filename = filename(id, part)
                fh.contentType = contentType
        }
    }

    def loadFile(id: UUID, part: String): Option[DatabaseFile] = {
        val gridfs = GridFS(Database.mongodb)
        val result = gridfs.findOne(filename(id, part))
        if (result.isDefined) {
            return Some(convertMongoFile(result.get))
        }
        return None
    }

    def deleteFile(id: UUID, part: String) {
        val gridfs = GridFS(Database.mongodb)
        gridfs.remove(filename(id, part))
    }

    def forEachFile[Entity <: Object]()(action: DatabaseFile => Unit) {
        val gridfs = GridFS(Database.mongodb)
        gridfs.foreach(file => {
            val actualFile = gridfs.find(file.underlying.getFilename).head
            action(convertMongoFile(actualFile))
        })
    }

    /**
     * Runs an action for each element in the database
     * @param clazz type of the entity
     * @param action action to run for each element
     * @param includeDeleted true: the action will also run for deleted entities
     */
    def forEach[Entity <: Object](clazz: Class[Entity], includeDeleted: Boolean = false)(action: Entity => Unit) {
        repository.forEach(clazz, includeDeleted) {
            action(_)
        }
    }
}