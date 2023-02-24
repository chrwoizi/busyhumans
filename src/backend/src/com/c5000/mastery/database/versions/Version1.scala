package com.c5000.mastery.database.versions

import com.c5000.mastery.database.Database
import com.c5000.mastery.database.models.ActivityM
import com.c5000.mongopa.MpaModel
import com.c5000.mastery.backend.TimeHelper
import com.mongodb.client.model.{Filters, Updates}
import org.slf4j.LoggerFactory

/**
 * Moving the reward due date from assignment level to activity level
 */
object Version1 {
    private val log = LoggerFactory.getLogger(getClass)

    def update() {
        val collection = Database.mongodb.getCollection(classOf[ActivityM].getAnnotation(classOf[MpaModel]).collection())
        val query = Filters.eq("rewardDueDate", null)
        val setter = Updates.combine(Updates.set("rewardDueDate", TimeHelper.now.plusDays(3).toDate),
            Updates.set("rewarded", null))
        val result = collection.updateMany(query, setter)
        log.info("Updated by moveRewardDueDate: " + result.getModifiedCount())
    }
}
