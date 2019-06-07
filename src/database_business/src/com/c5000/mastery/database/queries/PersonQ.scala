package com.c5000.mastery.database.queries

import java.util.UUID
import com.c5000.mastery.database.models.PersonM
import com.c5000.mastery.database.Database
import scala.collection.JavaConversions._
import com.mongodb.casbah._
import com.mongodb.casbah.commons.MongoDBObject
import com.c5000.mastery.shared.Config

object PersonQ extends BaseQ(classOf[PersonM]) {

    /**
     * @return achievement ids where:
     *         the skill matches
     */
    def getAchievementsBySkill(skillId: UUID, maxResults: Int): Iterable[UUID] = {
        val query = $and(
            MongoDBObject("deleted" -> false),
            "achievements" $elemMatch $and(
                MongoDBObject("deleted" -> false),
                MongoDBObject("skill" -> skillId)
            )
        ).asInstanceOf[DBObject]
        return getIds(query, 0, maxResults)
    }

    def getByRank(maxResults: Int): Iterable[UUID] = {
        val systemPersonId: UUID = UUID.fromString(Config.SYS_OBJ_ID)
        val query = $and(
            MongoDBObject("deleted" -> false),
            MongoDBObject("id" -> MongoDBObject("$ne" -> systemPersonId)),
            "xp" $gt 0
        ).asInstanceOf[DBObject]
        return getIds(query, 0, maxResults, "xp", -1)
    }

    def getRank(personXp: Int): Int = {
        val query = $and(
            MongoDBObject("deleted" -> false),
            "xp" $gt personXp
        ).asInstanceOf[DBObject]
        return getCount(query)
    }

    def getCloaks: Iterable[UUID] = {
        return getIds(MongoDBObject("account" -> null).asInstanceOf[DBObject])
    }

}
