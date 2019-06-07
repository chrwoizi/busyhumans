package com.c5000.mastery.database.models

import com.c5000.mongopa.MpaModel
import org.joda.time.DateTime
import java.util.UUID

@MpaModel(collection = "NOTIFICATION")
class NotificationM extends UniqueIdModelBase {

    var typ: Int = 0
    var account: UUID = null
    var timestamp: DateTime = null
    var email: DateTime = null

    var html: String = null

    var values: Iterable[String] = null

}
