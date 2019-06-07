package com.c5000.mastery.backend.services

import _root_.java.util
import com.c5000.mastery.backend.anon.{PrivateRecaptchaConfig, AnonCredentialHelper, AnonUser}
import com.c5000.mastery.backend.auth._
import com.c5000.mastery.backend.{Tokenizer, TimeHelper}
import com.c5000.mastery.database.models.{PersonM, UniqueIdModelBase, AccountM}
import com.c5000.mastery.database.queries.AccountQ
import com.c5000.mastery.database.updates.AccountU
import com.c5000.mastery.database.Database
import com.c5000.mastery.shared.data.auth._
import com.c5000.mastery.shared.Config

import com.c5000.mastery.shared.data.base.{AnonRegisterResultD, ResourceD}
import _root_.javax.servlet.http.{HttpServletResponse, Cookie, HttpSession}
import org.apache.commons.lang.RandomStringUtils
import util.UUID
import net.tanesha.recaptcha.ReCaptchaImpl


object AuthS extends HasServiceLogger {

    def authWithNewAnon[Person <: UniqueIdModelBase](remoteAddr: String, recaptchaChallenge: String, recaptchaResponse: String,
                                                     usernameCased: String, passwordClear: String,
                                                     name: String, pictureToken: String,
                                                     result: AnonRegisterResultD,
                                                     session: HttpSession, response: HttpServletResponse, cookies: Array[Cookie],
                                                     upsertPerson: (AccountM, Object) => Person) {
        if (remoteAddr == null || recaptchaChallenge == null
            || session == null || cookies == null) {
            logger.warn("invalid registration request: '" + remoteAddr + "' '" + recaptchaChallenge + "' '" + session + "' '" + cookies + "'. access denied.")
            result.registrationError = "An unexpected error occured"
            result.status = AuthStatus.NOT_AUTHORIZED
            return
        }
        if (pictureToken == null || pictureToken.isEmpty) {
            logger.warn("invalid registration request: picture not set")
            result.registrationError = "A profile picture must be provided"
            result.status = AuthStatus.NOT_AUTHORIZED
            return
        }
        if (!AnonCredentialHelper.isValidUsername(usernameCased)) {
            logger.warn("invalid registration request: username '" + usernameCased + "' invalid")
            result.registrationError = "The email address is invalid"
            result.status = AuthStatus.NOT_AUTHORIZED
            return
        }
        if (!AnonCredentialHelper.isValidPassword(passwordClear)) {
            logger.warn("invalid registration request: password invalid")
            result.registrationError = "The password is invalid"
            result.status = AuthStatus.NOT_AUTHORIZED
            return
        }
        if (!AnonCredentialHelper.isValidName(name)) {
            logger.warn("invalid registration request: name '" + name + "' invalid")
            result.registrationError = "The name is invalid"
            result.status = AuthStatus.NOT_AUTHORIZED
            return
        }
        if (recaptchaResponse == null) {
            logger.warn("invalid registration request: captcha '" + recaptchaResponse + "' invalid")
            result.registrationError = "Please enter the captcha"
            result.status = AuthStatus.NOT_AUTHORIZED
            return
        }

        val username = usernameCased.toLowerCase

        try {
            if (AccountQ.getAccountIdByAnonUsername(username) != null) {
                logger.warn("cannot create new anon user with existing username '" + username + "'. access denied.")
                clearAuthData(session)
                result.status = AuthStatus.NOT_AUTHORIZED
                result.registrationError = "This email address is already in use"
                return
            }

            val reCaptcha = new ReCaptchaImpl
            reCaptcha.setPrivateKey(PrivateRecaptchaConfig.PRIVATE_KEY)
            val reCaptchaResponse = reCaptcha.checkAnswer(remoteAddr, recaptchaChallenge, recaptchaResponse)
            if (!reCaptchaResponse.isValid) {
                logger.warn("invalid recaptcha response '" + recaptchaResponse + "'. access denied.")
                clearAuthData(session)
                result.status = AuthStatus.NOT_AUTHORIZED
                result.registrationError = "Wrong captcha"
                return
            }

            val authData = getAuthData(session)

            if (authData.credential != null) {
                // invalidate previous auth state
                authData.credential = null
            }

            // remove picture reference so it will not be deleted on the next upload of another picture
            authData.uploadedRegistrationPictureId = null

            val tokenizer = new Tokenizer(session, "upload", false)
            val picture = tokenizer.detokenize(pictureToken).asInstanceOf[ResourceD]
            if (picture == null) {
                logger.warn("new anon user picture not found in session. access denied.")
                clearAuthData(session)
                result.status = AuthStatus.NOT_AUTHORIZED
                result.registrationError = "No picture set"
                return
            }

            // save account data for later use by auth provider
            authData.newAnonUser = new AnonUser
            authData.newAnonUser.username = username.trim
            authData.newAnonUser.name = name.trim
            authData.newAnonUser.picture = ResourcePresenter.unpresent(picture)

            // authorize to create new account
            setAuth(result, new AnonAuthProvider, new AnonClientCredential(username, passwordClear), session, response, cookies, upsertPerson)
        }
        catch {
            case e: Throwable => {
                logger.warn("error while authenticating with new anon user. session will be cleared.")
                clearAuthData(session)
                result.status = AuthStatus.NOT_AUTHORIZED
            }
        }
    }

