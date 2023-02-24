package com.c5000.mastery.database.updates

import com.c5000.mastery.database.models._

import java.util.UUID
import org.joda.time.DateTime
import com.c5000.mastery.backend.services.HasServiceLogger
import com.mongodb.client.model.Filters

object NotificationU extends BaseU(classOf[NotificationM]) with HasServiceLogger {

    def setSent(accountId: UUID, timestamp: DateTime) {
        if(accountId == null || timestamp == null) {
            logger.error("Missing parameter for NotificationU.setSent")
            return
        }

        val query = Filters.and(
            Filters.eq("deleted", false),
            Filters.eq("account", accountId),
            Filters.lt("timestamp", timestamp.toDate),
            Filters.eq("email", null)
        )
        set(query, "email", timestamp.toDate)
    }

}
