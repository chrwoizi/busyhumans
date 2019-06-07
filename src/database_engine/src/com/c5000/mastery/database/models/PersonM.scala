package com.c5000.mastery.database.models

import org.joda.time.DateTime
import java.util.UUID
import com.c5000.mongopa.{MpaList, MpaListField, MpaModel}

@MpaModel(collection = "PERSON")
class PersonM extends UniqueIdModelBase {

    var account: UUID = null
    var registrationTimestamp: DateTime = null

    var name: String = null
    var picture: ResourceM = null
    var gender: String = null
    var birthday: DateTime = null
    var city: UUID = null
    var locale: String = null
    var timezone: Double = 0

    var xp: Int = 0

    var founder: java.lang.Boolean = null

    @MpaListField(itemType = classOf[ContactM])
    var contacts: MpaList[ContactM] = new MpaList

}