    def setAuth[Person <: UniqueIdModelBase](authResult: AuthResultD, authProvider: IAuthProvider, clientCredential: ClientCredential, session: HttpSession, response: HttpServletResponse, cookies: Array[Cookie], upsertPerson: (AccountM, Object) => Person) {

        if (session == null || cookies == null) {
            authResult.status = AuthStatus.NOT_AUTHORIZED
            return
        }

        try {
            val authData = getAuthData(session)

            if (authData.credential != null) {
                if (clientCredential != null) {
                    if (authData.credential.equals(clientCredential)) {
                        if (authWithSession(authData, session, response, authResult)) {
                            logger.info("received known credential '" + authData.credential + "' --> granted.")
                            return
                        }
                        else {
                            logger.info("received known but invalid credential '" + authData.credential + "' --> denied.")
                        }
                    }
                    else {
                        logger.info("received changed credential '" + authData.credential + "' --> checking.")
                        if (authWithParams(authProvider, cookies, clientCredential, authData, session, response, upsertPerson, authResult)) {
                            logger.info("checked changed credential '" + clientCredential + "' --> granted.")
                            return
                        }
                        else {
                            logger.info("checked new credential '" + clientCredential + "' --> denied.")
                        }
                    }
                }
                else {
                    if (authWithSession(authData, session, response, authResult)) {
                        logger.info("has known credential in session '" + authData.credential + "' --> granted.")
                        return
                    }
                    else if (authWithCookie(authData, session, response, cookies, authResult)) {
                        logger.info("has known credential in cookies --> granted.")
                        return
                    }
                    else {
                        logger.info("has known but invalid credential '" + authData.credential + "' --> denied.")
                    }
                }
            }
            else {
                if (clientCredential != null) {
                    logger.info("received new credential '" + clientCredential + "' --> checking.")
                    if (authWithParams(authProvider, cookies, clientCredential, authData, session, response, upsertPerson, authResult)) {
                        logger.info("checked new credential '" + clientCredential + "' --> granted.")
                        return
                    }
                    else {
                        logger.info("checked new credential '" + clientCredential + "' --> denied.")
                    }
                }
                else {
                    if (authWithCookie(authData, session, response, cookies, authResult)) {
                        logger.info("has known credential in cookies --> granted.")
                        return
                    }
                    else {
                        logger.info("received no credential --> denied.")
                        clearAuthData(session)
                        authResult.status = AuthStatus.NOT_AUTHORIZED
                        return
                    }
                }
            }
        }
        catch {
            case e: Throwable => {
                logger.warn("error while authenticating. session will be cleared.")
                clearAuthData(session)
                throw e
            }
        }

        authResult.status = AuthStatus.NOT_AUTHORIZED
    }

    private def authWithParams[Person <: UniqueIdModelBase](authProvider: IAuthProvider, cookies: Array[Cookie], clientCredential: ClientCredential, authData: AuthSessionData, session: HttpSession, response: HttpServletResponse, upsertPerson: (AccountM, Object) => Person, authResult: AuthResultD): Boolean = {

        if (authProvider.typ != clientCredential.provider) {
            logger.error("credential type " + clientCredential.provider + " does not match provider type " + authProvider.typ)
        }

        if (clientCredential.seemsValid && authProvider.checkIntegrity(cookies, clientCredential)) {

            val credential = authProvider.toServerCredential(authData, clientCredential)
            if (credential == null || !credential.isValid) {
                logger.warn("could not find matching server credential for client credential '" + clientCredential + "'. session will be cleared.")
                clearAuthData(session)
                authResult.status = AuthStatus.NOT_AUTHORIZED
                return false
            }

            val userInfo = authProvider.getUserInfo(credential, authData)
            if (userInfo == null) {
                logger.warn("could not get user info for '" + credential + "'.")
                clearAuthData(session)
                authResult.status = AuthStatus.NOT_AUTHORIZED
                return false
            }

            // update account
            val account = upsertAccount(authProvider, userInfo, credential)

            // make sure that a person is attached to the account
            val person = upsertPerson(account, userInfo)

            return finalizeAuth(authData, authResult, clientCredential, clientCredential.provider, account, person.id, session, response)
        }
        else {
            logger.warn("credential integrity check fail. session will be cleared.")
            clearAuthData(session)
            authResult.status = AuthStatus.NOT_AUTHORIZED
            return false
        }
    }

