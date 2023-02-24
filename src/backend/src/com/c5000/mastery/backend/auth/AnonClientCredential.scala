package com.c5000.mastery.backend.auth

import com.c5000.mastery.backend.anon.AnonCredentialHelper
import com.c5000.mastery.shared.data.auth.AuthProviderType

class AnonClientCredential(usernameParam: String, passwordClearParam: String) extends ClientCredential {

    def provider = AuthProviderType.ANON

    val username = usernameParam.toLowerCase
    val passwordClear = passwordClearParam

    override def toString = "[anon username='" + username + "' #passwordClear='" + AnonCredentialHelper.getHash(passwordClear, username) + "']"

    override def seemsValid: Boolean = {
        return username != null && username.length > 0 && AnonCredentialHelper.isValidPassword(passwordClear)
    }

    override def equals(other: ClientCredential): Boolean = {
        if (other == null || !other.isInstanceOf[ClientCredential])
            return false
        val casted = other.asInstanceOf[AnonClientCredential]
        return casted.username == username && casted.passwordClear == passwordClear
    }

}
