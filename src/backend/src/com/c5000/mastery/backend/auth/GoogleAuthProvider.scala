package com.c5000.mastery.backend.auth

import _root_.java.util.UUID
import _root_.javax.servlet.http.{HttpSession, Cookie}
import com.c5000.mastery.backend.google.{GoogleCredentialStore, PrivateGoogleConfig}
import com.c5000.mastery.backend.services.{AuthSessionData, HasServiceLogger}
import com.c5000.mastery.backend.TimeHelper
import com.c5000.mastery.database.models.AccountM
import com.c5000.mastery.database.queries.AccountQ
import com.c5000.mastery.database.updates.AccountU
import com.c5000.mastery.shared.data.auth.AuthProviderType
import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.http.HttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson.JacksonFactory
import com.google.api.client.json.JsonFactory
import org.joda.time.DateTime

object GoogleAuthProvider {

    val flow: GoogleAuthorizationCodeFlow = {
        val transport: HttpTransport = new NetHttpTransport
        val jsonFactory: JsonFactory = new JacksonFactory
        val builder = new GoogleAuthorizationCodeFlow.Builder(
            transport,
            jsonFactory,
            PrivateGoogleConfig.GOOGLE_APP_ID,
            PrivateGoogleConfig.GOOGLE_APP_SECRET,
            PrivateGoogleConfig.GOOGLE_SCOPE)
        builder.setCredentialStore(new GoogleCredentialStore)
        builder.build()
    }

    def isExpired(expires: DateTime): Boolean = {
        return expires == null || TimeHelper.now.isAfter(expires)
    }

    def isExpired(credential: Credential): Boolean = {
        return credential.getExpiresInSeconds == null || credential.getExpiresInSeconds <= 0
    }

    def isValid(credential: Credential): Boolean = {
        return credential != null &&
            credential.getAccessToken != null && !credential.getAccessToken.isEmpty &&
            ((credential.getRefreshToken != null && !credential.getRefreshToken.isEmpty) || !isExpired(credential))
    }

}

class GoogleAuthProvider extends IAuthProvider with HasServiceLogger {

    def typ = AuthProviderType.GOOGLE

    def checkIntegrity(cookies: Array[Cookie], credential: ClientCredential): Boolean = {
        return true
    }

    def toServerCredential(authData: AuthSessionData, credential: ClientCredential): ServerCredential = {
        val pre = credential.asInstanceOf[GoogleClientCredential]
        if (pre != null && pre.seemsValid) {
            val request = GoogleAuthProvider.flow.newTokenRequest(pre.code)
            request.setRedirectUri(PrivateGoogleConfig.GOOGLE_REDIRECT)
            val response = request.execute
            return new GoogleServerCredential(response.getAccessToken, response.getRefreshToken, response.getExpiresInSeconds)
        }
        return new GoogleServerCredential
    }

    def getUserInfo(credential: ServerCredential, authData: AuthSessionData): Object = {
        return null
    }

    def getAccountId(userInfo: Object): UUID = {
        return null
    }

    def saveCredential(accountId: UUID, credential: ServerCredential): Boolean = {
        if (credential.isValid) {
            AccountU.setGoogleCredential(accountId, credential.model)
            return true
        }
        else {
            AccountU.setGoogleCredential(accountId, null)
            return false
        }
    }

    def setCredential(account: AccountM, credential: ServerCredential): Boolean = {
        if (credential.isValid) {
            account.googleCredential = credential.model
            return true
        }
        else {
            account.googleCredential = null
            return false
        }
    }

    def loadCredential(accountId: UUID): ServerCredential = {
        return new GoogleServerCredential(loadApiCredential(accountId))
    }

    def loadApiCredential(accountId: UUID): Credential = {
        if (accountId == null)
            return null

        val credential = GoogleAuthProvider.flow.loadCredential(accountId.toString)
        if (credential == null)
            return null

        if (GoogleAuthProvider.isExpired(credential))
            if (!credential.refreshToken())
                return null

        return credential
    }

    def getAuthorizationUrl(authData: AuthSessionData): String = {
        val url = GoogleAuthProvider.flow.newAuthorizationUrl
        if (url == null)
            return null
        url.setRedirectUri(PrivateGoogleConfig.GOOGLE_REDIRECT)
        return url.build
    }

}
