package com.c5000.mastery.database

import models._
import org.reflections.Reflections
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import com.c5000.mongopa.{MpaModel, MpaModelBase, Repository}
import com.mongodb.{MongoClientSettings, MongoCredential, ServerAddress}
import org.bson.conversions.Bson

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream, OutputStream, PipedInputStream, PipedOutputStream}
import java.util.UUID
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoDatabase
import com.mongodb.client.gridfs.{GridFSBucket, GridFSBuckets}
import com.mongodb.client.gridfs.model.{GridFSFile, GridFSUploadOptions}
import com.mongodb.client.model.Filters
import com.mongodb.connection.ClusterSettings
import org.bson.{Document, UuidRepresentation}

import collection.mutable
import scala.reflect.ClassTag

object Database {

    private val log = LoggerFactory.getLogger(getClass)

    private val HOST = System.getenv("DATABASE_HOST")
    private val PORT = Integer.parseInt(System.getenv("DATABASE_PORT"))
    private val NAME = System.getenv("DATABASE_NAME")
    private val USER = System.getenv("DATABASE_USER")
    private val PASS = System.getenv("DATABASE_PASSWORD")

    private var connection: MongoClient = null
    private var db: MongoDatabase = null
    private val knownColumnFamilies = mutable.Buffer[String]()
    private var models: mutable.Set[Class[_]] = null

    private var repository: Repository = null

    def mongodb = db

    def init() {
        log.info("Initializing the database at " + this.USER + ":(" + this.PASS.length + " characters)@" + this.HOST + ":" + this.PORT + "/" + this.NAME)

        connection = MongoClients.create(
            MongoClientSettings.builder()
              .applyToClusterSettings((builder: ClusterSettings.Builder) => builder.hosts(List(new ServerAddress(HOST, PORT)).asJava))
              .credential(MongoCredential.createCredential(USER, NAME, PASS.toCharArray))
              .uuidRepresentation(UuidRepresentation.STANDARD)
              .build())
        db = connection.getDatabase(NAME)

        val packageName = classOf[UniqueIdModelBase].getPackage.getName
        log.trace("Searching for models in " + packageName)
        val reflections = new Reflections(packageName)
        models = reflections.getTypesAnnotatedWith(classOf[MpaModel]).asScala
        log.trace("Found " + models.toArray.length + " models")
        models.foreach(it => {
            val anno = it.getAnnotation(classOf[MpaModel])
            val collection = anno.collection
            if (!collection.isEmpty) {
                log.trace("initializing " + it.getName + "...")
                assertCollection(collection)
            }
            else {
                log.trace("not initializing " + it.getName + " because it has no MpaModel/collection attribute")
            }
        })
        log.trace("All models initialized")

        repository = new Repository(db, models)

        //Database.loadAndSaveAll(classOf[AccountM])
        //Database.loadAndSaveAll(classOf[ActivityM])
        //Database.loadAndSaveAll(classOf[AnnouncementM])
        //Database.loadAndSaveAll(classOf[AssignmentM])
        //Database.loadAndSaveAll(classOf[CityM])
        //Database.loadAndSaveAll(classOf[NotificationM])
        //Database.loadAndSaveAll(classOf[PersonM])
        //Database.loadAndSaveAll(classOf[SkillM])
        //Database.loadAndSaveAll(classOf[SubscriptionM])

        log.info("Database initialized.")
    }

    def assertCollection(name: String) {
        if (!knownColumnFamilies.contains(name))
            knownColumnFamilies += name
        val collection = db.getCollection(name)
        if (collection == null) {
            throw new RuntimeException("Could not create Collection '" + name + "'")
        }
        log.trace("collection " + name + " exists and has " + collection.countDocuments() + " documents")
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
    def convertToDbObject[Entity <: MpaModelBase](entity: Entity): Bson = {
        return repository.convertToDbObject(entity)
    }

    def convertMongoFile(gridfs: GridFSBucket, file: GridFSFile, os: OutputStream): DatabaseFile = {
        val filename = file.getFilename
        val result = new DatabaseFile
        result.id = UUID.fromString(filename.substring(0, 36))
        result.part = if (filename.length > 36) filename.substring(37) else null
        if(os != null) {
            log.trace("Downloading file from DB to stream " + filename + " " + result.id + " " + result.part)
            gridfs.downloadToStream(filename, os)
        };
        result.size = file.getLength.asInstanceOf[Int]
        result.contentType = file.getMetadata().getString("type")
        return result
    }

    def filename(id: UUID, part: String): String = {
        if(part != null)
            return id.toString + "-" + part
        else
            return id.toString
    }

    def saveFile(id: UUID, part: String, fileStream: InputStream, contentType: String) {
        val gridfs = GridFSBuckets.create(Database.mongodb)
        val options = new GridFSUploadOptions().metadata(new Document("type", contentType))
        gridfs.uploadFromStream(filename(id, part), fileStream, options)
    }

    def loadFile(id: UUID, part: String, os: OutputStream): DatabaseFile = {
        val gridfs = GridFSBuckets.create(Database.mongodb)
        val result = gridfs.find(Filters.eq("filename", filename(id, part))).first()
        if (result != null) {
            return convertMongoFile(gridfs, result, os)
        }
        return null
    }

    def deleteFile(id: UUID, part: String) {
        val gridfs = GridFSBuckets.create(Database.mongodb)
        val result = gridfs.find(Filters.eq("filename", filename(id, part))).first()
        if (result != null) {
            gridfs.delete(result.getObjectId)
        }
    }

    def forEachFile[Entity <: Object](includeData: Boolean)(action: (DatabaseFile, InputStream) => Unit) {
        val gridfs = GridFSBuckets.create(Database.mongodb)
        gridfs.find().asScala.foreach(file => {
            val actualFile = gridfs.find(Filters.eq("filename", file.getFilename)).first()

            if(includeData) {
                val os = new ByteArrayOutputStream()
                val fileMeta = convertMongoFile(gridfs, actualFile, os)
                action(fileMeta, new ByteArrayInputStream(os.toByteArray))
            }
            else {
                val fileMeta = convertMongoFile(gridfs, actualFile, null)
                action(fileMeta, null)
            }
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

    def loadAndSaveAll[Entity <: Object](clazz: Class[Entity]): Unit = {
        repository.loadAndSaveAll[Entity](clazz)
    }

}