    def setSessionAuthData[Person <: UniqueIdModelBase](authData: AuthSessionData, clientCredential: ClientCredential, provider: AuthProviderType, accountId: UUID, personId: UUID, email: String, psid: String) {
        authData.credential = clientCredential
        authData.provider = provider
        authData.accountId = accountId
        authData.personId = personId
        authData.email = email
        authData.psid = psid
    }

    private def authWithSession[Person <: UniqueIdModelBase](authData: AuthSessionData, session: HttpSession, response: HttpServletResponse, authResult: AuthResultD): Boolean = {
        val accountId = authData.accountId
        if (accountId == null) {
            logger.warn("Account id not set in session.")
            authResult.status = AuthStatus.NOT_AUTHORIZED
            return false
        }

        val account = Database.load(classOf[AccountM], accountId)
        if (account == null) {
            logger.warn("Account " + accountId + " from session not found in database.")
            authResult.status = AuthStatus.NOT_AUTHORIZED
            return false
        }

        return finalizeAuth(authData, authResult, authData.credential, authData.provider, account, authData.personId, session, response)
    }

    private def authWithCookie[Person <: UniqueIdModelBase](authData: AuthSessionData, session: HttpSession, response: HttpServletResponse, cookies: Array[Cookie], authResult: AuthResultD): Boolean = {

        val cookie = cookies.find(_.getName == Config.AUTH_COOKIE)
        if (cookie.isDefined) {
            val psid = cookie.get.getValue
            if (psid != null && !psid.isEmpty) {
                val accountId = AccountQ.getAccountIdByPersistentSessionId(psid)
                if (accountId != null) {

                    val account = Database.load(classOf[AccountM], accountId)
                    if (account == null) {
                        logger.warn("Account " + accountId + " from cookie not found in database.")
                        authResult.status = AuthStatus.NOT_AUTHORIZED
                        return false
                    }

                    val person = Database.load(classOf[PersonM], account.person)
                    if (person == null) {
                        logger.warn("Could not find person " + account.person + " of account " + accountId + ".")
                        authResult.status = AuthStatus.NOT_AUTHORIZED
                        return false
                    }

                    // try to determine the a valid auth provider for the account by looking at existing credentials
                    var provider = AuthProviderType.NONE
                    if (account.anonCredential != null)
                        provider = AuthProviderType.ANON
                    else if (account.facebookCredential != null)
                        provider = AuthProviderType.FACEBOOK
                    else if (account.twitterCredential != null)
                        provider = AuthProviderType.TWITTER

                    return finalizeAuth(authData, authResult, authData.credential, provider, account, person.id, session, response)
                }
                else {
                    logger.warn("Received unknown psid='" + psid + "'.")
                }
            }
        }

        return false
    }

    def finalizeAuth[Person <: UniqueIdModelBase](authData: AuthSessionData, authResult: AuthResultD,
                                                  clientCredential: ClientCredential, provider: AuthProviderType,
                                                  account: AccountM, personId: UUID,
                                                  session: HttpSession, response: HttpServletResponse): Boolean = {

        AccountU.setLastLogin(account.id, TimeHelper.now)

        if (account.psid == null || account.psid.isEmpty) {
            account.psid = RandomStringUtils.randomAlphanumeric(48)
            AccountU.setPersistentSessionId(account.id, account.psid)
        }

        val cookie = new Cookie(Config.AUTH_COOKIE, account.psid)
        cookie.setMaxAge(32000000)
        response.addCookie(cookie)

        // save auth data for faster client recognition in future rpc calls
        setSessionAuthData(authData, clientCredential, provider, account.id, personId, account.email, account.psid)

        authResult.provider = provider
        authResult.anonUsername = if (account.anonCredential != null) account.anonCredential.username else null

        if (account.banned) {
            logger.info("Account '" + account.id + "' is banned. session will be cleared.")
            clearAuthData(session)
            authResult.status = AuthStatus.ACCESS_DENIED
            return true
        }

        if (account.confirmedTosVersion == null || account.confirmedTosVersion < Config.CURRENT_TOS_VERSION) {
            authResult.status = AuthStatus.NEEDS_TOS_CONFIRMATION
            return true
        }

        authResult.status = AuthStatus.AUTHORIZED
        return true
    }

    def getAuthData(session: HttpSession): AuthSessionData = {
        var authData = session.getAttribute("auth").asInstanceOf[AuthSessionData]
        if (authData == null) {
            authData = new AuthSessionData()
            session.setAttribute("auth", authData)
        }
        return authData
    }

