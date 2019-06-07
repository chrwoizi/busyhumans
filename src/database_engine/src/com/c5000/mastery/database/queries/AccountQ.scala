package com.c5000.mastery.database.queries

import com.c5000.mastery.database.models.AccountM
import com.mongodb.casbah.commons.MongoDBObject
import java.util.UUID
import com.mongodb.casbah._

object AccountQ extends BaseQ(classOf[AccountM]) {

    def getAccountIdByPersistentSessionId(persistentSid: String): UUID = {
        if (persistentSid == null)
            return null
        return getId(MongoDBObject("psid" -> persistentSid))
    }

    def getAccountIdByFacebookUsername(facebookUid: String): UUID = {
        if (facebookUid == null)
            return null
        return getId(MongoDBObject("facebookCredential.username" -> facebookUid))
    }

    def getAccountIdByTwitterUsername(twitterUid: String): UUID = {
        if (twitterUid == null)
            return null
        return getId(MongoDBObject("twitterCredential.username" -> twitterUid))
    }

    def getAccountIdByAnonUsername(username: String): UUID = {
        if (username == null)
            return null
        return getId(MongoDBObject("anonCredential.username" -> username))
    }

}
