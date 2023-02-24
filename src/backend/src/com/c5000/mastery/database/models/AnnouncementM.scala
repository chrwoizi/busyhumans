package com.c5000.mastery.database.models

import com.c5000.mongopa.MpaModel
import org.joda.time.DateTime

@MpaModel(collection = "ANNOUNCEMENT")
class AnnouncementM extends UniqueIdModelBase {

    var showTime: DateTime = null
    var hideTime: DateTime = null
    var text: String = null
    var maintenance: Boolean = false

}
