package com.c5000.mastery.backend.auth

import com.c5000.mastery.database.models.CredentialM

abstract class ServerCredential {

    var model: CredentialM = new CredentialM
    def isValid: Boolean

    override def toString: String = {
        if(model != null) {
            return "[username='" + model.username + "' token='" + model.accessToken + "' secret='" + model.accessTokenSecret + "' refresh='" + model.refreshToken + "']"
        }
        else {
            return "[null]"
        }
    }

}
