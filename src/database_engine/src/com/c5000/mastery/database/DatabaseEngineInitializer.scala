package com.c5000.mastery.database


import org.slf4j.LoggerFactory
import search.Search

object DatabaseEngineInitializer {
    private val log = LoggerFactory.getLogger(getClass)

    private var isInitialized = false

    def init() {
        synchronized {
            if (isInitialized) return
            try {
                Database.init()
                SpecialUsers.setup()
                Search.init()
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
}
