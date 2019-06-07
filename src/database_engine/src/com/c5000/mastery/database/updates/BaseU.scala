package com.c5000.mastery.database.updates

import com.mongodb.DBObject
import com.c5000.mastery.database.Database
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.casbah._
import com.c5000.mastery.database.models.UniqueIdModelBase
import com.c5000.mongopa.{MpaModelBase, MpaModel}
import java.util.UUID

object BaseU {

    def set(collectionName: String, query: DBObject, field: String, value: Any): Boolean = {
        val collection = Database.mongodb.getCollection(collectionName)
        val setter = MongoDBObject("$set" -> MongoDBObject(field -> value))
        return collection.updateMulti(query, setter.asInstanceOf[DBObject]).getN > 0
    }

    def setMulti(collectionName: String, query: DBObject, values: DBObject): Boolean = {
        val collection = Database.mongodb.getCollection(collectionName)
        val setter = MongoDBObject("$set" -> values)
        collection.update(query, setter.asInstanceOf[DBObject])
        return collection.updateMulti(query, setter.asInstanceOf[DBObject]).getN > 0
    }

    def increment(collectionName: String, query: DBObject, fieldName: String, delta: Any): Boolean = {
        val collection = Database.mongodb.getCollection(collectionName)
        val setter = MongoDBObject("$inc" -> MongoDBObject(fieldName -> delta))
        return collection.updateMulti(query, setter.asInstanceOf[DBObject]).getN > 0
    }

    def setDeleted(model: UniqueIdModelBase): Boolean = {
        val collectionName = model.getClass.getAnnotation(classOf[MpaModel]).collection()
        val query = MongoDBObject("id" -> model.id).asInstanceOf[DBObject]
        if(set(collectionName, query, "deleted", true)) {
            model.deleted = true
            return true
        }
        return false
    }

    def add(collectionName: String, query: DBObject, arrayField: String, newItem: MpaModelBase): Boolean = {
        val collection = Database.mongodb.getCollection(collectionName)
        val modelObj = Database.convertToDbObject(newItem)
        val setter = MongoDBObject("$push" -> MongoDBObject(arrayField -> modelObj))
        return collection.updateMulti(query, setter.asInstanceOf[DBObject]).getN > 0
    }

}

class BaseU(clazz: Class[_]) {

    protected val dbCollection = clazz.getAnnotation(classOf[MpaModel]).collection()

    protected def setMulti(query: DBObject, values: Any): Boolean = {
        return BaseU.setMulti(dbCollection, query, values.asInstanceOf[DBObject])
    }

    protected def set(query: DBObject, field: String, value: Any): Boolean = {
        return BaseU.set(dbCollection, query, field, value)
    }

    protected def increment(query: DBObject, fieldName: String, delta: Any): Boolean = {
        return BaseU.increment(dbCollection, query, fieldName, delta)
    }

    protected def addItem(query: DBObject, arrayField: String, newItem: MpaModelBase): Boolean = {
        return BaseU.add(dbCollection, query, arrayField, newItem)
    }

    protected def makeQuery(id: UUID): DBObject = MongoDBObject("id" -> id, "deleted" -> false).asInstanceOf[DBObject]

    protected def makeQuery(id: UUID, query: Any): DBObject = {
        return $and (
            MongoDBObject("id" -> id, "deleted" -> false),
            query.asInstanceOf[DBObject]
        ).asInstanceOf[DBObject]
    }

}