    def clearAuthData[Person <: UniqueIdModelBase](session: HttpSession) {
        getAuthData(session).unauth()
    }

    private def upsertAccount(authProvider: IAuthProvider, userInfo: Object, credential: ServerCredential): AccountM = {
        logger.trace("looking for account '" + credential + "' via provider " + authProvider.typ + " and userInfo '" + userInfo + "'.")
        val accountId = authProvider.getAccountId(userInfo)
        var account: AccountM = null
        if (accountId != null) {
            account = Database.load(classOf[AccountM], accountId)
            if (account == null) {
                logger.error("could not find account " + accountId + " for credential update.")
                return null
            }
            logger.info("updating account '" + accountId + "' credential to '" + credential + "'.")
            authProvider.saveCredential(accountId, credential)
        }
        else {
            account = new AccountM()
            account.joindate = TimeHelper.now
            authProvider.setCredential(account, credential)
            logger.info("creating account '" + account.id + "' with credential '" + credential + "'.")
            Database.save(account)
        }
        return account
    }

    def unauth(session: HttpSession, response: HttpServletResponse, userAccountId: UUID): AuthStatus = {
        logger.info("user requested logout. auth data will be cleared.")
        clearAuthData(session)

        if (userAccountId != null) {
            AccountU.setPersistentSessionId(userAccountId, null)
            response.addCookie(new Cookie(Config.AUTH_COOKIE, null))
        }

        return AuthStatus.NOT_AUTHORIZED
    }

    def confirmTos(accountId: util.UUID) {
        val account = Database.load(classOf[AccountM], accountId)
        if (account == null) {
            logger.warn("Account '" + accountId + "' not found.")
            return
        }

        logger.info("Account '" + accountId + "' agrees to terms of service version " + Config.CURRENT_TOS_VERSION + ".")
        AccountU.setConfirmedTos(account, Config.CURRENT_TOS_VERSION)
    }

    def checkCredential(accountId: UUID, session: HttpSession, authProvider: IAuthProvider): CredentialCheckResultD = {
        val authData = getAuthData(session)
        if (accountId != null) {
            val credential = authProvider.loadCredential(accountId)
            return present(credential.isValid, authProvider, authData)
        }
        else {
            return present(false, authProvider, authData)
        }
    }

    def validateCredential(accountId: UUID, session: HttpSession, authProvider: IAuthProvider, clientCredential: ClientCredential): Boolean = {
        val authData = getAuthData(session)
        if (accountId != null) {
            val credential = authProvider.toServerCredential(authData, clientCredential)
            val isValid = authProvider.saveCredential(accountId, credential)
            return isValid
        }
        else {
            return false
        }
    }

    private def present(isValid: Boolean, authProvider: IAuthProvider, authData: AuthSessionData): CredentialCheckResultD = {
        val result = new CredentialCheckResultD
        if (isValid) {
            result.valid = true
        }
        else {
            result.valid = false
            result.authUrl = authProvider.getAuthorizationUrl(authData)
        }
        return result
    }

    def setEmailAddress(accountId: UUID, email: String, oldPasswordClear: String, newPasswordClear: String, session: HttpSession): EmailSetResult = {
        if (accountId == null) {
            logger.error("No account id given while setting email address.")
            return EmailSetResult.UNKNOWN
        }

        if (email == null || !email.matches(Config.EMAIL_REGEX)) {
            logger.info("User tried to set invalid email address '" + email + "'.")
            return EmailSetResult.UNKNOWN
        }

        val account = Database.load(classOf[AccountM], accountId)
        if (account == null) {
            logger.warn("Could not find account " + accountId + " to set new email.")
            return EmailSetResult.UNKNOWN
        }

        var newPasswordHash: String = null
        if (account.anonCredential != null) {
            val oldPasswordHash = AnonCredentialHelper.getHash(oldPasswordClear, account.anonCredential.accessTokenSecret)
            if (account.anonCredential.accessToken != oldPasswordHash) {
                logger.warn("User entered wrong password while setting email address for account " + accountId + ".")
                return EmailSetResult.WRONG_PASSWORD
            }

            // make new password
            newPasswordHash = AnonCredentialHelper.getHash(newPasswordClear, account.anonCredential.accessTokenSecret)
        }

        logger.info("User sets email address '" + email + "'.")
        if (!AccountU.setEmailAddress(accountId, email, newPasswordHash)) {
            logger.warn("Error while setting email address for account " + accountId + ".")
            return EmailSetResult.UNKNOWN
        }

        getAuthData(session).email = email
        return EmailSetResult.OK
    }

}
