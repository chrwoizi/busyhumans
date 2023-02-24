package com.c5000.mastery.backend.services

import _root_.java.util.{Date, UUID}
import com.c5000.mastery.shared.data.auth.CloakD
import com.c5000.mastery.shared.{Config, FileParts}
import com.c5000.mastery.database.models._
import com.c5000.mastery.shared.data.base._
import com.c5000.mastery.shared.data.combined.AchievementVD
import com.c5000.mastery.shared.data.combined.AssignmentVD
import com.google.gdata.data.youtube.{VideoEntry, YtPublicationState}

import scala.collection.JavaConverters._

object Presenter {
    def present(preferences: PreferencesM): PreferencesD = {
        val result = new PreferencesD
        result.subscribeOwnAssignments = preferences.subscribeOwnAssignments
        result.notifyOtherActivity = preferences.notifyOtherActivity
        result.notifyActivityReward = preferences.notifyActivityReward
        result.notifyOtherActivityReward = preferences.notifyOtherActivityReward
        return result
    }

    def unpresent(preferences: PreferencesD): PreferencesM = {
        val result = new PreferencesM
        result.subscribeOwnAssignments = preferences.subscribeOwnAssignments
        result.notifyOtherActivity = preferences.notifyOtherActivity
        result.notifyActivityReward = preferences.notifyActivityReward
        result.notifyOtherActivityReward = preferences.notifyOtherActivityReward
        return result
    }

    def present(announcement: AnnouncementM): AnnouncementD = {
        val result = new AnnouncementD
        result.id = announcement.id.toString
        result.showTime = announcement.showTime.toDate
        result.hideTime = if(announcement.hideTime == null) null else announcement.hideTime.toDate
        result.text = announcement.text
        result.maintenance = announcement.maintenance
        return result
    }

    def presentAdminInfo(person: PersonM, account: AccountM): PersonAdminInfoD = {
        val result = new PersonAdminInfoD
        result.person = Presenter.present(person)
        if(account.anonCredential != null)
            result.loginMethod = "password"
        if(account.facebookCredential != null)
            result.loginMethod = "facebook"
        if(account.twitterCredential != null)
            result.loginMethod = "twitter"
        result.hasGoogleAuth = account.googleCredential != null && account.googleCredential.accessToken != null
        result.hasEmail = account.email != null
        result.lastLogin = account.lastLogin.toDate
        return result
    }

    def present(person: PersonM): PersonD = {
        val result = new PersonD
        result.id = person.id.toString
        result.name = person.name
        result.picture = ResourcePresenter.present(person.picture)
        result.xp = person.xp
        result.level = Balancing.getLevel(person.xp)
        result.levelProgress = Balancing.getLevelProgress(person.xp)
        result.levelXp = Balancing.getMinXpForLevel(result.level)
        result.nextLevelXp = Balancing.getMinXpForLevel(result.level + 1)
        result.newAssignmentReward = Balancing.getInitialAssignmentReward(result.level)
        result.joinDate = person.registrationTimestamp.toDate
        result.createdAssignments = null
        result.completedAssignments = null
        result.founder = person.founder == true
        return result
    }

    def present(person: PersonM, createdAssignments: Int, completedAssignments: Int): PersonD = {
        val result = new PersonD
        result.id = person.id.toString
        result.name = person.name
        result.picture = ResourcePresenter.present(person.picture)
        result.xp = person.xp
        result.level = Balancing.getLevel(person.xp)
        result.levelProgress = Balancing.getLevelProgress(person.xp)
        result.levelXp = Balancing.getMinXpForLevel(result.level)
        result.nextLevelXp = Balancing.getMinXpForLevel(result.level + 1)
        result.joinDate = person.registrationTimestamp.toDate
        result.createdAssignments = Integer.valueOf(createdAssignments)
        result.completedAssignments = Integer.valueOf(completedAssignments)
        result.founder = person.founder == true
        return result
    }

