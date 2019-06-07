package com.c5000.mastery.backend.auth

import com.c5000.mastery.backend.TimeHelper
import com.c5000.mastery.database.models.CredentialM
import com.google.api.client.auth.oauth2.Credential

class GoogleServerCredential extends ServerCredential {

    override def toString: String = {
        if(model != null) {
            return "[username='" + model.username + "' token='" + model.accessToken + "' refresh='" + model.refreshToken + "']"
        }
        else {
            return "[null]"
        }
    }

    def this(accessToken: String, refreshToken: String, expiresInSeconds: java.lang.Long) {
        this()
        model = new CredentialM
        model.accessToken = accessToken
        model.refreshToken = refreshToken
        model.expires = if(expiresInSeconds != null) TimeHelper.now.plusSeconds(expiresInSeconds.intValue()) else null
    }

    def this(apiInternal: Credential) {
        this()
        model = new CredentialM
        if (apiInternal != null) {
            model.accessToken = apiInternal.getAccessToken
            model.refreshToken = apiInternal.getRefreshToken
            model.expires = if(apiInternal.getExpiresInSeconds != null) TimeHelper.now.plusSeconds(apiInternal.getExpiresInSeconds.intValue()) else null
        }
    }

    def isValid: Boolean = {
        return model != null &&
            model.accessToken != null && !model.accessToken.isEmpty &&
            ((model.refreshToken != null && !model.refreshToken.isEmpty) || !GoogleAuthProvider.isExpired(model.expires))
    }

}
