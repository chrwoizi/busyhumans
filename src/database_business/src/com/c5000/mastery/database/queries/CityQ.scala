package com.c5000.mastery.database.queries

import com.mongodb.casbah.commons.MongoDBObject
import java.util.UUID
import com.c5000.mastery.database.models.CityM
import com.mongodb.casbah._

object CityQ extends BaseQ(classOf[CityM]) {

    def getByFacebookId(facebookId: String): UUID = {
        return getId(MongoDBObject("deleted" -> false, "facebookId" -> facebookId).asInstanceOf[DBObject])
    }

    def getByName(name: String): UUID = {
        return getId(MongoDBObject("deleted" -> false, "name" -> name).asInstanceOf[DBObject])
    }

}
