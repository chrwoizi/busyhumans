package com.c5000.mastery.database.versions

import org.slf4j.LoggerFactory
import com.c5000.mastery.database.models.{PreferencesM, AccountM}
import com.c5000.mastery.database.Database

/**
 * set default preferences for all accounts without preferences
 */
object Version14 {
    private val log = LoggerFactory.getLogger(getClass)

    def update() {
        Database.forEach(classOf[AccountM], includeDeleted = true) {
            account => {
                if (account.preferences == null) {
                    account.preferences = new PreferencesM
                    log.info("Setting preferences of account " + account.id + " to default")
                    Database.save(account)
                }
            }
        }
    }
}
