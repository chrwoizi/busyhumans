package com.c5000.mastery.database.queries

import java.util.UUID
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.casbah._
import com.c5000.mastery.database.models.{AchievementM, ActivityM}

object AchievementQ extends BaseQ(classOf[AchievementM]) {

    /**
     * @return achievement ids for a person
     */
    def getByPerson(personId: UUID, offset: Int, maxResults: Int): Iterable[UUID] = {
        val query = MongoDBObject("deleted" -> false, "owner" -> personId).asInstanceOf[DBObject]
        return getIds(query, offset, maxResults)
    }

    /**
     * @return achievement ids for a person and skill
     */
    def getByPersonAndSkill(personId: UUID, skillId: UUID, maxResults: Int): Iterable[UUID] = {
        val query = MongoDBObject("deleted" -> false, "owner" -> personId, "skill" -> skillId).asInstanceOf[DBObject]
        return getIds(query, 0, maxResults)
    }


}
