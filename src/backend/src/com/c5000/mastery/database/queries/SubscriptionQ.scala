package com.c5000.mastery.database.queries

import java.util.UUID
import com.c5000.mastery.database.models.{AchievementM, SubscriptionM}
import com.mongodb.client.model.Filters

object SubscriptionQ extends BaseQ(classOf[SubscriptionM]) {

    def get(accountId: UUID, assignmentId: UUID): UUID = {
        val query = Filters.and(Filters.eq("deleted", false), Filters.eq("account", accountId), Filters.eq("assignment", assignmentId))
        val option = getIds(query, 0, 1).headOption
        if(option.isDefined)
            return option.get
        return null
    }

    def getByAccount(accountId: UUID): Iterable[UUID] = {
        val query = Filters.and(Filters.eq("deleted", false), Filters.eq("account", accountId))
        return getIds(query)
    }

    def getByAssignment(assignmentId: UUID): Iterable[UUID] = {
        val query = Filters.and(Filters.eq("deleted", false), Filters.eq("assignment", assignmentId))
        return getIds(query)
    }

    def isSubscribed(accountId: UUID, assignmentId: UUID): Boolean = {
        val query = Filters.and(Filters.eq("deleted", false), Filters.eq("account", accountId), Filters.eq("assignment", assignmentId))
        return getCount(query) > 0
    }

}
