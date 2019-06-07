package com.c5000.mastery.database.versions

import org.slf4j.LoggerFactory
import com.c5000.mastery.database.{FileActions, Database}
import collection.mutable
import java.util.UUID
import com.c5000.mastery.shared.FileParts

/**
 * recreate images with lower resolutions
 */
object Version16 {
    private val log = LoggerFactory.getLogger(getClass)

    def update() {
        val ids = mutable.Buffer[UUID]()
        Database.forEachFile() {
            file => {
                if (!ids.contains(file.id))
                    ids += file.id
            }
        }
        ids.foreach(id => {
            val fileOpt = Database.loadFile(id, FileParts.HIRES)
            if (fileOpt.isDefined) {
                val file = fileOpt.get
                if (file.contentType == "image/jpeg") {
                    log.info("Recreating lower image resolutions for file " + file.id)
                    FileActions.recreateSmallImages(file)
                }
            }
        })
    }
}
