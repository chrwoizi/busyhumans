package com.c5000.mastery.database.queries

import java.util.UUID
import com.mongodb.DBObject
import com.c5000.mastery.database.Database
import com.c5000.mongopa.MpaModel
import scala.collection.JavaConversions._
import com.mongodb.casbah.commons.MongoDBObject

object BaseQ {

    def getId(collectionName: String, query: DBObject): UUID = {
        val result = Database.mongodb.getCollection(collectionName).findOne(query)
        if (result == null)
            return null
        return result.get("id").asInstanceOf[UUID]
    }

    def getObject(collectionName: String, query: DBObject): DBObject = {
        return Database.mongodb.getCollection(collectionName).findOne(query)
    }

    def getIds(collectionName: String, query: DBObject): Iterable[UUID] = {
        val result = Database.mongodb.getCollection(collectionName).find(query)
        if (result == null)
            return null
        return result.iterator().toIterable.map(x => x.get("id").asInstanceOf[UUID])
    }

    def getIds(collectionName: String, query: DBObject, offset: Int, maxResults: Int): Iterable[UUID] = {
        val result = Database.mongodb.getCollection(collectionName).find(query).skip(offset).limit(maxResults)
        if (result == null)
            return null
        return result.iterator().toIterable.map(x => x.get("id").asInstanceOf[UUID])
    }

    def getIds(collectionName: String, query: DBObject, offset: Int, maxResults: Int, orderBy: String, order: Int): Iterable[UUID] = {
        val result = Database.mongodb.getCollection(collectionName).find(query).sort(MongoDBObject(orderBy -> order).asInstanceOf[DBObject]).skip(offset).limit(maxResults)
        if (result == null)
            return null
        return result.iterator().toIterable.map(x => x.get("id").asInstanceOf[UUID])
    }

    def getDistinctIds(collectionName: String, query: DBObject, distinctBy: String): Iterable[UUID] = {
        val result = Database.mongodb.getCollection(collectionName).distinct(distinctBy, query)
        if (result == null)
            return null
        return result.map(x => x.asInstanceOf[UUID])
    }

    def getIds(collectionName: String, query: DBObject, offset: Int, maxResults: Int, orderBy: String, order: Int, orderBy2: String, order2: Int): Iterable[UUID] = {
        val result = Database.mongodb.getCollection(collectionName).find(query).sort(MongoDBObject(orderBy -> order, orderBy2 -> order2).asInstanceOf[DBObject]).skip(offset).limit(maxResults)
        if (result == null)
            return null
        return result.iterator().toIterable.map(x => x.get("id").asInstanceOf[UUID])
    }

    def getObjects(collectionName: String, query: DBObject, maxResults: Int): Iterable[DBObject] = {
        val result = Database.mongodb.getCollection(collectionName).find(query).limit(maxResults)
        if (result == null)
            return null
        return result.iterator().toIterable
    }

    def getObjects(collectionName: String, query: DBObject): Iterable[DBObject] = {
        val result = Database.mongodb.getCollection(collectionName).find(query)
        if (result == null)
            return null
        return result.iterator().toIterable
    }

    def getCount(collectionName: String, query: DBObject): Int = {
        return Database.mongodb.getCollection(collectionName).find(query).count()
    }

}

class BaseQ(clazz: Class[_]) {

    protected val dbCollection = clazz.getAnnotation(classOf[MpaModel]).collection()

    protected def getId(query: DBObject): UUID = {
        return BaseQ.getId(dbCollection, query)
    }

    protected def getObject(query: DBObject): DBObject = {
        return BaseQ.getObject(dbCollection, query)
    }

    protected def getIds(query: DBObject): Iterable[UUID] = {
        return BaseQ.getIds(dbCollection, query)
    }

    protected def getIds(query: DBObject, offset: Int, maxResults: Int): Iterable[UUID] = {
        return BaseQ.getIds(dbCollection, query, offset, maxResults)
    }

    protected def getIds(query: DBObject, offset: Int, maxResults: Int, orderBy: String, order: Int): Iterable[UUID] = {
        return BaseQ.getIds(dbCollection, query, offset, maxResults, orderBy, order)
    }

    protected def getIds(query: DBObject, offset: Int, maxResults: Int, orderBy: String, order: Int, orderBy2: String, order2: Int): Iterable[UUID] = {
        return BaseQ.getIds(dbCollection, query, offset, maxResults, orderBy, order, orderBy2, order2)
    }

    protected def getDistinctIds(query: DBObject, distinctBy: String): Iterable[UUID] = {
        return BaseQ.getDistinctIds(dbCollection, query, distinctBy)
    }

    protected def getObjects(query: DBObject, maxResults: Int): Iterable[DBObject] = {
        return BaseQ.getObjects(dbCollection, query, maxResults)
    }

    protected def getCount(query: DBObject): Int = {
        return BaseQ.getCount(dbCollection, query)
    }

}
