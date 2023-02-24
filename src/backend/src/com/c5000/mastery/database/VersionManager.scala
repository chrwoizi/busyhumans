package com.c5000.mastery.database

import models.SystemM
import org.slf4j.LoggerFactory
import versions._
import com.c5000.mastery.shared.Config

object VersionManager {
    private val log = LoggerFactory.getLogger(getClass)

    def update(sys: SystemM) {
        if (sys.version < Config.VERSION) {
            log.info("Updating data from version " + sys.version + " to " + Config.VERSION)

            ///////////// update script invocations go BELOW ///////////////
            if (sys.version < 1) Version1.update()
            if (sys.version < 2) Version2.update()
            if (sys.version < 3) Version3.update()
            if (sys.version < 4) Version4.update()
            if (sys.version < 5) Version5.update()
            if (sys.version < 6) Version6.update()
            if (sys.version < 7) Version7.update()
            if (sys.version < 8) Version8.update()
            if (sys.version < 9) Version9.update()
            if (sys.version < 10) Version10.update()
            if (sys.version < 11) Version11.update()
            if (sys.version < 12) Version12.update()
            if (sys.version < 13) Version13.update()
            if (sys.version < 14) Version14.update()
            if (sys.version < 15) Version15.update()
            if (sys.version < 16) Version16.update()
            if (sys.version < 17) Version17.update()
            ///////////// update script invocations go ABOVE ///////////////

            sys.version = Config.VERSION
        }
    }
}
