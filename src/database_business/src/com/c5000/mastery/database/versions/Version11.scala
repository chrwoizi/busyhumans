package com.c5000.mastery.database.versions

import org.slf4j.LoggerFactory
import com.c5000.mastery.database.models.ActivityM
import com.c5000.mastery.database.Database

/**
 * set "authentic" property on all activity content blocks
 */
object Version11 {
    private val log = LoggerFactory.getLogger(getClass)

    def update() {
        Database.forEach(classOf[ActivityM], includeDeleted = true) {
            activity => {
                activity.contentBlocks.allItems.foreach(cb => {
                    if (cb.authentic == null) cb.authentic = true
                })
                log.info("Ensuring authentic=true on content blocks of activity " + activity.id)
                Database.save(activity)
            }
        }
    }
}
