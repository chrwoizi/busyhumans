package com.c5000.mastery.database.versions

import org.slf4j.LoggerFactory
import com.c5000.mastery.database.{Database, FileActions}

import collection.mutable
import java.util.UUID
import com.c5000.mastery.shared.FileParts

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

/**
 * recreate images with lower resolutions
 */
object Version16 {
    private val log = LoggerFactory.getLogger(getClass)

    def update() {
        val ids = mutable.Buffer[UUID]()
        Database.forEachFile(false) {
          (file, stream) => {
                if (!ids.contains(file.id))
                    ids += file.id
            }
        }
        ids.foreach(id => {
            val os = new ByteArrayOutputStream()
            val file = Database.loadFile(id, FileParts.HIRES, os)
            if (file != null) {
                if (file.contentType == "image/jpeg") {
                    log.info("Recreating lower image resolutions for file " + file.id)
                    val bytes = os.toByteArray
                    val is = new ByteArrayInputStream(bytes)
                    FileActions.recreateSmallImages(file, is)
                }
            }
        })
    }
}
