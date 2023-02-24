package com.c5000.mastery.database.queries

import java.util.UUID
import com.c5000.mastery.database.models.ActivityM
import com.mongodb.client.model.Filters

object ActivityQ extends BaseQ(classOf[ActivityM]) {

    /**
     * @return activity ids ordered descending by abuse reports count
     */
    def getWithMostAbuseReports(maxResults: Int): Iterable[UUID] = {
        val query = Filters.and(
            Filters.eq("deleted", false),
            Filters.gt("abuseReportsCount", 0)
        )
        return getIds(query, 0, maxResults, "abuseReportsCount", -1)
    }


}
