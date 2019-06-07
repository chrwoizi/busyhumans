package com.c5000.mastery.backend.auth

import _root_.java.util.UUID
import _root_.javax.servlet.http.{Cookie, HttpSession}
import com.c5000.mastery.backend.services.AuthSessionData
import com.c5000.mastery.database.models.AccountM
import com.c5000.mastery.shared.data.auth.AuthProviderType

trait IAuthProvider {

    def typ: AuthProviderType

    /**
     * checks signature if applicable
     */
    def checkIntegrity(cookies: Array[Cookie], credential: ClientCredential): Boolean

    /**
     * converts the client credential to server credential. may send a request to the provider to get the actual access tokens.
     */
    def toServerCredential(authData: AuthSessionData, credential: ClientCredential): ServerCredential

    /**
     * gets basic user info from the provider
     */
    def getUserInfo(credential: ServerCredential, authData: AuthSessionData): Object

    /**
     * gets the account id for the given user. the user info must be fetched with getUserInfo.
     */
    def getAccountId(userInfo: Object): UUID

    /**
     * writes the credential to database
     */
    def saveCredential(accountId: UUID, credential: ServerCredential): Boolean

    /**
     * writes the credential to the in-memory account model. does not write to database.
     */
    def setCredential(account: AccountM, credential: ServerCredential): Boolean

    /**
     * loads the credential from the database
     */
    def loadCredential(accountId: UUID): ServerCredential

    /**
     * build a url for oauth if applicable
     */
    def getAuthorizationUrl(authData: AuthSessionData): String
}
