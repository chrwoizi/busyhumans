package com.c5000.mastery.database.updates

import java.util.{Date, UUID}
import com.c5000.mastery.database.models.{AccountM, CredentialM, PreferencesM}
import com.c5000.mastery.database.Database
import org.joda.time.DateTime
import com.mongodb.client.model.{Filters, Updates}

object AccountU extends BaseU(classOf[AccountM]) {

    def setPersistentSessionId(accountId: UUID, psid: String) {
        val query = Filters.eq("id", accountId)
        set(query, "psid", psid)
    }

    def setPreferences(accountId: UUID, preferences: PreferencesM) {
        val query = Filters.eq("id", accountId)
        set(query, "preferences", Database.convertToDbObject(preferences))
    }

    def setEmailAddress(accountId: UUID, email: String, password: String): Boolean = {
        val query = Filters.eq("id", accountId)
        if(set(query, "email", email)) {
            val query2 = Filters.and(
                Filters.eq("id", accountId),
                Filters.ne("anonCredential.username", null))
            setMulti(query2, Updates.combine(Updates.set("anonCredential.username", email), Updates.set("anonCredential.accessToken", password)))
            return true
        }
        return false
    }

    def setLastLogin(accountId: UUID, timestamp: DateTime) {
        val query = Filters.eq("id", accountId)
        set(query, "lastLogin", timestamp.toDate)
    }

    def setLastAnnouncement(accountId: UUID, timestamp: Date) {
        val query = Filters.eq("id", accountId)
        set(query, "lastAnnouncement", timestamp)
    }

    def setPerson(account: AccountM, personId: UUID) {
        val query = Filters.eq("id", account.id)
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
        val query = Filters.eq("id", accountId)
        val value = if (credential != null) Database.convertToDbObject(credential) else null
        set(query, field, value)
    }

    def setConfirmedTos(account: AccountM, tosVersion: java.lang.Long) {
        val query = Filters.eq("id", account.id)
        set(query, "confirmedTosVersion", tosVersion)
        account.confirmedTosVersion = tosVersion
    }

    def setCloak(userAccountId: UUID, cloakAccountId: UUID) {
        val query = Filters.eq("id", userAccountId)
        set(query, "cloak", cloakAccountId)
    }

}
