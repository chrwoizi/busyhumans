package com.c5000.mastery.database.versions

import org.slf4j.LoggerFactory
import com.mongodb.casbah.commons.MongoDBObject
import com.c5000.mastery.database.models.PersonM
import com.c5000.mongopa.MpaModel
import com.c5000.mastery.database.Database
import com.c5000.mastery.backend.TimeHelper
import com.mongodb.casbah._

/**
 * makes sure that there is a PERSON.registrationTimestamp
 */
object Version10 {
    private val log = LoggerFactory.getLogger(getClass)

    def update() {
        val collection = Database.mongodb.getCollection(classOf[PersonM].getAnnotation(classOf[MpaModel]).collection())

        val query = $or(
            "registrationTimestamp" -> MongoDBObject("$exists" -> false),
            "registrationTimestamp" -> null
        )
        val set = MongoDBObject("$set" -> MongoDBObject("registrationTimestamp" -> TimeHelper.now.toDate))
        val result = collection.updateMulti(query, set)
        log.info("Created timestamps in collection PERSON: " + result.getN)
    }
}
