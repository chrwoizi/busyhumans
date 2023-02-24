package com.c5000.mastery.database.versions

import com.c5000.mastery.database.Database
import org.slf4j.LoggerFactory
import com.c5000.mastery.database.models.{ActivityM, SkillM, ResourceM, PersonM}
import com.c5000.mastery.shared.FileParts

/**
 * Create LOD urls for all dynamic images
 */
object Version4 {
    private val log = LoggerFactory.getLogger(getClass)

    def update() {
        Database.forEach(classOf[SkillM], includeDeleted = true) {
            skill => {
                log.info("Creating image LOD for skill " + skill.id + ".")
                if(skill.picture != null) {
                    createLod(skill.picture)
                    Database.save(skill)
                }
            }
        }
        Database.forEach(classOf[ActivityM], includeDeleted = true) {
            activity => {
                log.info("Creating image LOD for activity " + activity.id + ".")
                var hasChanged = false
                activity.contentBlocks.allItems.foreach(cb => {
                    if(cb.value != null) {
                        createLod(cb.value)
                        hasChanged = true
                    }
                })
                if(hasChanged)
                    Database.save(activity)
            }
        }
    }

    def createLod(picture: ResourceM) {
        if (picture.resource.startsWith("mastery/res/dynamic/")) {
            if (picture.small == null)
                picture.small = picture.resource + "/" + FileParts.SMALL
            if (picture.medium == null)
                picture.medium = picture.resource + "/" + FileParts.MEDIUM
            if (picture.large == null)
                picture.large = picture.resource + "/" + FileParts.LARGE
            if (picture.hires == null)
                picture.hires = picture.resource + "/" + FileParts.HIRES
        }
    }
}
