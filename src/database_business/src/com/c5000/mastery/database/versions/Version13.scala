package com.c5000.mastery.database.versions

import org.slf4j.LoggerFactory
import com.c5000.mastery.database.models.{AccountM, AssignmentM, ActivityM}
import com.c5000.mastery.database.Database

/**
 * set "lastLogin" property on all accounts
 */
object Version13 {
    private val log = LoggerFactory.getLogger(getClass)

    def update() {
        Database.forEach(classOf[AccountM], includeDeleted = true) {
            account => {
                account.lastLogin = account.joindate
                log.info("Setting lastLogin of account " + account.id + " to " + account.lastLogin)
                Database.save(account)
            }
        }
    }
}
