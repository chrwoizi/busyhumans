package com.c5000.mastery.database.updates

import com.c5000.mastery.database.models.{ListItemM, AchievementM}
import java.util.UUID
import com.mongodb.casbah.commons.MongoDBObject
import com.c5000.mastery.backend.services.ServiceLogger


object AchievementU extends BaseU(classOf[AchievementM]) {

    def deleteEmpty(achievement: AchievementM, logger: ServiceLogger): Boolean = {
        logger.trace("Trying to delete empty achievement " + achievement.id + ".")
        val query = makeQuery(achievement.id,
            MongoDBObject("activities" ->
                MongoDBObject("$not" ->
                    MongoDBObject("$elemMatch" ->
                        MongoDBObject("deleted" -> false)))))
        if (set(query, "deleted", true)) {
            logger.trace("Empty achievement " + achievement.id + " deleted.")
            achievement.deleted = true
            return true
        }
        logger.trace("Empty achievement " + achievement.id + " not deleted because it is not empty.")
        return false
    }

    def addActivity(achievement: AchievementM, activityId: UUID) {
        val query = makeQuery(achievement.id)
        val listItem = new ListItemM(activityId)
        addItem(query, "activities", listItem)
        achievement.activities.add(listItem)
    }

    def removeActivity(achievement: AchievementM, activityId: UUID) {
        val query = makeQuery(achievement.id,
            MongoDBObject("activities" ->
                MongoDBObject("$elemMatch" ->
                    MongoDBObject("id" -> activityId, "deleted" -> false))))
        set(query, "activities.$.deleted", true)
        achievement.activities.setDeleted(new ListItemM(activityId))
    }

}
