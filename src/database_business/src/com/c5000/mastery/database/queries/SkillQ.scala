package com.c5000.mastery.database.queries

import com.mongodb.casbah.commons.MongoDBObject
import java.util.UUID
import com.mongodb.casbah._
import com.c5000.mastery.database.models.SkillM

object SkillQ extends BaseQ(classOf[SkillM]) {

    /**
     * @return true if an assignment with the given title exists. case insensitive.
     */
    def existsWithTitle(title: String): Boolean = {
        val regex = "(?i:^" + title + "$)"
        val query = $and(
            MongoDBObject("deleted" -> false),
            MongoDBObject("title" -> regex.r)
        ).asInstanceOf[DBObject]
        return getObject(query) != null
    }

    def getSkillIdsByTitle(title: String, maxResults: Int): Iterable[UUID] = {
        val query = MongoDBObject("deleted" -> false, "title" -> title).asInstanceOf[DBObject]
        return getIds(query, 0, maxResults)
    }

    /**
     * @return skill ids ordered descending by abuse reports count
     */
    def getWithMostAbuseReports(maxResults: Int): Iterable[UUID] = {
        val query = $and(
                MongoDBObject("deleted" -> false),
                "abuseReportsCount" $gt 0
            ).asInstanceOf[DBObject]
        return getIds(query, 0, maxResults, "abuseReportsCount", -1)
    }

}
