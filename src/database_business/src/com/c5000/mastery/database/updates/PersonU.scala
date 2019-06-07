package com.c5000.mastery.database.updates

import java.util.UUID
import com.c5000.mastery.database.Database
import com.c5000.mastery.database.models.{PersonM, ListItemM}
import com.c5000.mastery.backend.services.ServiceLogger
import com.mongodb.casbah.commons.MongoDBObject
import collection.mutable.Buffer
import com.mongodb.DBObject
import com.c5000.mastery.backend.TimeHelper
import collection.mutable
import com.mongodb.casbah._

object PersonU extends BaseU(classOf[PersonM]) {

    def incrementXp(personId: UUID, xp: Int) {
        val personQuery = makeQuery(personId)
        increment(personQuery, "xp", xp)
    }

    def updatePersonDetails(person: PersonM) {

        val contacts = mutable.Buffer[DBObject]()
        for (contact <- person.contacts.allItems) {
            contacts += Database.convertToDbObject(contact)
        }

        setMulti(makeQuery(person.id),
            MongoDBObject(
                "name" -> person.name,
                "picture" -> Database.convertToDbObject(person.picture),
                "birthday" -> (if(person.birthday != null) person.birthday.toDate else null),
                "gender" -> person.gender,
                "locale" -> person.locale,
                "timezone" -> person.timezone,
                "city" -> person.city,
                "contacts" -> contacts
            ))
    }

    def setFounder(person: PersonM, founderReward: Int): Boolean = {
        val query = makeQuery(person.id, MongoDBObject("founder" -> MongoDBObject("$ne" -> true)))
        if(set(query, "founder", true)) {
            person.founder = true
            incrementXp(person.id, founderReward)
            return true
        }
        return false
    }

}
