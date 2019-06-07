package com.c5000.mastery.backend.auth

import com.c5000.mastery.database.models.CredentialM

class FacebookServerCredential extends ServerCredential {

    override def toString: String = {
        if (model != null) {
            return "[username='" + model.username + "' token='" + model.accessToken + "'']"
        }
        else {
            return "[null]"
        }
    }

    def this(model: CredentialM) {
        this()
        this.model = model
    }

    def this(clientCredential: FacebookClientCredential) {
        this()
        model = new CredentialM
        if (clientCredential != null) {
            model.username = clientCredential.uid
            model.accessToken = clientCredential.accessToken
        }
    }

    def isValid: Boolean = {
        return model != null &&
            model.username != null && !model.username.isEmpty &&
            model.accessToken != null && !model.accessToken.isEmpty
    }

}