    def present(achievement: AchievementM, skill: SkillM, persons: Iterable[PersonM], userPersonId: UUID): AchievementVD = {
        val result = new AchievementVD
        result.id = achievement.id.toString
        result.owner = achievement.owner.toString
        result.skill = present(skill, userPersonId)
        result.persons = new java.util.HashMap[String, PersonD]
        persons.foreach(person => result.persons.put(person.id.toString, present(person)))
        result.activities = new java.util.ArrayList[AchievementActivityD]
        return result
    }

    def unpresent(description: SkillDescriptionD): ResourceM = {
        val result = new ResourceM
        result.resource = description.description
        if (description.isWikipedia) {
            result.authorName = "Wikipedia"
            result.authorUrl = "http://en.wikipedia.org"
            result.license = LicenseTypes.WIKIPEDIA
        }
        else {
            result.authorName = "busyhumans.com"
            result.authorUrl = "http://busyhumans.com"
            result.license = LicenseTypes.INTERNAL_TEXT
        }
        return result
    }

    def present(skill: SkillM, userPersonId: UUID): SkillD = {
        val result = new SkillD
        result.id = skill.id.toString
        result.picture = ResourcePresenter.present(skill.picture)
        result.title = skill.title
        result.description = ResourcePresenter.present(skill.description)
        result.hasReportedAbuse = if(userPersonId != null) skill.abuseReports.validItems.find(_.author == userPersonId).isDefined else false
        result.abuseReports = skill.abuseReportsCount
        return result
    }

    def present(block: ContentBlockM): ContentBlockD = {
        val result = new ContentBlockD
        result.typ = block.typ
        result.value = ResourcePresenter.present(block.value, null, block.typ == ContentBlockD.TYPE_IMAGE)
        result.ready = true
        result.authentic = if(block.authentic != null && block.authentic) true else false
        return result
    }

    def present(activity: ActivityM, canDelete: Boolean, userPersonId: UUID): ActivityD = {
        val result = new ActivityD()
        result.id = activity.id.toString
        result.authorId = activity.author.toString
        result.assignmentId = activity.assignment.toString
        result.achievementId = activity.achievement.toString
        result.contentBlocks = new java.util.ArrayList[ContentBlockD]
        if (activity.contentBlocks != null) {
            activity.contentBlocks.validItems.foreach(block => result.contentBlocks.add(present(block)))
        }
        result.likes = activity.ratings.validItems.count(_.positive)
        result.dislikes = activity.ratings.validItems.size - result.likes
        if(userPersonId != null) {
            val myRating = activity.ratings.validItems.find(_.author == userPersonId)
            result.myRating =
                if (myRating.isDefined)
                    if (myRating.get.positive) 1 else -1
                else 0
        } else {
            result.myRating = 0
        }
        result.rewardDueDate = activity.rewardDueDate.toDate
        result.rewarded = activity.rewarded
        result.canDelete = userPersonId != null && canDelete && userPersonId == activity.author && activity.rewarded == null
        result.hasReportedAbuse = if(userPersonId != null) activity.abuseReports.validItems.find(_.author == userPersonId).isDefined else false
        result.abuseReports = activity.abuseReportsCount
        return result
    }

    def present(assignment: AssignmentM,
                skill: SkillM,
                persons: Iterable[PersonM],
                activities: Iterable[ActivityD],
                userPerson: PersonM,
                userIsAdmin: Boolean,
                subscribed: Boolean): AssignmentVD = {
        val result = new AssignmentVD
        result.assignment = present(assignment, activities, skill, if(userPerson != null) userPerson.id else null, userIsAdmin, subscribed)
        result.skill = present(skill, if(userPerson != null) userPerson.id else null)
        result.persons = new java.util.HashMap[String, PersonD]
        persons.foreach(it => result.persons.put(it.id.toString, present(it)))
        result.activities = new java.util.ArrayList[ActivityD](activities.toList.asJava)
        result.userPerson = if(userPerson != null) present(userPerson) else null
        return result
    }

