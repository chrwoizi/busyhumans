package com.c5000.mastery.database.queries

import java.util.UUID
import com.c5000.mastery.database.models.AssignmentM
import com.c5000.mastery.shared.data.base.SortBy
import com.c5000.mastery.shared.Config
import com.mongodb.client.model.Filters
import org.bson.conversions.Bson

import collection.mutable

object AssignmentQ extends BaseQ(classOf[AssignmentM]) {

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

    def buildCompletedQuery(personId: UUID): Bson = {
        return Filters.and(
            Filters.eq("deleted", false),
            Filters.elemMatch("activities", Filters.and(
                Filters.eq("deleted", false),
                Filters.eq("person", personId)
            ))
        )
    }

    /**
     * @return count of assignment ids where:
     *         the person has an activity
     */
    def getCompletedCount(personId: UUID): Int = {
        val query = buildCompletedQuery(personId)
        return getCount(query)
    }

    /**
     * @return assignment ids where:
     *         the person has an activity
     */
    def getCompleted(personId: UUID, offset: Int, maxResults: Int): Iterable[UUID] = {
        val query = buildCompletedQuery(personId)
        return getIds(query, offset, maxResults, "rewardsSum", -1)
    }

    def buildCreatedQuery(personId: UUID): Bson = {
        return Filters.and(Filters.eq("deleted", false), Filters.eq("author", personId))
    }

    /**
     * @return assignment ids where:
     *         the person is the author
     */
    def getCreatedCount(personId: UUID): Int = {
        val query = buildCreatedQuery(personId)
        return getCount(query)
    }

    /**
     * @return assignment ids where:
     *         the person is the author
     */
    def getCreated(personId: UUID, offset: Int, maxResults: Int): Iterable[UUID] = {
        val query = buildCreatedQuery(personId)
        return getIds(query, offset, maxResults, "rewardsSum", -1)
    }

    /**
     * @return assignment ids where:
     *         the assignment is not done
     */
    def getAll(skip: Int, maxResults: Int, sortBy: SortBy): Iterable[UUID] = {
        val query = Filters.and(
            Filters.eq("deleted", false),
            Filters.ne("id", Config.FOUNDER_ASSIGNMENT_ID))

        val sortField = sortBy match {
            case SortBy.NEWEST => "creationTimestamp"
            case SortBy.ACTIVITY => "lastActivity"
            case SortBy.REWARD => "rewardsSum"
            case _ => "creationTimestamp"
        }

        val sortOrder = sortBy match {
            case SortBy.NEWEST => -1
            case SortBy.ACTIVITY => -1
            case SortBy.REWARD => -1
            case _ => -1
        }

        var result = mutable.Buffer[UUID]()
        var realSkip = skip
        if (skip == 0) {
            result += UUID.fromString(Config.FOUNDER_ASSIGNMENT_ID)
        }
        else {
            // skip includes the founder assignment but the db query doesnt
            realSkip = skip - 1
        }
        getIds(query, realSkip, maxResults, sortField, sortOrder, "creationTimestamp", -1).foreach(result += _)
        return result
    }

    /**
     * @return assignment ids ordered descending by abuse reports count
     */
    def getWithMostAbuseReports(maxResults: Int): Iterable[UUID] = {
        val query = Filters.and(
            Filters.eq("deleted", false),
            Filters.gt("abuseReportsCount", 0)
        )
        return getIds(query, 0, maxResults, "abuseReportsCount", -1)
    }

    /**
     * @return assignment ids where:
     *         the assignment skill matches
     */
    def getBySkill(skillId: UUID, offset: Int, maxResults: Int): Iterable[UUID] = {
        val query = Filters.and(Filters.eq("deleted", false), Filters.eq("skill", skillId))
        return getIds(query, offset, maxResults)
    }

    def getCountBySkill(skillId: UUID): Int = {
        val query = Filters.and(Filters.eq("deleted", false), Filters.eq("skill", skillId))
        return getCount(query)
    }


}
