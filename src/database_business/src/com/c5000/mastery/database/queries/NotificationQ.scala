package com.c5000.mastery.database.queries

import java.util.UUID
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.casbah._
import com.c5000.mastery.database.models.NotificationM
import com.mongodb.DBObject

object NotificationQ extends BaseQ(classOf[NotificationM]) {

    def getUnsentAccounts: Iterable[UUID] = {
        val query = $and(
            MongoDBObject("deleted" -> false),
            MongoDBObject("email" -> null)
        ).asInstanceOf[DBObject]
        return getDistinctIds(query, "account")
    }

    def getUnsent(accountId: UUID, includeTypes: Iterable[Int]): Iterable[UUID] = {
        if (includeTypes.isEmpty)
            return List()

        val query = $and(
            MongoDBObject("deleted" -> false),
            MongoDBObject("account" -> accountId),
            MongoDBObject("email" -> null),
            $or(includeTypes.map(typ => MongoDBObject("typ" -> typ)).toSeq)
        ).asInstanceOf[DBObject]
        return getIds(query, 0, 999, "timestamp", 1)
    }

}
