package com.c5000.mastery.database.updates

import java.util.{Date, UUID}
import com.mongodb.casbah.commons.MongoDBObject
import com.c5000.mastery.database.models.{PreferencesM, CredentialM, AccountM}
import com.c5000.mastery.database.Database
import com.mongodb.casbah._
import org.joda.time.DateTime
import com.mongodb.DBObject
import com.c5000.mastery.shared.data.base.PreferencesD

object AccountU extends BaseU(classOf[AccountM]) {

    def setPersistentSessionId(accountId: UUID, psid: String) {
        val query = MongoDBObject("id" -> accountId).asInstanceOf[DBObject]
        set(query, "psid", psid)
    }

    def setPreferences(accountId: UUID, preferences: PreferencesM) {
        val query = MongoDBObject("id" -> accountId).asInstanceOf[DBObject]
        set(query, "preferences", Database.convertToDbObject(preferences))
    }

    def setEmailAddress(accountId: UUID, email: String, password: String): Boolean = {
        val query = MongoDBObject("id" -> accountId).asInstanceOf[DBObject]
        if(set(query, "email", email)) {
            val query2 = MongoDBObject(
                "id" -> accountId,
                "anonCredential.username" -> MongoDBObject("$ne" -> null)).asInstanceOf[DBObject]
            setMulti(query2, MongoDBObject("anonCredential.username" -> email, "anonCredential.accessToken" -> password))
            return true
        }
        return false
    }

    def setLastLogin(accountId: UUID, timestamp: DateTime) {
        val query = MongoDBObject("id" -> accountId).asInstanceOf[DBObject]
        set(query, "lastLogin", timestamp.toDate)
    }

    def setLastAnnouncement(accountId: UUID, timestamp: Date) {
        val query = MongoDBObject("id" -> accountId).asInstanceOf[DBObject]
        set(query, "lastAnnouncement", timestamp)
    }

    def setPerson(account: AccountM, personId: UUID) {
        val query = MongoDBObject("id" -> account.id).asInstanceOf[DBObject]
        set(query, "person", personId)
        account.person = personId
    }

    def setFacebookCredential(accountId: UUID, credential: CredentialM) {
        setCredential(accountId, "facebookCredential", credential)
    }

    def setGoogleCredential(accountId: UUID, credential: CredentialM) {
        setCredential(accountId, "googleCredential", credential)
    }

    def setTwitterCredential(accountId: UUID, credential: CredentialM) {
        setCredential(accountId, "twitterCredential", credential)
    }

    def setAnonCredential(accountId: UUID, credential: CredentialM) {
        setCredential(accountId, "anonCredential", credential)
    }

    def setCredential(accountId: UUID, field: String, credential: CredentialM) {
        val query = MongoDBObject("id" -> accountId).asInstanceOf[DBObject]
        val value = if (credential != null) Database.convertToDbObject(credential) else null
        set(query, field, value)
    }

    def setConfirmedTos(account: AccountM, tosVersion: java.lang.Long) {
        val query = MongoDBObject("id" -> account.id).asInstanceOf[DBObject]
        set(query, "confirmedTosVersion", tosVersion)
        account.confirmedTosVersion = tosVersion
    }

    def setCloak(userAccountId: UUID, cloakAccountId: UUID) {
        val query = MongoDBObject("id" -> userAccountId).asInstanceOf[DBObject]
        set(query, "cloak", cloakAccountId)
    }

}
