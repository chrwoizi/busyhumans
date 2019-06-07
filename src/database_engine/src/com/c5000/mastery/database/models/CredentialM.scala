package com.c5000.mastery.database.models

import com.c5000.mongopa.MpaModel
import org.joda.time.DateTime

@MpaModel
class CredentialM extends UniqueIdModelBase {

    var username: String = null
    var accessToken: String = null
    var accessTokenSecret: String = null
    var refreshToken: String = null
    var expires: DateTime = null

}
