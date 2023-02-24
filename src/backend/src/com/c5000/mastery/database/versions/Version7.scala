package com.c5000.mastery.database.versions

import com.c5000.mastery.database.Database
import org.slf4j.LoggerFactory
import com.c5000.mastery.database.models._
import com.c5000.mastery.shared.Config
import java.util.UUID

/**
 * Change reward of assignments to Config.SYS_REWARD if assignment author is system
 */
object Version7 {
    private val log = LoggerFactory.getLogger(getClass)

    def update() {
        val sysPerson: UUID = UUID.fromString(Config.SYS_OBJ_ID)
        Database.forEach(classOf[AssignmentM], includeDeleted = true) {
            assignment => {
                if (assignment.author == sysPerson) {
                    val rewards = assignment.rewards.validItems
                    if (rewards.size > 0 && rewards.head.donatingPerson == sysPerson) {
                        log.info("Changing reward of assignment " + assignment.id + " to " + Config.SYS_REWARD + " XP.")
                        rewards.head.amount = Config.SYS_REWARD
                        Database.save(assignment)
                    }
                }
            }
        }
    }
}
