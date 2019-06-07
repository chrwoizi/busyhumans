package com.c5000.mastery.backend.services

import _root_.java.util.UUID
import com.c5000.mastery.backend.{Tokenizer, TimeHelper}
import com.c5000.mastery.database.models._
import com.c5000.mastery.database.Database
import com.c5000.mastery.database.queries.{AchievementQ, AssignmentQ}
import com.c5000.mastery.database.search.{SearchBeanTypes, Search}
import com.c5000.mastery.database.updates.{BaseU, PersonU}
import com.c5000.mastery.shared.Config
import com.c5000.mastery.shared.data.base._
import com.c5000.mastery.shared.data.combined.AchievementVD
import collection.mutable.Buffer
import scala.collection.JavaConversions._

object AchievementS extends HasServiceLogger {

    def getPersonAchievements(personId: UUID, userAccountId: UUID, userPersonId: UUID, userIsAdmin: Boolean, offset: Int): Iterable[AchievementVD] = {
        if (personId == null)
            return List()

        val achievementIds = AchievementQ.getByPerson(personId, offset, Config.PAGE_SIZE)
        logger.trace("Found " + achievementIds.size + " achievement ids for person " + personId + ".")
        return achievementIds.map(it => getAchievement(it, userAccountId, userPersonId, userIsAdmin)).filter(it => it != null)
    }

    def getAchievement(achievementId: UUID, userAccountId: UUID, userPersonId: UUID, userIsAdmin: Boolean): AchievementVD = {
        if (achievementId == null)
            return null

        logger.trace("Loading achievement " + achievementId + ".")

        val achievement = Database.load(classOf[AchievementM], achievementId)
        if (achievement == null) {
            logger.warn("Achievement " + achievementId + " not found.")
            return null
        }

        val skill = AbuseFilter.filter(Database.load(classOf[SkillM], achievement.skill), userIsAdmin)
        if (skill == null) {
            logger.warn("Skill " + achievement.skill + " of achievement " + achievementId + " not found.")
            return null
        }

        // all activities for the achievement
        logger.trace("Found " + achievement.activities.validItems.size + " activities for achievement " + achievementId + ".")
        val activities = achievement.activities.validItems.
            map(item => AbuseFilter.filter(Database.load(classOf[ActivityM], item.id), userIsAdmin)).
            filter(_ != null).
            sortBy(activity => if(activity.rewarded != null) -activity.rewarded else 0)

        // all persons that are referenced in the sent data
        val personIds = Buffer(achievement.owner)
        personIds.appendAll(activities.map(_.author))
        personIds.appendAll(activities.map(_.ratings.validItems).flatten.map(_.author))
        val persons = personIds.distinct.map(it => PersonS.loadPerson(it)).filter(it => it != null)
        logger.trace("" + persons.size + " persons referenced in achievement " + achievementId + ".")

        // present
        val result = Presenter.present(achievement, skill, persons, userPersonId)
        val presentedActivities = activities.map(activity => {
            val assignment = AbuseFilter.filter(Database.load(classOf[AssignmentM], activity.assignment), userIsAdmin)
            if (assignment == null) {
                logger.warn("Assignment " + activity.assignment + " for activity " + activity.id + " not found.")
                return null
            }
            val aa = new AchievementActivityD
            aa.activity = Presenter.present(activity, userIsAdmin || activity.rewardDueDate.isAfter(TimeHelper.now), userPersonId)
            aa.assignment = AssignmentS.getAssignment(assignment.id, userAccountId, userPersonId, userIsAdmin).assignment
            aa
        }).filter(it => it != null).toList
        result.activities = new java.util.ArrayList[AchievementActivityD](presentedActivities)
        logger.trace("Returning " + result.activities.size() + " activities for achievement " + achievement.id + ".")
        return result
    }

    def deleteAchievement(achievementId: UUID, userPersonId: UUID, userIsAdmin: Boolean): Boolean = {
        if (achievementId == null || userPersonId == null)
            return false

        val achievement = Database.load(classOf[AchievementM], achievementId)
        if (achievement == null) {
            logger.warn("Achievement " + achievementId + " not found.")
            return false
        }

        if (!userIsAdmin && achievement.owner != userPersonId) {
            logger.warn("Cannot delete achievement " + achievementId + " because the user is not the owner.")
            return false
        }

        val person = PersonS.loadPerson(achievement.owner)
        if (person == null) {
            logger.warn("Achievement owner " + achievement.owner + " not found.")
            return false
        }

        logger.info("Deleting achievement " + achievement.id + " and all its activities.")
        achievement.activities.validItems.foreach(item => ActivityS.deleteActivity(item.id, userPersonId, userIsAdmin))
        BaseU.setDeleted(achievement)

        return true
    }

}
