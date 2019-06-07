package com.c5000.mastery.database.versions

import org.slf4j.LoggerFactory
import com.c5000.mastery.database.models.{AssignmentM, ActivityM}
import com.c5000.mastery.database.Database

/**
 * set "lastActivity" property on all assignments
 */
object Version12 {
    private val log = LoggerFactory.getLogger(getClass)

    def update() {
        Database.forEach(classOf[AssignmentM], includeDeleted = true) {
            assignment => {
                val acitivities = assignment.activities.validItems.map(ref => Database.load(classOf[ActivityM], ref.activity))
                if(acitivities.isEmpty)
                    assignment.lastActivity = null
                else
                    assignment.lastActivity = acitivities.maxBy(_.timestamp.getMillis).timestamp
                log.info("Setting lastActivity on assignment " + assignment.id + " to " + assignment.lastActivity)
                Database.save(assignment)
            }
        }
    }
}
