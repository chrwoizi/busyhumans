package com.c5000.mastery.backend.auth

import _root_.java.util.UUID
import _root_.javax.crypto.Mac
import _root_.javax.crypto.spec.SecretKeySpec
import _root_.javax.servlet.http.{HttpSession, Cookie}
import com.c5000.mastery.backend.facebook.{Facebook, FbsrCookie, PrivateFacebookConfig}
import com.c5000.mastery.backend.Json
import com.c5000.mastery.backend.services.{AuthSessionData, HasServiceLogger}
import com.c5000.mastery.database.Database
import com.c5000.mastery.database.models.AccountM
import com.c5000.mastery.database.queries.AccountQ
import com.c5000.mastery.database.updates.AccountU
import com.c5000.mastery.shared.data.auth.{AuthProviderType, AuthStatus}
import com.c5000.mastery.shared.PublicFacebookConfig
import com.restfb.types.User
import org.apache.commons.codec.binary.Base64

class FacebookAuthProvider extends IAuthProvider with HasServiceLogger {

    def typ = AuthProviderType.FACEBOOK

    def checkIntegrity(cookies: Array[Cookie], credential: ClientCredential): Boolean = {
        return !PrivateFacebookConfig.CHECK_FB_SIGNED_REQUEST || validateFacebookSignedRequest(cookies, credential.asInstanceOf[FacebookClientCredential].uid)
    }

    def toServerCredential(authData: AuthSessionData, credential: ClientCredential): ServerCredential = {
        return new FacebookServerCredential(credential.asInstanceOf[FacebookClientCredential])
    }

    def getUserInfo(credential: ServerCredential, authData: AuthSessionData): Object = {
        val pre = credential.asInstanceOf[FacebookServerCredential]
        val user: User = Facebook.getUser(pre.model.accessToken)
        if (user.getId != pre.model.username) {
            logger.warn("username '" + pre.model.username + "' does not match access token uid '" + user.getId + "'")
            return null
        }
        return user
    }

    def getAccountId(userInfo: Object): UUID = {
        return AccountQ.getAccountIdByFacebookUsername(userInfo.asInstanceOf[User].getId)
    }

    def saveCredential(accountId: UUID, credential: ServerCredential): Boolean = {
        if (credential.isValid) {
            AccountU.setFacebookCredential(accountId, credential.model)
            return true
        }
        else {
            AccountU.setFacebookCredential(accountId, null)
            return false
        }
    }

    def setCredential(account: AccountM, credential: ServerCredential): Boolean = {
        if (credential.isValid) {
            account.facebookCredential = credential.model
            return true
        }
        else {
            account.facebookCredential = null
            return false
        }
    }

    def loadCredential(accountId: UUID): ServerCredential = {
        val account = Database.load(classOf[AccountM], accountId)
        if (account == null) {
            logger.warn("Could not find account '" + accountId + "' while checking facebook auth.")
            return new FacebookServerCredential()
        }
        return new FacebookServerCredential(account.facebookCredential)
    }

    def getAuthorizationUrl(authData: AuthSessionData): String = null

    private def validateFacebookSignedRequest(cookies: Array[Cookie], facebookUid: String): Boolean = {
        val cookieOpt = cookies.find(it => it.getName == "fbsr_" + PublicFacebookConfig.FACEBOOK_APP_ID)
        if (cookieOpt.isDefined) {
            val cookie = cookieOpt.get
            val fbsrStr = cookie.getValue
            val fbsrSignature = fbsrStr.substring(0, fbsrStr.indexOf("."))
            val fbsrPayload = fbsrStr.substring(fbsrStr.indexOf(".") + 1)
            val decodedFbsrSignature = new String(new Base64(true).decode(fbsrSignature.getBytes("UTF-8")))
            val decodedFbsrPayload = new String(new Base64(true).decode(fbsrPayload.getBytes("UTF-8")))

            val fbsr: FbsrCookie = Json.parse(classOf[FbsrCookie], decodedFbsrPayload)

            if (!verifyFbsrSignature(decodedFbsrSignature, fbsrPayload, fbsr)) {
                logger.warn("wrong fbsr signature '" + decodedFbsrSignature + "' for fbsr payload '" + fbsrPayload + "'.")
                return false
            }

            if (fbsr.user_id != facebookUid) {
                logger.warn("username '" + facebookUid + "' does not match fbsr cookie user_id '" + fbsr.user_id + "'")
                return false
            }

            return true
        }
        return false
    }

    private def verifyFbsrSignature(signature: String, payload64enc: String, data: FbsrCookie): Boolean = {
        val secretKey: SecretKeySpec = new SecretKeySpec(PrivateFacebookConfig.FACEBOOK_APP_SECRET.getBytes("UTF-8"), "HmacSHA256")
        val mac: Mac = Mac.getInstance("HmacSHA256")
        mac.init(secretKey)
        val hmacData: Array[Byte] = mac.doFinal(payload64enc.getBytes("UTF-8"))
        new String(hmacData).equals(signature)
    }
}
