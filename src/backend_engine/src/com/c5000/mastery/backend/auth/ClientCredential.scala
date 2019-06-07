package com.c5000.mastery.backend.auth

import com.c5000.mastery.shared.data.auth.AuthProviderType


abstract class ClientCredential {

    def provider: AuthProviderType
    def seemsValid: Boolean
    def equals(other: ClientCredential): Boolean

}
