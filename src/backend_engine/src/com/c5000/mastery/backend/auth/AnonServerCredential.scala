package com.c5000.mastery.backend.auth

import com.c5000.mastery.database.models.CredentialM
import org.scribe.model.Token

class AnonServerCredential extends ServerCredential {

    def username: String = if (model == null) null else model.username
    def passwordHash: String = if (model == null) null else model.accessToken
    def passwordHashSalt: String = if (model == null) null else model.accessTokenSecret

    override def toString: String = {
        if (model != null) {
            return "[username='" + username + "' passwordHash='" + passwordHash + "' passwordHashSalt='" + passwordHashSalt + "']"
        }
        else {
            return "[null]"
        }
    }

    def this(model: CredentialM) {
        this()
        this.model = model
    }

    def this(username: String, passwordHash: String, passwordHashSalt: String) {
        this()
        model = new CredentialM
        model.username = username
        model.accessToken = passwordHash
        model.accessTokenSecret = passwordHashSalt
    }

    def isValid: Boolean = {
        return model != null &&
            model.username != null && !model.username.isEmpty &&
            model.accessToken != null && !model.accessToken.isEmpty &&
            model.accessTokenSecret != null && !model.accessTokenSecret.isEmpty
    }

}
