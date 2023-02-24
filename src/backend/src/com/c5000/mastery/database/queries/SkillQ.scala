package com.c5000.mastery.database.queries

import java.util.UUID
import com.c5000.mastery.database.models.SkillM
import com.mongodb.client.model.Filters

object SkillQ extends BaseQ(classOf[SkillM]) {

    /**
     * @return true if an assignment with the given title exists. case insensitive.
     */
    def existsWithTitle(title: String): Boolean = {
        val regex = "(?i:^" + title + "$)"
        val query = Filters.and(
            Filters.eq("deleted", false),
            Filters.regex("title", regex)
        )
        return getObject(query) != null
    }

    def getSkillIdsByTitle(title: String, maxResults: Int): Iterable[UUID] = {
        val query = Filters.and(Filters.eq("deleted", false), Filters.eq("title", title))
        return getIds(query, 0, maxResults)
    }

    /**
     * @return skill ids ordered descending by abuse reports count
     */
    def getWithMostAbuseReports(maxResults: Int): Iterable[UUID] = {
        val query = Filters.and(
            Filters.eq("deleted", false),
            Filters.gt("abuseReportsCount", 0)
            )
        return getIds(query, 0, maxResults, "abuseReportsCount", -1)
    }

}
