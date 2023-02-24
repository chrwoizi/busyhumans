package com.c5000.mastery.database.versions

import org.slf4j.LoggerFactory
import com.c5000.mastery.database.models.AccountM
import com.c5000.mongopa.MpaModel
import com.c5000.mastery.database.Database

import java.util.UUID
import com.c5000.mastery.shared.Config
import com.mongodb.client.MongoCollection
import com.mongodb.client.model.{Filters, Updates}
import org.bson.Document

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

    def create[T](collection: MongoCollection[Document], field: String, value: T) {
        val query = Filters.exists(field, false)
        val setter = Updates.set(field, value)
        val result = collection.updateMany(query, setter)
        log.info("Rows where a new field has been created: " + result.getModifiedCount)
    }

    def createButSystem[T](collection: MongoCollection[Document], field: String, value: T) {
        val query = Filters.and(
            Filters.ne("id", UUID.fromString(Config.SYS_OBJ_ID)),
            Filters.exists(field, false)
        )
        val setter = Updates.set(field, value)
        val result = collection.updateMany(query, setter)
        log.info("Rows where a new field has been created: " + result.getModifiedCount)
    }

    def renameButSystem(collection: MongoCollection[Document], oldName: String, newName: String) {
        val query = Filters.and(
            Filters.ne("id", UUID.fromString(Config.SYS_OBJ_ID)),
            Filters.exists(oldName, true)
        )
        val setter = Updates.rename(oldName, newName)
        val result = collection.updateMany(query, setter)
        log.info("Rows where a field has been renamed: " + result.getModifiedCount)
    }

    def remove(collection: MongoCollection[Document], field: String) {
        val query = Filters.exists(field, true)
        val setter = Updates.unset(field)
        val result = collection.updateMany(query, setter)
        log.info("Rows where a field has been removed: " + result.getModifiedCount)
    }

}
