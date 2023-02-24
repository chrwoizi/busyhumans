package com.c5000.mastery.backend.auth

import com.c5000.mastery.shared.data.auth.AuthProviderType

class GoogleClientCredential(codeParam: String) extends ClientCredential {

    def provider = AuthProviderType.GOOGLE

    val code = codeParam

    override def toString = "[google code='" + code + "']"

    override def seemsValid: Boolean = {
        return code != null && !code.isEmpty
    }

    override def equals(other: ClientCredential): Boolean = {
        if(other == null || !other.isInstanceOf[ClientCredential])
            return false
        val casted = other.asInstanceOf[GoogleClientCredential]
        return casted.code == code
    }

}
