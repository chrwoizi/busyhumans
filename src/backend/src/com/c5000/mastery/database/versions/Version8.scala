package com.c5000.mastery.database.versions

import org.slf4j.LoggerFactory
import com.c5000.mastery.database.models.AccountM
import com.c5000.mongopa.MpaModel
import com.c5000.mastery.database.Database
import com.mongodb.client.model.{Filters, Updates}

/**
 * removes ACCOUNT.impersonateSystem
 */
object Version8 {
    private val log = LoggerFactory.getLogger(getClass)

    def update() {
        val collection = Database.mongodb.getCollection(classOf[AccountM].getAnnotation(classOf[MpaModel]).collection())
        val query = Filters.exists("impersonateSystem", true)
        val setter = Updates.unset("impersonateSystem")
        val result = collection.updateMany(query, setter)
        log.info("Removed impersonateSystem from collection ACCOUNT: " + result.getModifiedCount)
    }
}
