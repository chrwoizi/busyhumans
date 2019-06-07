package com.c5000.mastery.backend.auth

import _root_.java.util.UUID
import _root_.javax.servlet.http.Cookie
import com.c5000.mastery.backend.anon.{AnonCredentialHelper, AnonUser}
import com.c5000.mastery.backend.services.{AuthSessionData, HasServiceLogger}
import com.c5000.mastery.database.Database
import com.c5000.mastery.database.models.{PersonM, AccountM}
import com.c5000.mastery.database.queries.AccountQ
import com.c5000.mastery.database.updates.AccountU
import com.c5000.mastery.shared.Config
import com.c5000.mastery.shared.data.auth.AuthProviderType

class AnonAuthProvider extends IAuthProvider with HasServiceLogger {

    def typ = AuthProviderType.ANON

    def checkIntegrity(cookies: Array[Cookie], credential: ClientCredential): Boolean = {
        return true
    }

    def toServerCredential(authData: AuthSessionData, credential: ClientCredential): ServerCredential = {

        if (credential != null && credential.isInstanceOf[AnonClientCredential] && credential.seemsValid) {
            val cc = credential.asInstanceOf[AnonClientCredential]

            // try to use an existing account
            val accountId = AccountQ.getAccountIdByAnonUsername(cc.username)
            val account = Database.load(classOf[AccountM], accountId)
            if (account != null) {
                if (account.anonCredential != null
                    && account.anonCredential.username == cc.username
                    && account.anonCredential.accessTokenSecret != null) {
                    val passwordHash = AnonCredentialHelper.getHash(cc.passwordClear, account.anonCredential.accessTokenSecret)
                    if (account.anonCredential.accessToken == passwordHash) {
                        return new AnonServerCredential(account.anonCredential)
                    }
                }
            }

            if (authData.newAnonUser != null && authData.newAnonUser.username == cc.username) {
                // generate new credential
                val passwordHashSalt = AnonCredentialHelper.getRandomSalt(cc.username)
                val passwordHash = AnonCredentialHelper.getHash(cc.passwordClear, passwordHashSalt)
                return new AnonServerCredential(cc.username, passwordHash, passwordHashSalt)
            }
        }

        return new AnonServerCredential
    }

    def getUserInfo(credential: ServerCredential, authData: AuthSessionData): Object = {
        if (!credential.isInstanceOf[AnonServerCredential])
            return null

        val sc = credential.asInstanceOf[AnonServerCredential]

        // try to get user info from existing person in database
        val accountId = AccountQ.getAccountIdByAnonUsername(sc.username)
        if (accountId != null) {
            val account = Database.load(classOf[AccountM], accountId)
            if (account != null) {
                if (account.person != null) {
                    val person = Database.load(classOf[PersonM], account.person)
                    if (person != null) {
                        val result = new AnonUser
                        result.username = sc.username
                        result.name = person.name
                        result.picture = person.picture
                        return result
                    }
                }
            }
        }

        // try to get user info from session
        if (authData.newAnonUser != null && authData.newAnonUser.username == sc.username) {
            return authData.newAnonUser
        }

        return null
    }

    def getAccountId(userInfo: Object): UUID = {
        return AccountQ.getAccountIdByAnonUsername(userInfo.asInstanceOf[AnonUser].username)
    }

    def saveCredential(accountId: UUID, credential: ServerCredential): Boolean = {
        if (credential.isValid) {
            AccountU.setAnonCredential(accountId, credential.model)
            return true
        }
        else {
            AccountU.setAnonCredential(accountId, null)
            return false
        }
    }

    def setCredential(account: AccountM, credential: ServerCredential): Boolean = {
        if (credential.isValid) {
            account.anonCredential = credential.model
            if (credential.model != null && credential.model.username.matches(Config.EMAIL_REGEX)) {
                account.email = credential.model.username
            }
            return true
        }
        else {
            account.anonCredential = null
            return false
        }
    }

    def loadCredential(accountId: UUID): ServerCredential = {
        val account = Database.load(classOf[AccountM], accountId)
        if (account == null) {
            logger.warn("Could not find account '" + accountId + "' while checking anon auth.")
            return new AnonServerCredential
        }
        return new AnonServerCredential(account.anonCredential)
    }

    def getAuthorizationUrl(authData: AuthSessionData): String = {
        return null
    }

}
