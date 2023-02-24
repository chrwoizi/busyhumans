package com.c5000.mastery.database

import models.{ResourceM, PersonM, AccountM, SystemM}
import search.{SearchBeanManager, Search}
import java.util
import org.slf4j.LoggerFactory
import com.c5000.mastery.shared.Config
import com.c5000.mastery.shared.data.base.{ResourceD, TokenizedResourceD, LicenseTypes}
import com.c5000.mongopa.MpaList

object DatabaseBusinessInitializer {
    private val log = LoggerFactory.getLogger(getClass)

    final val SYS_OBJ_ID = util.UUID.fromString(Config.SYS_OBJ_ID)

    private var isInitialized = false

    def init() {
        synchronized {
            if (isInitialized) return
            try {
                DatabaseEngineInitializer.init()
                tryUpdate()
                Search.instance.beanManager = new SearchBeanManager
                Search.instance.rebuildIndex()
                isInitialized = true
            }
            catch {
                case t: Throwable => {
                    log.error("Error while initializing the database.", t)
                    throw t
                }
            }
        }
    }

    private def tryUpdate() {
        var sys = Database.load(classOf[SystemM], SYS_OBJ_ID)
        if (sys == null) {
            sys = new SystemM
            sys.id = SYS_OBJ_ID
            sys.version = 0
        }

        if (!Database.exists(classOf[AccountM], SYS_OBJ_ID)) {
            val account = new AccountM
            account.id = SYS_OBJ_ID
            account.person = SYS_OBJ_ID
            Database.save(account)
        }

        if (!Database.exists(classOf[PersonM], SYS_OBJ_ID)) {
            val person = new PersonM
            person.id = SYS_OBJ_ID
            person.name = "System"
            person.picture = getDefaultPicture
            Database.save(person)
        }

        VersionManager.update(sys)
        Database.save(sys)
    }


    def getDefaultPicture: ResourceM = {
        val picture = new ResourceM
        picture.resource = "static/default-skill.png"
        picture.authorName = "busyhumans.com"
        picture.authorUrl = "http://busyhumans.com"
        picture.license = LicenseTypes.INTERNAL_PICTURE
        picture
    }
}
