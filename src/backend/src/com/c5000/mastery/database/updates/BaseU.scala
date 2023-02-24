package com.c5000.mastery.database.updates

import com.c5000.mastery.database.Database
import com.c5000.mastery.database.models.UniqueIdModelBase
import com.c5000.mongopa.{MpaModel, MpaModelBase}
import com.mongodb.client.model.{Filters, Updates}
import org.bson.conversions.Bson

import java.util.UUID

object BaseU {

    def set(collectionName: String, query: Bson, field: String, value: Any): Boolean = {
        val collection = Database.mongodb.getCollection(collectionName)
        val setter = Updates.set(field, value)
        return collection.updateMany(query, setter).getModifiedCount > 0
    }

    def setMulti(collectionName: String, query: Bson, setter: Bson): Boolean = {
        val collection = Database.mongodb.getCollection(collectionName)
        return collection.updateMany(query, setter).getModifiedCount > 0
    }

    def increment(collectionName: String, query: Bson, fieldName: String, delta: Number): Boolean = {
        val collection = Database.mongodb.getCollection(collectionName)
        val setter = Updates.inc(fieldName, delta)
        return collection.updateMany(query, setter).getModifiedCount > 0
    }

    def setDeleted(model: UniqueIdModelBase): Boolean = {
        val collectionName = model.getClass.getAnnotation(classOf[MpaModel]).collection()
        val query = Filters.eq("id", model.id)
        if(set(collectionName, query, "deleted", true)) {
            model.deleted = true
            return true
        }
        return false
    }

    def add(collectionName: String, query: Bson, arrayField: String, newItem: MpaModelBase): Boolean = {
        val collection = Database.mongodb.getCollection(collectionName)
        val modelObj = Database.convertToDbObject(newItem)
        val setter = Updates.push(arrayField, modelObj)
        return collection.updateMany(query, setter).getModifiedCount > 0
    }

}

class BaseU(clazz: Class[_]) {

    protected val dbCollection = clazz.getAnnotation(classOf[MpaModel]).collection()

    protected def setMulti(query: Bson, values: Bson): Boolean = {
        return BaseU.setMulti(dbCollection, query, values)
    }

    protected def set(query: Bson, field: String, value: Any): Boolean = {
        return BaseU.set(dbCollection, query, field, value)
    }

    protected def increment(query: Bson, fieldName: String, delta: Number): Boolean = {
        return BaseU.increment(dbCollection, query, fieldName, delta)
    }

    protected def addItem(query: Bson, arrayField: String, newItem: MpaModelBase): Boolean = {
        return BaseU.add(dbCollection, query, arrayField, newItem)
    }

    protected def makeQuery(id: UUID): Bson = Filters.and(Filters.eq("id", id), Filters.eq("deleted", false))

    protected def makeQuery(id: UUID, query: Bson): Bson = {
        return Filters.and(
            Filters.eq("id", id), Filters.eq("deleted", false),
            query
        )
    }

}
