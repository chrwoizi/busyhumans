package com.c5000.mastery.backend.services

import _root_.java.util.UUID
import com.c5000.mastery.backend.facebook.Facebook
import com.c5000.mastery.backend.twitter.TwitterS
import com.c5000.mastery.backend.{Tokenizer, TimeHelper}
import com.c5000.mastery.database.models._
import com.c5000.mastery.database.Database
import com.c5000.mastery.database.queries.{AchievementQ, ActivityQ}
import com.c5000.mastery.database.updates._
import com.c5000.mastery.shared.{Sanitizer, ImageHelper, Config}
import com.c5000.mastery.shared.data.base.{LicenseTypes, ContentBlockD, ActivityD}
import com.c5000.mastery.shared.data.combined.{ActivityVD, ActivityDeletedVD}
import com.c5000.mongopa.MpaList
import collection.mutable

object ActivityS extends HasServiceLogger {

    def getActivity(activityId: UUID, userPersonId: UUID, userIsAdmin: Boolean): ActivityVD = {
        if (activityId == null)
            return null

        val activity = AbuseFilter.filter(Database.load(classOf[ActivityM], activityId), userIsAdmin)
        if (activity == null) {
            logger.warn("Activity " + activityId + " not found.")
            return null
        }

        val author = Database.load(classOf[PersonM], activity.author)
        if (author == null) {
            logger.warn("Activity author " + activity.author + " not found.")
            return null
        }

        val canDelete = if (userIsAdmin) Some(true) else getCanDelete(activity, userIsAdmin)
        if (canDelete.isEmpty)
            return null

        val result = new ActivityVD
        result.activity = Presenter.present(activity, canDelete = canDelete.get, userPersonId = userPersonId)
        result.author = Presenter.present(author)
        return result
    }

    def cleanup(block: ContentBlockD): ContentBlockM = {
        if (block.value.resource.resource.trim().isEmpty)
            return null

        if (block.typ == ContentBlockD.TYPE_TEXT) {
            block.value.resource.resource = Sanitizer.activityText(block.value.resource.resource)
            if (block.value.resource.resource == null) {
                logger.warn("Invalid text content block.")
                return null
            }
        }

        block.typ match {
            case ContentBlockD.TYPE_TEXT
                 | ContentBlockD.TYPE_IMAGE
                 | ContentBlockD.TYPE_VIDEO => {

                val model = new ContentBlockM
                model.typ = block.typ
                model.value = ResourcePresenter.unpresent(block.value.resource)
                model.authentic = block.authentic
                return model
            }
            case _ => {}
        }
        return null
    }

