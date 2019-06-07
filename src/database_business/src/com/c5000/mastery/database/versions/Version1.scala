package com.c5000.mastery.database.versions

import com.c5000.mastery.database.Database
import com.c5000.mastery.database.models.ActivityM
import com.c5000.mongopa.MpaModel
import com.mongodb.casbah.commons.MongoDBObject
import com.c5000.mastery.backend.TimeHelper
import org.slf4j.LoggerFactory

/**
 * Moving the reward due date from assignment level to activity level
 */
object Version1 {
    private val log = LoggerFactory.getLogger(getClass)

    def update() {
        val collection = Database.mongodb.getCollection(classOf[ActivityM].getAnnotation(classOf[MpaModel]).collection())
        val query = MongoDBObject("rewardDueDate" -> null)
        val setter = MongoDBObject("$set" -> MongoDBObject(
            "rewardDueDate" -> TimeHelper.now.plusDays(3).toDate,
            "rewarded" -> null))
        val result = collection.updateMulti(query, setter)
        log.info("Updated by moveRewardDueDate: " + result.getN)
    }
}
