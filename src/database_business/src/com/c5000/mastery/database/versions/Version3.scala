package com.c5000.mastery.database.versions

import com.c5000.mastery.database.Database
import org.slf4j.LoggerFactory
import com.c5000.mastery.database.models.{ResourceM, PersonM}

/**
 * Convert person pictures from 3 ResourceM to 1 ResourceM
 */
object Version3 {
    private val log = LoggerFactory.getLogger(getClass)

    def update() {
        /*Database.forEach(classOf[PersonM], includeDeleted = true) {
            person => {
                log.info("Merging avatar images of person " + person.id + " (" + person.name + ").")
                person.picture = new ResourceM
                person.picture.resource = person.pictureLarge.resource
                person.picture.small = person.pictureSmall.resource
                person.picture.medium = person.pictureMedium.resource
                person.picture.authorName = person.pictureLarge.authorName
                person.picture.authorUrl = person.pictureLarge.authorUrl
                person.picture.license = person.pictureLarge.license
                person.pictureSmall = null
                person.pictureMedium = null
                person.pictureLarge = null
                Database.save(person)
            }
        }*/
    }
}
