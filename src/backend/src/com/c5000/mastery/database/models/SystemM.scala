package com.c5000.mastery.database.models

import com.c5000.mongopa.{MpaList, MpaListField, MpaModel}
import org.joda.time.DateTime
import java.util.UUID

@MpaModel(collection = "SYSTEM")
class SystemM extends UniqueIdModelBase {

    var version: Int = 0

}