    def getPicture(assignment: AssignmentM, activities: Iterable[ActivityD], skill: SkillM): ResourceD = {
        var result = ResourcePresenter.present(skill.picture)
        val pictures = activities.map(activity => {
            val pictures = activity.contentBlocks.asScala.map(block => {
                block.typ match {
                    case ContentBlockD.TYPE_IMAGE => {
                        block.value.resource
                    }
                    case ContentBlockD.TYPE_VIDEO => {
                        val resource = block.value.resource
                        val thumb = new ResourceD
                        thumb.resource = "http://i2.ytimg.com/vi/" + resource.resource + "/hqdefault.jpg"
                        thumb.authorName = resource.authorName
                        thumb.authorUrl = resource.authorUrl
                        thumb.license = resource.license
                        thumb
                    }
                    case _ => null
                }
            }).filter(_ != null)
            if (!pictures.isEmpty) {
                var rating = 0f
                if (activity.likes + activity.dislikes > 0) {
                    rating = activity.likes / (activity.likes + activity.dislikes).asInstanceOf[Float]
                }
                if (activity.abuseReports > 0)
                    rating *= 0.01f / activity.abuseReports
                (pictures.head, rating)
            }
            else null
        }).filter(_ != null)
        if (!pictures.isEmpty) {
            result = pictures.maxBy(_._2)._1
        }
        return result
    }

    def present(assignment: AssignmentM, activities: Iterable[ActivityD], skill: SkillM, userPersonId: UUID, userIsAdmin: Boolean, subscribed: Boolean): AssignmentD = {
        val result = new AssignmentD
        result.id = assignment.id.toString
        result.authorId = assignment.author.toString
        result.skillId = assignment.skill.toString
        result.title = assignment.title
        result.picture = getPicture(assignment, activities, skill)
        result.description = assignment.description
        result.reward = math.max(0, assignment.rewards.validItems.map(_.amount.intValue()).sum)
        result.creationTimestamp = assignment.creationTimestamp.toDate
        result.canComplete = userPersonId != null
        result.hasCompleted =
            userPersonId != null &&
                assignment.activities.validItems.exists(_.person == userPersonId)
        result.boosted = null
        if (userPersonId != null) {
            val boost = assignment.rewards.validItems.find(_.donatingPerson == userPersonId)
            if (boost.isDefined) {
                result.boosted = boost.get.amount
            }
        }
        result.hasReportedAbuse = assignment.abuseReports.validItems.find(_.author == userPersonId).isDefined
        result.abuseReports = assignment.abuseReportsCount
        result.subscribed = subscribed
        return result
    }

    /**
     * use this method for videos that have been pasted by url
     */
    def presentVideoById(videoId: String): VideoD = {
        // assume that the video is available
        val result = new VideoD
        result.authenticated = false
        result.id = videoId
        result.title = null
        result.date = null
        result.embeddable = true
        result.ready = true
        return result
    }

    /**
     * use this method for videos from a connected youtube account
     */
    def present(video: VideoEntry): VideoD = {
        val result = new VideoD
        result.authenticated = true
        result.id = getVideoId(video)
        result.title = video.getTitle.getPlainText
        result.date = new Date(video.getPublished.getValue)
        result.embeddable = video.isEmbeddable
        result.ready = !video.isDraft
        if (!result.ready) {
            val state = video.getPublicationState
            if (state.getState == YtPublicationState.State.REJECTED) {
                result.error = "The video has been rejected by YouTube. Reason: " + state.getDescription
            }
            else if (state.getState == YtPublicationState.State.FAILED) {
                result.error = "Error while uploading the video: " + state.getDescription
            }
        }
        return result
    }

    def getVideoId(video: VideoEntry): String = {
        return video.getId.substring(video.getId.lastIndexOf(':') + 1)
    }

    def presentCloak(person: PersonM): CloakD = {
        val result = new CloakD
        result.personId = person.id.toString
        result.name = person.name
        return result
    }
}
