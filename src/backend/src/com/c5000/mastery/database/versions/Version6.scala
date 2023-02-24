package com.c5000.mastery.database.versions

import com.c5000.mastery.database.{SpecialUsers, Database}
import org.slf4j.LoggerFactory
import com.c5000.mastery.database.models._
import com.c5000.mastery.shared.Config
import java.util.UUID

/**
 * Change first reward donor of assignments to system if assignment author is system
 */
object Version6 {
    private val log = LoggerFactory.getLogger(getClass)

    def update() {
        val sysPerson: UUID = UUID.fromString(Config.SYS_OBJ_ID)
        Database.forEach(classOf[AssignmentM], includeDeleted = true) {
            assignment => {
                if (assignment.author == sysPerson) {
                    val rewards = assignment.rewards.validItems
                    if (rewards.size > 0 && rewards.head.donatingPerson != sysPerson) {
                        log.info("Changing donor of reward for assignment " + assignment.id + " to System.")
                        rewards.head.donatingPerson = sysPerson
                        Database.save(assignment)
                    }
                }
            }
        }
    }
}
