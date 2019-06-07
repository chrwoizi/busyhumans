package com.c5000.mastery.database.queries

import java.util.UUID
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.casbah._
import com.c5000.mastery.database.models.{SubscriptionM, AchievementM}

object SubscriptionQ extends BaseQ(classOf[SubscriptionM]) {

    def get(accountId: UUID, assignmentId: UUID): UUID = {
        val query = MongoDBObject("deleted" -> false, "account" -> accountId, "assignment" -> assignmentId).asInstanceOf[DBObject]
        val option = getIds(query, 0, 1).headOption
        if(option.isDefined)
            return option.get
        return null
    }

    def getByAccount(accountId: UUID): Iterable[UUID] = {
        val query = MongoDBObject("deleted" -> false, "account" -> accountId).asInstanceOf[DBObject]
        return getIds(query)
    }

    def getByAssignment(assignmentId: UUID): Iterable[UUID] = {
        val query = MongoDBObject("deleted" -> false, "assignment" -> assignmentId).asInstanceOf[DBObject]
        return getIds(query)
    }

    def isSubscribed(accountId: UUID, assignmentId: UUID): Boolean = {
        val query = MongoDBObject("deleted" -> false, "account" -> accountId, "assignment" -> assignmentId).asInstanceOf[DBObject]
        return getCount(query) > 0
    }

}
