package com.c5000.mastery.backend.auth

import com.c5000.mastery.shared.data.auth.AuthProviderType

class TwitterClientCredential(verifierParam: String) extends ClientCredential {

    def provider = AuthProviderType.TWITTER

    val verifier = verifierParam

    override def toString = "[twitter verifier='" + verifier + "']"

    override def seemsValid: Boolean = {
        return verifier != null && !verifier.isEmpty
    }

    override def equals(other: ClientCredential): Boolean = {
        if(other == null || !other.isInstanceOf[ClientCredential])
            return false
        val casted = other.asInstanceOf[TwitterClientCredential]
        return casted.verifier == verifier
    }

}
