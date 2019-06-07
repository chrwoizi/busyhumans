package com.c5000.mastery.database.versions

import org.slf4j.LoggerFactory
import com.mongodb.casbah.commons.MongoDBObject
import com.c5000.mastery.database.models.AccountM
import com.c5000.mongopa.MpaModel
import com.c5000.mastery.database.{SpecialUsers, Database}
import com.mongodb.DBCollection
import java.util.UUID
import com.mongodb.casbah._
import com.c5000.mastery.shared.Config

/**
 * Moves and renames facebook credentials within ACCOUNT, ADMIN and BLACKLIST
 */
object Version9 {
    private val log = LoggerFactory.getLogger(getClass)

    def update() {
        val accountCollection = Database.mongodb.getCollection(classOf[AccountM].getAnnotation(classOf[MpaModel]).collection())
        renameButSystem(accountCollection, "username", "facebookCredential.username")
        renameButSystem(accountCollection, "token", "facebookCredential.accessToken")
        createButSystem(accountCollection, "facebookCredential.id", UUID.randomUUID())
        createButSystem(accountCollection, "facebookCredential.deleted", false)
        createButSystem(accountCollection, "facebookCredential.refreshToken", null)
        createButSystem(accountCollection, "facebookCredential.expires", null)
        renameButSystem(accountCollection, "googleAccessToken", "googleCredential.accessToken")
        renameButSystem(accountCollection, "googleRefreshToken", "googleCredential.refreshToken")
        renameButSystem(accountCollection, "googleAccessTokenExpiration", "googleCredential.expires")
        createButSystem(accountCollection, "googleCredential.id", UUID.randomUUID())
        createButSystem(accountCollection, "googleCredential.deleted", false)
        createButSystem(accountCollection, "googleCredential.username", null)
        create(accountCollection, "admin", false)
        create(accountCollection, "banned", false)
        remove(accountCollection, "username")
        remove(accountCollection, "token")
        remove(accountCollection, "googleAccessToken")
        remove(accountCollection, "googleRefreshToken")
        remove(accountCollection, "googleAccessTokenExpiration")
        create(accountCollection, "facebookCredential", null)
        create(accountCollection, "googleCredential", null)
        Database.mongodb.getCollection("ADMINS").drop()
        Database.mongodb.getCollection("BLACKLIST").drop()
        Database.mongodb.getCollection("WHITELIST").drop()
    }

    def create[T](collection: DBCollection, field: String, value: T) {
        val query = MongoDBObject(field -> MongoDBObject("$exists" -> false))
        val setter = MongoDBObject("$set" -> MongoDBObject(field -> value))
        val result = collection.updateMulti(query, setter)
        log.info("Rows where a new field in " + collection.getName + " has been created: " + result.getN)
    }

    def createButSystem[T](collection: DBCollection, field: String, value: T) {
        val query = $and(
            MongoDBObject("id" -> MongoDBObject("$ne" -> UUID.fromString(Config.SYS_OBJ_ID))),
            MongoDBObject(field -> MongoDBObject("$exists" -> false))
        )
        val setter = MongoDBObject("$set" -> MongoDBObject(field -> value))
        val result = collection.updateMulti(query, setter)
        log.info("Rows where a new field in " + collection.getName + " has been created: " + result.getN)
    }

    def renameButSystem(collection: DBCollection, oldName: String, newName: String) {
        val query = $and(
            MongoDBObject("id" -> MongoDBObject("$ne" -> UUID.fromString(Config.SYS_OBJ_ID))),
            MongoDBObject(oldName -> MongoDBObject("$exists" -> true))
        )
        val setter = MongoDBObject("$rename" -> MongoDBObject(oldName -> newName))
        val result = collection.updateMulti(query, setter)
        log.info("Rows where a field in " + collection.getName + " has been renamed: " + result.getN)
    }

    def remove(collection: DBCollection, field: String) {
        val query = MongoDBObject(field -> MongoDBObject("$exists" -> true))
        val setter = MongoDBObject("$unset" -> MongoDBObject(field -> 1))
        val result = collection.updateMulti(query, setter)
        log.info("Rows where a field in " + collection.getName + " has been removed: " + result.getN)
    }

}
