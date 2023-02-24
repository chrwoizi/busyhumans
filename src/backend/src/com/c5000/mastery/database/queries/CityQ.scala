package com.c5000.mastery.database.queries

import java.util.UUID
import com.c5000.mastery.database.models.CityM
import com.mongodb.client.model.Filters

object CityQ extends BaseQ(classOf[CityM]) {

    def getByFacebookId(facebookId: String): UUID = {
        return getId(Filters.and(Filters.eq("deleted", false), Filters.eq("facebookId", facebookId)))
    }

    def getByName(name: String): UUID = {
        return getId(Filters.and(Filters.eq("deleted", false), Filters.eq("name", name)))
    }

}
