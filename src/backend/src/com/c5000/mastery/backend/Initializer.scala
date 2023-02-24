package com.c5000.mastery.backend

import com.c5000.mastery.database.DatabaseBusinessInitializer
import services.NotificationS

object Initializer {

    def init() {
        DatabaseBusinessInitializer.init()
        NotificationS.initScheduler()
    }

}
