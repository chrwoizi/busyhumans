package com.c5000.mastery.database.models

import com.c5000.mongopa.MpaModel
import org.joda.time.DateTime
import java.util.UUID

@MpaModel
class ContactM extends UniqueIdModelBase {

    var timestamp: DateTime = null
    var typ: String = null
    var value: String = null

}
