package com.c5000.mastery.database.queries

import java.util.UUID
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.casbah._
import com.c5000.mastery.database.models.ActivityM

object ActivityQ extends BaseQ(classOf[ActivityM]) {

    /**
     * @return activity ids ordered descending by abuse reports count
     */
    def getWithMostAbuseReports(maxResults: Int): Iterable[UUID] = {
        val query = $and(
                MongoDBObject("deleted" -> false),
                "abuseReportsCount" $gt 0
            ).asInstanceOf[DBObject]
        return getIds(query, 0, maxResults, "abuseReportsCount", -1)
    }


}
