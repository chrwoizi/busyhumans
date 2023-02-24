package com.c5000.mastery.database.versions

import com.c5000.mastery.database.{FileActions, Database}
import org.slf4j.LoggerFactory

/**
 * Create LOD for images
 */
object Version2 {
    private val log = LoggerFactory.getLogger(getClass)

    def update() {
        Database.forEachFile(true) {
          (file, stream) => {
                if(FileActions.isImage(file.contentType) && file.part == null) {
                    log.info("Updating file: id=" + file.id + ".")
                    try {
                        FileActions.saveImage(file.id, stream)
                        Database.deleteFile(file.id, null)
                    }
                    catch {
                        case e: Throwable => {
                            log.error("ERROR updating file " + file.id + ".", e)
                            throw e
                        }
                    }
                }
            }
        }
    }
}
