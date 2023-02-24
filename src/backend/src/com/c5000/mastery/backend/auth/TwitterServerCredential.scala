package com.c5000.mastery.backend.auth

import com.c5000.mastery.database.models.CredentialM
import org.scribe.model.Token

class TwitterServerCredential extends ServerCredential {

    var token: Token = null

    override def toString: String = {
        if(model != null) {
            return "[username='" + model.username + "' token='" + model.accessToken + "' secret='" + model.accessTokenSecret + "']"
        }
        else {
            return "[null]"
        }
    }

    def this(model: CredentialM) {
        this()
        this.model = model
        if(model != null) {
            this.token = new Token(model.accessToken, model.accessTokenSecret)
        }
    }

    def this(id: String, accessToken: String, accessTokenSecret: String) {
        this()
        model = new CredentialM
        model.username = id
        model.accessToken = accessToken
        model.accessTokenSecret = accessTokenSecret
        this.token = new Token(accessToken, accessTokenSecret)
    }

    def isValid: Boolean = {
        return model != null &&
            model.username != null && !model.username.isEmpty &&
            model.accessToken != null && !model.accessToken.isEmpty &&
            model.accessTokenSecret != null && !model.accessTokenSecret.isEmpty
    }

}
