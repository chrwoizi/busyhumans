package com.c5000.mastery.database.queries

import java.util.UUID
import com.c5000.mastery.database.models.NotificationM
import com.mongodb.client.model.Filters
import scala.collection.JavaConverters._

object NotificationQ extends BaseQ(classOf[NotificationM]) {

    def getUnsentAccounts: Iterable[UUID] = {
        val query = Filters.and(
            Filters.eq("deleted", false),
            Filters.eq("email", null)
        )
        return getDistinctIds(query, "account")
    }

    def getUnsent(accountId: UUID, includeTypes: Iterable[Int]): Iterable[UUID] = {
        if (includeTypes.isEmpty)
            return List()

        val query = Filters.and(
            Filters.eq("deleted", false),
            Filters.eq("account", accountId),
            Filters.eq("email", null),
            Filters.or(includeTypes.map(typ => Filters.eq("typ", typ)).asJava)
        )
        return getIds(query, 0, 999, "timestamp", 1)
    }

}
