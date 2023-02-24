package com.c5000.mastery.database.queries

import scala.collection.JavaConverters._
import java.util.UUID
import com.c5000.mastery.database.Database
import com.c5000.mongopa.MpaModel
import com.mongodb.client.model.Sorts
import org.bson.Document
import org.bson.conversions.Bson
import org.bson.types.Binary

object BaseQ {

    def toUUID(id: Any): UUID = {
        if (id.isInstanceOf[Binary]) {
            return UUID.nameUUIDFromBytes(id.asInstanceOf[Binary].getData)
        }
        if (id.isInstanceOf[String]) {
            return UUID.fromString(id.asInstanceOf[String])
        }
        return id.asInstanceOf[UUID]
    }

    def getId(collectionName: String, query: Bson): UUID = {
        val result = Database.mongodb.getCollection(collectionName).find(query).first()
        if (result == null)
            return null
        return this.toUUID(result.get("id"))
    }

    def getObject(collectionName: String, query: Bson): Document = {
        val result = Database.mongodb.getCollection(collectionName).find(query).first()
        return result
    }

    def getIds(collectionName: String, query: Bson): Iterable[UUID] = {
        val result = Database.mongodb.getCollection(collectionName).find(query)
        if (result == null)
            return null
        return result.map(x => this.toUUID(x.get("id"))).asScala
    }

    def getIds(collectionName: String, query: Bson, offset: Int, maxResults: Int): Iterable[UUID] = {
        val result = Database.mongodb.getCollection(collectionName).find(query).skip(offset).limit(maxResults)
        if (result == null)
            return null
        return result.map(x => this.toUUID(x.get("id"))).asScala
    }

    def getIds(collectionName: String, query: Bson, offset: Int, maxResults: Int, orderBy: String, order: Int): Iterable[UUID] = {
        val result = Database.mongodb.getCollection(collectionName).find(query).sort(if (order > 0) Sorts.ascending(orderBy) else Sorts.descending(orderBy)).skip(offset).limit(maxResults)
        if (result == null)
            return null
        return result.map(x => this.toUUID(x.get("id"))).asScala
    }

    def getDistinctIds(collectionName: String, query: Bson, distinctBy: String): Iterable[UUID] = {
        val result = Database.mongodb.getCollection(collectionName).distinct(distinctBy, query, classOf[UUID])
        if (result == null)
            return null
        return result.map(x => this.toUUID(x)).asScala
    }

    def getIds(collectionName: String, query: Bson, offset: Int, maxResults: Int, orderBy: String, order: Int, orderBy2: String, order2: Int): Iterable[UUID] = {
        val result = Database.mongodb.getCollection(collectionName).find(query)
          .sort(Sorts.orderBy(
              if (order > 0) Sorts.ascending(orderBy) else Sorts.descending(orderBy),
              if (order2 > 0) Sorts.ascending(orderBy2) else Sorts.descending(orderBy2)
          )).skip(offset).limit(maxResults)
        if (result == null)
            return null
        return result.map(x => this.toUUID(x.get("id"))).asScala
    }

    def getObjects(collectionName: String, query: Bson, maxResults: Int): Iterable[Document] = {
        val result = Database.mongodb.getCollection(collectionName).find(query).limit(maxResults)
        if (result == null)
            return null
        return result.asScala.toArray[Document]
    }

    def getObjects(collectionName: String, query: Bson): Iterable[Document] = {
        val result = Database.mongodb.getCollection(collectionName).find(query)
        if (result == null)
            return null
        return result.asScala.toArray[Document]
    }

    def getCount(collectionName: String, query: Bson): Int = {
        val result = Database.mongodb.getCollection(collectionName).countDocuments(query)
        return result.toInt
    }

}

class BaseQ(clazz: Class[_]) {

    protected val dbCollection = clazz.getAnnotation(classOf[MpaModel]).collection()

    protected def getId(query: Bson): UUID = {
        return BaseQ.getId(dbCollection, query)
    }

    protected def getObject(query: Bson): Document = {
        return BaseQ.getObject(dbCollection, query)
    }

    protected def getIds(query: Bson): Iterable[UUID] = {
        return BaseQ.getIds(dbCollection, query)
    }

    protected def getIds(query: Bson, offset: Int, maxResults: Int): Iterable[UUID] = {
        return BaseQ.getIds(dbCollection, query, offset, maxResults)
    }

    protected def getIds(query: Bson, offset: Int, maxResults: Int, orderBy: String, order: Int): Iterable[UUID] = {
        return BaseQ.getIds(dbCollection, query, offset, maxResults, orderBy, order)
    }

    protected def getIds(query: Bson, offset: Int, maxResults: Int, orderBy: String, order: Int, orderBy2: String, order2: Int): Iterable[UUID] = {
        return BaseQ.getIds(dbCollection, query, offset, maxResults, orderBy, order, orderBy2, order2)
    }

    protected def getDistinctIds(query: Bson, distinctBy: String): Iterable[UUID] = {
        return BaseQ.getDistinctIds(dbCollection, query, distinctBy)
    }

    protected def getObjects(query: Bson, maxResults: Int): Iterable[Document] = {
        return BaseQ.getObjects(dbCollection, query, maxResults)
    }

    protected def getCount(query: Bson): Int = {
        return BaseQ.getCount(dbCollection, query)
    }

}