    def createActivity(assignmentId: UUID, contentBlocks: Iterable[ContentBlockD], accountId: UUID, personId: UUID, isCloaked: Boolean, userIsAdmin: Boolean, uploadTokenizer: Tokenizer): ActivityVD = {
        if (assignmentId == null || contentBlocks == null || contentBlocks.isEmpty || accountId == null || personId == null)
            return null

        if (assignmentId.toString == Config.FOUNDER_ASSIGNMENT_ID) {
            logger.warn("cannot complete founder assignment explicitly.")
            // cannot complete founder assignment explicitly. must create own assignment to complete founder.
            return null
        }

        val person = PersonS.loadPerson(personId)
        if (person == null) {
            logger.warn("Person " + personId + " not found.")
            return null
        }

        val emptyTexts = mutable.Buffer[ContentBlockD]()

        contentBlocks.foreach(cb => {
            cb.typ match {
                case ContentBlockD.TYPE_TEXT => {
                    cb.authentic = true
                    cb.value.resource.authorName = person.name
                    cb.value.resource.authorUrl = "#person=" + person.id
                    cb.value.resource.license = LicenseTypes.INTERNAL_TEXT
                    val text = Sanitizer.activityText(cb.value.resource.resource)
                    if (text == null) {
                        logger.warn("Activity text is invalid.")
                        return null
                    }
                    if (text.isEmpty)
                        emptyTexts += cb
                }
                case ContentBlockD.TYPE_IMAGE => {
                    cb.authentic = uploadTokenizer.detokenize(cb.value)
                    if (!cb.authentic)
                        return null
                }
                case ContentBlockD.TYPE_VIDEO => {
                    cb.value.resource.license = LicenseTypes.YOUTUBE
                    if (YoutubeS.getUserVideos(accountId).exists(_.id == cb.value.resource.resource)) {
                        cb.value.resource.authorName = person.name
                        cb.value.resource.authorUrl = "#person=" + person.id
                        cb.authentic = true
                    }
                    else {
                        logger.info("Video '" + cb.value.resource.resource + "' for new activity is not authenticated because it was added via url")
                        cb.value.resource.authorName = null
                        cb.value.resource.authorUrl = null
                        cb.authentic = false
                    }
                }
            }
        })

        if (contentBlocks.size == emptyTexts.size) {
            logger.warn("No content for new activity in assignment " + assignmentId + ".")
            return null
        }

        val assignment = AbuseFilter.filter(Database.load(classOf[AssignmentM], assignmentId), userIsAdmin)
        if (assignment == null) {
            logger.warn("Assignment " + assignmentId + " not found.")
            return null
        }

        val existingActivity = assignment.activities.validItems.find(_.person == personId)
        if (existingActivity.isDefined) {
            logger.warn("Person " + personId + " has already completed assignment " + assignment.id + ".")
            return null
        }

        val achievementOpt = AchievementQ.getByPersonAndSkill(person.id, assignment.skill, 100).map(item => Database.load(classOf[AchievementM], item)).filter(_ != null).headOption
        var achievement: AchievementM = null
        if (achievementOpt.isDefined) {
            achievement = achievementOpt.get
        }
        else {
            logger.trace("Person " + personId + " does not have an achievement for skill " + assignment.skill + " yet.")

            val skill = Database.load(classOf[SkillM], assignment.skill)
            if (skill == null) {
                logger.warn("Skill " + assignment.skill + " of assignment " + assignmentId + " not found.")
                return null
            }

            logger.info("Creating achievement for skill " + skill.id + " for Person " + personId + ".")
            achievement = new AchievementM()
            achievement.owner = personId
            achievement.skill = skill.id
            Database.save(achievement)
        }

        logger.info("Creating result activity for assignment " + assignmentId + ".")
        val activity = new ActivityM()
        activity.timestamp = TimeHelper.now
        activity.rewardDueDate = TimeHelper.now.plusDays(3)
        activity.assignment = assignment.id
        activity.achievement = achievement.id
        activity.author = personId

        activity.contentBlocks = new MpaList[ContentBlockM]()
        contentBlocks.foreach(block => {
            logger.trace("got block " + block.typ + "=" + block.value)
            val model = cleanup(block)
            if (model != null)
                activity.contentBlocks.add(model)
        })

        Database.save(activity)

        logger.info("Adding activity " + activity.id + " to achievement " + achievement.id + ".")
        AchievementU.addActivity(achievement, activity.id)

        val ref = new ActivityRefM
        ref.person = person.id
        ref.activity = activity.id
        AssignmentU.addActivity(assignment, ref, activity)

        val result = new ActivityVD
        result.activity = Presenter.present(activity, userIsAdmin || activity.rewardDueDate.isAfter(TimeHelper.now), personId)
        result.author = Presenter.present(person)

        val account = Database.load(classOf[AccountM], accountId)

        NotificationS.createOtherActivityNotifications(person, assignment)

        if (!isCloaked && Config.IS_LIVE) {
            // try to post on facebook
            try {
                if (account != null && account.facebookCredential != null) {
                    val assignmentD = AssignmentS.getAssignment(assignmentId, accountId, personId, userIsAdmin)
                    val picture = ImageHelper.getAbsoluteUrl(assignmentD.assignment.picture, ImageHelper.Size.MEDIUM)
                    val url = Config.META_URL + "?token=" + "assignment%3D" + assignmentId
                    val title = assignment.title
                    val description = assignment.description
                    Facebook.publishAction(logger, account.facebookCredential.accessToken, "complete", "assignment", url, picture, title, description)
                }
            }
            catch {
                case _: Throwable => {}
            }

            // try to post on twitter
            try {
                if (account.twitterCredential != null && account.twitterCredential.accessToken != null) {
                    val url = Config.META_URL + "?token=" + "assignment%3D" + assignmentId
                    val text = "I completed \"" + trimEnd(assignment.title, 140 - 18 - url.length) + "\" on " + url
                    TwitterS.publish(logger, account.twitterCredential, text)
                }
            }
            catch {
                case _: Throwable => {}
            }
        }

        return result
    }

    private def trimEnd(s: String, length: Int): String = {
        if (s.length < length)
            return s
        return s.substring(0, length)
    }

