package com.c5000.mastery.database

import models.AccountM
import scala.collection.JavaConversions._
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.{DBObject, DBCollection}
import java.util.UUID
import com.c5000.mongopa.MpaModel
import com.c5000.mastery.shared.Config

object SpecialUsers {

    private val accountCollectionName = classOf[AccountM].getAnnotation(classOf[MpaModel]).collection()
    private def accountCollection = Database.mongodb.getCollection(accountCollectionName)

    def setup() {
    }

    /**
     * @return true if the user has admin acces rights
     */
    def isAdmin(accountId: UUID): Boolean = {
        if(accountId == null)
            return false
        val account = accountCollection.findOne(MongoDBObject("id" -> accountId))
        if(account == null)
            return false
        return account.get("admin").asInstanceOf[Boolean]
    }

}
