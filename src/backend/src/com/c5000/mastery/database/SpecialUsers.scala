package com.c5000.mastery.database

import models.AccountM

import java.util.UUID
import com.c5000.mongopa.MpaModel
import com.mongodb.client.model.Filters

import scala.concurrent.Await
import scala.concurrent.duration.Duration

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
        val account = accountCollection.find(Filters.eq("id", accountId)).first()
        if(account == null)
            return false
        return account.get("admin").asInstanceOf[Boolean]
    }

}
