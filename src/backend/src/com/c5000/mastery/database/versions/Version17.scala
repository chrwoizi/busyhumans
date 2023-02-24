package com.c5000.mastery.database.versions

import com.c5000.mastery.database.Database
import com.mongodb.client.model.{Filters, Updates}
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._

/**
 * recreate images with lower resolutions
 */
object Version17 {
    private val log = LoggerFactory.getLogger(getClass)

    def update() {
        val collection = Database.mongodb.getCollection("fs.files")
        val files = collection.find()
        for(file <- files.asScala) {
          val contentType = file.get("contentType")
          collection.updateOne(Filters.eq("_id", file.get("_id")), Updates.combine(
            Updates.set("metadata.type", contentType),
            Updates.unset("contentType"),
            Updates.unset("md5")))
        }
    }
}