    def deleteActivity(activityId: UUID, userPersonId: UUID, userIsAdmin: Boolean): ActivityDeletedVD = {
        if (activityId == null || userPersonId == null)
            return null

        val activity = Database.load(classOf[ActivityM], activityId)
        if (activity == null) {
            logger.warn("Activity " + activityId + " not found.")
            return null
        }

        if (!userIsAdmin) {
            if (activity.author != userPersonId) {
                logger.warn("Cannot delete activity " + activityId + " because the user is not the author.")
                return null
            }

            if (activity.rewarded != null) {
                logger.warn("Cannot delete activity " + activityId + " because it has been rewarded.")
                return null
            }
        }

        val achievement = Database.load(classOf[AchievementM], activity.achievement)
        if (achievement == null) {
            logger.warn("Achievement " + activity.achievement + " not found.")
            return null
        }

        val assignment = Database.load(classOf[AssignmentM], activity.assignment)
        if (assignment != null) {
            val ref = assignment.activities.validItems.find(_.person == activity.author)
            if (ref.isDefined) {
                logger.info("Removing activity " + activity.id + " from assignment " + assignment.id)
                AssignmentU.removeActivity(assignment, ref.get)
            }
        }

        logger.info("Removing activity " + activity.id + " from achievement " + achievement.id)
        AchievementU.removeActivity(achievement, activity.id)
        val achievementDeleted = AchievementU.deleteEmpty(achievement, logger)

        logger.info("Deleting activity " + activity.id + ".")
        BaseU.setDeleted(activity)

        val result = new ActivityDeletedVD
        result.achievementDeleted = achievementDeleted
        return result
    }

    private def getCanDelete(activity: ActivityM, userIsAdmin: Boolean): Option[Boolean] = {
        var canDelete = true
        return Some(canDelete)
    }

    def rate(activityId: UUID, ratingValue: Int, userPersonId: UUID, userIsAdmin: Boolean): ActivityD = {
        if (activityId == null || userPersonId == null)
            return null

        val activity = AbuseFilter.filter(Database.load(classOf[ActivityM], activityId), userIsAdmin)
        if (activity == null) {
            logger.warn("Activity " + activityId + " not found.")
            return null
        }

        val canDelete = if (userIsAdmin) Some(true) else getCanDelete(activity, userIsAdmin)
        if (canDelete.isEmpty)
            return null

        // remove the old rating
        val ratingOpt = activity.ratings.validItems.find(_.author == userPersonId)
        var rating = if (ratingOpt.isDefined) ratingOpt.get else null
        if (rating != null) {
            logger.info("Deleting rating for activity " + activity.id + ".")
            ActivityU.removeRating(activity, rating)
        }

        // add new rating if required
        if (ratingValue != 0) {
            logger.info("Creating rating for activity " + activity.id + " with positive=" + (ratingValue > 0) + ".")
            rating = new RatingM()
            rating.author = userPersonId
            rating.timestamp = TimeHelper.now
            rating.positive = ratingValue > 0
            ActivityU.addRating(activity, rating)
        }

        return Presenter.present(activity, canDelete = canDelete.get, userPersonId = userPersonId)
    }

    def setAbuseReport(activityId: UUID, isAbuse: Boolean, userPersonId: UUID, userIsAdmin: Boolean): ActivityD = {
        if (activityId == null || userPersonId == null)
            return null

        val activity = Database.load(classOf[ActivityM], activityId)
        if (activity == null) {
            logger.warn("Activity " + activityId + " not found.")
            return null
        }

        val canDelete = if (userIsAdmin) Some(true) else getCanDelete(activity, userIsAdmin)
        if (canDelete.isEmpty)
            return null

        // get existing report
        val reportOpt = activity.abuseReports.validItems.find(report => report.author == userPersonId)

        if (isAbuse) {
            if (reportOpt.isEmpty) {
                logger.info("Creating abuse report for activity " + activity.id + ".")
                val report = new AbuseReportM()
                report.author = userPersonId
                report.timestamp = TimeHelper.now
                ActivityU.addAbuseReport(activity, report)
            }
        }
        else {
            if (reportOpt.isDefined) {
                logger.info("Deleting abuse report for activity " + activity.id + ".")
                ActivityU.removeAbuseReport(activity, reportOpt.get)
            }
        }

        return Presenter.present(activity, canDelete = canDelete.get, userPersonId = userPersonId)
    }

    def clearAbuseReports(activityId: UUID, userPersonId: UUID, userIsAdmin: Boolean): ActivityD = {
        if (activityId == null || userPersonId == null)
            return null

        val activity = Database.load(classOf[ActivityM], activityId)
        if (activity == null) {
            logger.warn("Activity " + activityId + " not found.")
            return null
        }

        val canDelete = if (userIsAdmin) Some(true) else getCanDelete(activity, userIsAdmin)
        if (canDelete.isEmpty)
            return null

        logger.info("Deleting all abuse reports for activity " + activity.id + ".")
        ActivityU.removeAbuseReports(activity)

        return Presenter.present(activity, canDelete = canDelete.get, userPersonId = userPersonId)
    }

    def getWithMostAbuseReports(userPersonId: UUID, userIsAdmin: Boolean): Iterable[ActivityVD] = {
        val activityIds = ActivityQ.getWithMostAbuseReports(100)
        logger.trace("Found " + activityIds.size + " activities with abuse reports.")
        return activityIds.map(it => getActivity(it, userPersonId, userIsAdmin)).filter(it => it != null)
    }

}
