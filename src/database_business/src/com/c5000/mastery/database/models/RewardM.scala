package com.c5000.mastery.database.models

import org.joda.time.DateTime
import java.util.UUID
import com.c5000.mongopa.MpaModel

@MpaModel
class RewardM extends UniqueIdModelBase {

    var timestamp: DateTime = null
    var donatingPerson: UUID = null
    var amount: java.lang.Integer = null

}
