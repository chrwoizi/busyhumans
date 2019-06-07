package com.c5000.mastery.database.versions

import org.slf4j.LoggerFactory
import com.mongodb.casbah.commons.MongoDBObject
import com.c5000.mastery.database.models.AccountM
import com.c5000.mongopa.MpaModel
import com.c5000.mastery.database.Database

/**
 * removes ACCOUNT.impersonateSystem
 */
object Version8 {
    private val log = LoggerFactory.getLogger(getClass)

    def update() {
        val collection = Database.mongodb.getCollection(classOf[AccountM].getAnnotation(classOf[MpaModel]).collection())
        val query = MongoDBObject("impersonateSystem" -> MongoDBObject("$exists" -> true))
        val setter = MongoDBObject("$unset" -> MongoDBObject("impersonateSystem" -> 1))
        val result = collection.updateMulti(query, setter)
        log.info("Removed impersonateSystem from collection ACCOUNT: " + result.getN)
    }
}
