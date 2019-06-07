package com.c5000.mastery.database.updates

import com.c5000.mastery.database.models._
import java.util.UUID
import com.mongodb.casbah.commons.MongoDBObject
import org.joda.time.DateTime
import com.mongodb.casbah._
import com.c5000.mastery.backend.services.HasServiceLogger

object NotificationU extends BaseU(classOf[NotificationM]) with HasServiceLogger {

    def setSent(accountId: UUID, timestamp: DateTime) {
        if(accountId == null || timestamp == null) {
            logger.error("Missing parameter for NotificationU.setSent")
            return
        }

        val query = $and(
            MongoDBObject("deleted" -> false),
            MongoDBObject("account" -> accountId),
            MongoDBObject("timestamp" -> MongoDBObject("$lt" -> timestamp.toDate)),
            MongoDBObject("email" -> null)
        ).asInstanceOf[DBObject]
        set(query, "email", timestamp.toDate)
    }

}
