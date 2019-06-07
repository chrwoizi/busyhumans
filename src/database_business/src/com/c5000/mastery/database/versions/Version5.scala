package com.c5000.mastery.database.versions

import com.c5000.mastery.database.{SpecialUsers, Database}
import org.slf4j.LoggerFactory
import com.c5000.mastery.database.models._
import com.c5000.mastery.shared.Config
import java.util.UUID

/**
 * Change assignment authors to system if author is admin
 */
object Version5 {
    private val log = LoggerFactory.getLogger(getClass)

    def update() {
        Database.forEach(classOf[AssignmentM], includeDeleted = true) {
            assignment => {
                val author = Database.load(classOf[PersonM], assignment.author)
                if (author != null) {
                    if (SpecialUsers.isAdmin(author.account)) {
                        log.info("Changing author of assignment " + assignment.id + " to System.")
                        assignment.author = UUID.fromString(Config.SYS_OBJ_ID)
                        Database.save(assignment)
                    }
                }
            }
        }
    }
}
