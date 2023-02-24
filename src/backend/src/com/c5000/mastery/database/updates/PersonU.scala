package com.c5000.mastery.database.updates

import java.util.UUID
import com.c5000.mastery.database.Database
import com.c5000.mastery.database.models.PersonM
import com.mongodb.{BasicDBList, BasicDBObject, DBObject}

import collection.mutable
import com.mongodb.client.model.{Filters, Updates}
import org.bson.conversions.Bson

object PersonU extends BaseU(classOf[PersonM]) {

    def incrementXp(personId: UUID, xp: Int) {
        val personQuery = makeQuery(personId)
        increment(personQuery, "xp", xp)
    }

    def updatePersonDetails(person: PersonM) {

        val contacts = new BasicDBList()
        for (contact <- person.contacts.allItems) {
            contacts.add(Database.convertToDbObject(contact))
        }

        setMulti(makeQuery(person.id),
            Updates.combine(
                Updates.set("name", person.name),
                Updates.set("picture", Database.convertToDbObject(person.picture)),
                Updates.set("birthday", (if(person.birthday != null) person.birthday.toDate else null)),
                Updates.set("gender", person.gender),
                Updates.set("locale", person.locale),
                Updates.set("timezone", person.timezone),
                Updates.set("city", person.city),
                Updates.set("contacts", contacts)
            ))
    }

    def setFounder(person: PersonM, founderReward: Int): Boolean = {
        val query = makeQuery(person.id, Filters.ne("founder", true))
        if(set(query, "founder", true)) {
            person.founder = true
            incrementXp(person.id, founderReward)
            return true
        }
        return false
    }

}
