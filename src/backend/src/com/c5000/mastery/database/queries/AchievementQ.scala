package com.c5000.mastery.database.queries

import java.util.UUID
import com.c5000.mastery.database.models.AchievementM
import com.mongodb.client.model.Filters

object AchievementQ extends BaseQ(classOf[AchievementM]) {

    /**
     * @return achievement ids for a person
     */
    def getByPerson(personId: UUID, offset: Int, maxResults: Int): Iterable[UUID] = {
        val query = Filters.and(Filters.eq("deleted", false), Filters.eq("owner", personId))
        return getIds(query, offset, maxResults)
    }

    /**
     * @return achievement ids for a person and skill
     */
    def getByPersonAndSkill(personId: UUID, skillId: UUID, maxResults: Int): Iterable[UUID] = {
        val query = Filters.and(Filters.eq("deleted", false), Filters.eq("owner", personId), Filters.eq("skill", skillId))
        return getIds(query, 0, maxResults)
    }


}
