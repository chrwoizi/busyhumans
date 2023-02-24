package com.c5000.mastery.database.models

import com.c5000.mongopa.MpaModel

import org.joda.time.DateTime
import java.util.UUID

@MpaModel(collection = "ACCOUNT")
class AccountM extends UniqueIdModelBase {

    var facebookCredential: CredentialM = null
    var googleCredential: CredentialM = null
    var twitterCredential: CredentialM = null
    var anonCredential: CredentialM = null

    /**
     * persistent session id for automatic login
     */
    var psid: String = null

    var email: String = null

    var joindate: DateTime = null
    var lastLogin: DateTime = null

    var confirmedTosVersion: java.lang.Long = 0

    var person: UUID = null

    var cloak: UUID = null

    var admin: Boolean = false
    var banned: Boolean = false

    var lastAnnouncement: DateTime = null

    var preferences: PreferencesM = new PreferencesM

}
