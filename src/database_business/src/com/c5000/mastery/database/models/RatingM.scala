package com.c5000.mastery.database.models

import com.c5000.mongopa.MpaModel
import org.joda.time.DateTime
import java.util.UUID

@MpaModel
class RatingM extends UniqueIdModelBase {

    var author: UUID = null
    var timestamp: DateTime = null
    var positive: Boolean = false

}
