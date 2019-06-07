package com.c5000.mastery.database.models

import com.c5000.mongopa.MpaModel
import java.util
import org.joda.time.DateTime

@MpaModel
class ActivityRefM extends UniqueIdModelBase {

    var person: util.UUID = null
    var activity: util.UUID = null

}
