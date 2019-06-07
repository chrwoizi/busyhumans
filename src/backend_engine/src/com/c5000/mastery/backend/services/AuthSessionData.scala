package com.c5000.mastery.backend.services

import com.c5000.mastery.backend.anon.AnonUser
import com.c5000.mastery.backend.auth.ClientCredential
import com.c5000.mastery.shared.data.auth.AuthProviderType
import java.util.UUID

class AuthSessionData {

    var credential: ClientCredential = null
    var provider: AuthProviderType = AuthProviderType.NONE
    var psid: String = null
    var twitterRequestToken: Object = null
    var accountId: UUID = null
    var personId: UUID = null
    var newAnonUser: AnonUser = null
    var uploadedRegistrationPictureId: UUID = null
    var email: String = null

    def unauth() {
        credential = null
        provider = AuthProviderType.NONE
        psid = null
        twitterRequestToken = null
        accountId = null
        personId = null
        newAnonUser = null
        email = null
    }
}