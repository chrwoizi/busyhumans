package com.c5000.mastery.backend.google

import _root_.java.util.UUID
import com.c5000.mastery.backend.auth.{GoogleServerCredential, GoogleAuthProvider}
import com.c5000.mastery.backend.services.HasServiceLogger
import com.c5000.mastery.backend.TimeHelper
import com.c5000.mastery.database.Database
import com.c5000.mastery.database.models.AccountM
import com.c5000.mastery.database.updates.AccountU
import com.google.api.client.auth.oauth2.{CredentialStore, Credential}
import org.joda.time.Seconds

class GoogleCredentialStore extends CredentialStore with HasServiceLogger {

    def store(userId: String, credential: Credential) {
        val accountId = UUID.fromString(userId)
        val wrapper = new GoogleServerCredential(credential)
        if(wrapper.isValid) {
            AccountU.setGoogleCredential(accountId, wrapper.model)
        }
        else {
            AccountU.setGoogleCredential(accountId, null)
        }
    }

    def load(userId: String, credential: Credential): Boolean = {
        val accountId = UUID.fromString(userId)

        val account = Database.load(classOf[AccountM], accountId)
        if (account == null) {
            logger.warn("Could not find account '" + accountId + "' while checking google auth.")
            return false
        }

        if(account.googleCredential != null) {
            credential.setAccessToken(account.googleCredential.accessToken)
            credential.setRefreshToken(account.googleCredential.refreshToken)
            if (account.googleCredential.expires != null) {
                credential.setExpiresInSeconds(Seconds.secondsBetween(account.googleCredential.expires, TimeHelper.now).getSeconds)
            }
            else {
                credential.setExpiresInSeconds(0)
            }
        }
        else {
            credential.setAccessToken(null)
            credential.setRefreshToken(null)
            credential.setExpiresInSeconds(0)
        }

        return GoogleAuthProvider.isValid(credential)
    }

    def delete(userId: String, credential: Credential) {
        val accountId = UUID.fromString(userId)
        AccountU.setGoogleCredential(accountId, null)
    }

}

