package com.c5000.mastery.backend.anon

import com.c5000.mastery.shared.Config
import org.apache.commons.codec.digest.DigestUtils
import util.Random

object AnonCredentialHelper {

    def isValidPassword(passwordClear: String): Boolean = {
        return passwordClear != null && passwordClear.length >= Config.ANON_PASSWORD_LENGTH_MIN && passwordClear.length <= Config.ANON_PASSWORD_LENGTH_MAX
    }

    def isValidUsername(username: String): Boolean = {
        return username != null && username.matches(Config.EMAIL_REGEX)
    }

    def isValidName(name: String): Boolean = {
        return name != null && name.trim().length >= Config.ANON_NAME_LENGTH_MIN && name.trim().length <= Config.ANON_NAME_LENGTH_MAX
    }

    def getRandomSalt(username: String): String = {
        return Random.alphanumeric.take(16).mkString
    }

    def getHash(passwordClear: String, salt: String): String = {
        return DigestUtils.sha256Hex(passwordClear + salt)
    }

}
