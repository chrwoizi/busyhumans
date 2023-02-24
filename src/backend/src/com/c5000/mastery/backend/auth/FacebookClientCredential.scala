package com.c5000.mastery.backend.auth

import com.c5000.mastery.shared.data.auth.AuthProviderType

class FacebookClientCredential(uidParam: String, accessTokenParam: String) extends ClientCredential {

    def provider = AuthProviderType.FACEBOOK

    val uid = uidParam
    val accessToken = accessTokenParam

    override def toString = "[facebook uid='" + uid + "' token='" + accessToken + "']"

    override def seemsValid: Boolean = {
        return uid != null && !uid.isEmpty &&
            accessToken != null && !accessToken.isEmpty
    }

    override def equals(other: ClientCredential): Boolean = {
        if(other == null || !other.isInstanceOf[ClientCredential])
            return false
        val casted = other.asInstanceOf[FacebookClientCredential]
        return casted.uid == uidParam && casted.accessToken == accessToken
    }

}
