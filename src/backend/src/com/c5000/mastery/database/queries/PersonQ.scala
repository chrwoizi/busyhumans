package com.c5000.mastery.database.queries

import java.util.UUID
import com.c5000.mastery.database.models.PersonM

import scala.collection.JavaConverters._
import com.c5000.mastery.shared.Config
import com.mongodb.client.model.Filters

object PersonQ extends BaseQ(classOf[PersonM]) {

    /**
     * @return achievement ids where:
     *         the skill matches
     */
    def getAchievementsBySkill(skillId: UUID, maxResults: Int): Iterable[UUID] = {
        val query = Filters.and(
            Filters.eq("deleted", false),
            Filters.elemMatch("achievements", Filters.and(
                Filters.eq("deleted", false),
                Filters.eq("skill", skillId)
            )
        ))
        return getIds(query, 0, maxResults)
    }

    def getByRank(maxResults: Int): Iterable[UUID] = {
        val systemPersonId: UUID = UUID.fromString(Config.SYS_OBJ_ID)
        val query = Filters.and(
            Filters.eq("deleted", false),
            Filters.ne("id", systemPersonId),
            Filters.gt("xp", 0)
        )
        return getIds(query, 0, maxResults, "xp", -1)
    }

    def getRank(personXp: Int): Int = {
        val query = Filters.and(
            Filters.eq("deleted", false),
            Filters.gt("xp", personXp)
        )
        return getCount(query)
    }

    def getCloaks: Iterable[UUID] = {
        return getIds(Filters.eq("account", null))
    }

}
