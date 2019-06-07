package com.c5000.mastery.database.models

import org.joda.time.DateTime
import java.util.UUID
import com.c5000.mongopa.MpaModel

@MpaModel(collection = "SUBSCRIPTION")
class SubscriptionM extends UniqueIdModelBase {

    var timestamp: DateTime = null
    var account: UUID = null
    var assignment: UUID = null

}
