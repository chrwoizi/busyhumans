package com.c5000.mastery.backend.facebook

/**
 * facebook signed request cookie payload.
 * https://developers.facebook.com/docs/authentication/signed_request/
 */
class FbsrCookie {
    var algorithm: String = null
    var user_id: String = null
    var code: String = null
    var issued_at: String = null
}