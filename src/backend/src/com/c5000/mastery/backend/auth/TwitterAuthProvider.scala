package com.c5000.mastery.backend.auth

import _root_.java.util.UUID
import _root_.javax.servlet.http.{Cookie, HttpSession}
import com.c5000.mastery.backend.services.{AuthSessionData, HasServiceLogger}
import com.c5000.mastery.backend.twitter.{TwitterUser, TwitterS}
import com.c5000.mastery.database.Database
import com.c5000.mastery.database.models.AccountM
import com.c5000.mastery.database.queries.AccountQ
import com.c5000.mastery.database.updates.AccountU
import com.c5000.mastery.shared.data.auth.AuthProviderType
import org.scribe.model.{Token, Verifier}

class TwitterAuthProvider extends IAuthProvider with HasServiceLogger {

    def typ = AuthProviderType.TWITTER

    def checkIntegrity(cookies: Array[Cookie], credential: ClientCredential): Boolean = {
        return true
    }

    def toServerCredential(authData: AuthSessionData, credential: ClientCredential): ServerCredential = {
        val requestToken = authData.twitterRequestToken
        if (requestToken == null) {
            logger.warn("tried to set oauth verifier without request token in session")
            return new TwitterServerCredential
        }

        val v = new Verifier(credential.asInstanceOf[TwitterClientCredential].verifier)
        val accessToken = TwitterS.service.getAccessToken(requestToken.asInstanceOf[Token], v)

        if (accessToken != null && accessToken.getToken != null) {
            val data = TwitterS.verifyCredentials(accessToken)
            if (data != null && data.id != null) {
                return new TwitterServerCredential(data.id, accessToken.getToken, accessToken.getSecret)
            }
        }

        return new TwitterServerCredential
    }

    def getUserInfo(credential: ServerCredential, authData: AuthSessionData): Object = {
        return TwitterS.verifyCredentials(new Token(credential.model.accessToken, credential.model.accessTokenSecret))
    }

    def getAccountId(userInfo: Object): UUID = {
        return AccountQ.getAccountIdByTwitterUsername(userInfo.asInstanceOf[TwitterUser].id)
    }

    def saveCredential(accountId: UUID, credential: ServerCredential): Boolean = {
        if (credential.isValid) {
            AccountU.setTwitterCredential(accountId, credential.model)
            return true
        }
        else {
            AccountU.setTwitterCredential(accountId, null)
            return false
        }
    }

    def setCredential(account: AccountM, credential: ServerCredential): Boolean = {
        if (credential.isValid) {
            account.twitterCredential = credential.model
            return true
        }
        else {
            account.twitterCredential = null
            return false
        }
    }

    def loadCredential(accountId: UUID): ServerCredential = {
        val account = Database.load(classOf[AccountM], accountId)
        if (account == null) {
            logger.warn("Could not find account '" + accountId + "' while checking twitter auth.")
            return new TwitterServerCredential
        }
        return new TwitterServerCredential(account.twitterCredential)
    }

    def getAuthorizationUrl(authData: AuthSessionData): String = {
        val requestToken = TwitterS.service.getRequestToken
        authData.twitterRequestToken = requestToken
        return TwitterS.service.getAuthorizationUrl(requestToken)
    }

}
