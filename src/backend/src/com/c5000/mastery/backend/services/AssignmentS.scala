package com.c5000.mastery.backend.services

import _root_.java.util.UUID
import com.c5000.mastery.backend.facebook.Facebook
import com.c5000.mastery.backend.twitter.TwitterS
import com.c5000.mastery.backend.{Tokenizer, TimeHelper}
import com.c5000.mastery.database.Database
import com.c5000.mastery.database.models._
import com.c5000.mastery.database.queries.{SubscriptionQ, AssignmentQ}
import com.c5000.mastery.database.search.Search
import com.c5000.mastery.database.updates.{PersonU, ActivityU, BaseU, AssignmentU}
import com.c5000.mastery.shared.{Sanitizer, ImageHelper, Config}
import com.c5000.mastery.shared.data.base.{SortBy, AssignmentCreationParamsD}
import com.c5000.mastery.shared.data.combined.AssignmentVD
import collection.mutable.Buffer

object AssignmentS extends HasServiceLogger {

    def boostAssignment(assignmentId: UUID, boostValue: Int, userAccountId: UUID, userPersonId: UUID, userIsAdmin: Boolean): AssignmentVD = {
        if (assignmentId == null || userPersonId == null)
            return null

        val assignment = AbuseFilter.filter(Database.load(classOf[AssignmentM], assignmentId), userIsAdmin)
        if (assignment == null) {
            logger.warn("Assignment " + assignmentId + " not found.")
            return null
        }

        // remove the old boost
        val boostOpt = assignment.rewards.validItems.find(_.donatingPerson == userPersonId)
        var boost = if (boostOpt.isDefined) boostOpt.get else null
        if (boost != null) {
            logger.info("Deleting boost for assignment " + assignment.id + ".")
            AssignmentU.removeBoost(assignment.id, boost)
        }

        // add new boost if required
        if (boostValue != 0) {
            val person = Database.load(classOf[PersonM], userPersonId)
            if (person == null) {
                logger.warn("Person " + userPersonId + " not found.")
                return null
            }

            val rewardAmount = Balancing.getInitialAssignmentReward(Balancing.getLevel(person.xp))
            if (rewardAmount <= 0) {
                logger.warn("Person " + userPersonId + " has invalid reward boost value: " + rewardAmount)
                return null
            }

            logger.info("Creating boost for assignment " + assignment.id + " with positive=" + (boostValue > 0) + ".")
            boost = new RewardM()
            boost.timestamp = TimeHelper.now
            boost.donatingPerson = userPersonId
            boost.amount = if (boostValue > 0) rewardAmount else -rewardAmount
            AssignmentU.addBoost(assignment.id, boost)
        }

        return getAssignment(assignmentId, userAccountId, userPersonId, userIsAdmin)
    }

    def getCompletedAssignments(personId: UUID, userAccountId: UUID, userPersonId: UUID, userIsAdmin: Boolean, offset: Int): Iterable[AssignmentVD] = {
        if (personId == null)
            return List()

        val assignmentIds = AssignmentQ.getCompleted(personId, offset, Config.PAGE_SIZE)
        logger.trace("Found " + assignmentIds.size + " assignment ids where person " + personId + " is participating.")
        return assignmentIds.map(it => getAssignment(it, userAccountId, userPersonId, userIsAdmin)).filter(it => it != null)
    }

    def getActiveAssignments(userAccountId: UUID, userPersonId: UUID, userIsAdmin: Boolean, offset: Int, sortBy: SortBy): Iterable[AssignmentVD] = {
        val assignmentIds = AssignmentQ.getAll(offset, Config.PAGE_SIZE, sortBy)
        logger.trace("Found " + assignmentIds.size + " active assignments.")
        return assignmentIds.map(it => getAssignment(it, userAccountId, userPersonId, userIsAdmin)).filter(it => it != null)
    }

    def getCreatedAssignments(personId: UUID, userAccountId: UUID, userPersonId: UUID, userIsAdmin: Boolean, offset: Int): Iterable[AssignmentVD] = {
        if (personId == null)
            return List()

        val assignmentIds = AssignmentQ.getCreated(personId, offset, Config.PAGE_SIZE)
        logger.trace("Found " + assignmentIds.size + " assignment ids which were created by person " + personId + ".")
        return assignmentIds.map(it => getAssignment(it, userAccountId, userPersonId, userIsAdmin)).filter(it => it != null)
    }

    def getAssignment(assignmentId: UUID, userAccountId: UUID, userPersonId: UUID, userIsAdmin: Boolean): AssignmentVD = {
        if (assignmentId == null)
            return null

        logger.trace("Loading assignment " + assignmentId + ".")

        val assignment = AbuseFilter.filter(Database.load(classOf[AssignmentM], assignmentId), userIsAdmin)
        if (assignment == null) {
            logger.warn("Assignment " + assignmentId + " not found.")
            return null
        }

        val skill = AbuseFilter.filter(Database.load(classOf[SkillM], assignment.skill), userIsAdmin)
        if (skill == null) {
            logger.warn("Skill " + assignment.skill + " of assignment " + assignment.id + " not found.")
            return null
        }

        val userPerson = PersonS.loadPerson(userPersonId)

        distributeRewards(assignment)

        // all result activities
        val activities = assignment.activities.validItems.map(it => AbuseFilter.filter(Database.load(classOf[ActivityM], it.activity), userIsAdmin)).filter(_ != null)
        logger.trace("Found " + activities.size + " result activities for assignment " + assignmentId + ".")

        // all persons that are referenced in the sent data
        val referencedPersonIds = Buffer(assignment.author)
        referencedPersonIds.appendAll(assignment.activities.validItems.map(_.person))
        referencedPersonIds.appendAll(activities.map(_.author))
        referencedPersonIds.appendAll(activities.map(_.ratings.validItems.map(_.author)).flatten)
        val referencedPersons = referencedPersonIds.filter(_ != null).distinct.map(it => PersonS.loadPerson(it)).filter(_ != null)
        logger.trace("" + referencedPersons.size + " persons referenced in assignment " + assignmentId + ".")

        val subscribed = if(userAccountId != null) SubscriptionQ.isSubscribed(userAccountId, assignment.id) else false

        val presentedActivities = activities.map(it => Presenter.present(it, userIsAdmin || it.rewardDueDate.isAfter(TimeHelper.now), userPersonId)).sortBy(-_.likes)
        return Presenter.present(assignment, skill, referencedPersons, presentedActivities, userPerson, userIsAdmin, subscribed)
    }

    private def distributeRewards(assignment: AssignmentM) {

        val assignmentAuthor = Database.load(classOf[PersonM], assignment.author)

        assignment.activities.validItems.foreach(ref => {
            val activity = Database.load(classOf[ActivityM], ref.activity)
            if (activity != null && activity.rewarded == null && activity.rewardDueDate.isBefore(TimeHelper.now)) {
                // give reward to activity author
                val maxReward = math.max(0, assignment.rewards.validItems.map(_.amount.intValue()).sum)
                val likes = activity.ratings.validItems.count(_.positive)
                val dislikes = activity.ratings.validItems.count(it => !it.positive)
                val likeRatio = if (likes + dislikes == 0) 0.5f else (likes / (likes + dislikes).toFloat)
                val activityAuthorReward = math.ceil(likeRatio * maxReward).toInt
                if (ActivityU.reward(activity, ref.person, activityAuthorReward)) {
                    logger.info("Person " + ref.person + " got " + activityAuthorReward + " reward xp for assignment " + assignment.id)

                    if (activityAuthorReward > 0) {
                        // create notification for activity author
                        val activityAuthor = Database.load(classOf[PersonM], activity.author)
                        if (activityAuthor != null && activityAuthor.account != null) {
                            NotificationS.createActivityRewardNotification(activityAuthor.account, activityAuthorReward, assignment)
                        }

                        if (assignment.author != activity.author) {
                            // give reward to assignment author
                            val assignmentAuthorReward = (Balancing.OWNER_REWARD_FACTOR * activityAuthorReward).toInt
                            logger.info("Author " + assignment.author + " gets " + assignmentAuthorReward + " reward xp for assignment " + assignment.id)
                            PersonU.incrementXp(assignment.author, assignmentAuthorReward)

                            if (assignmentAuthorReward > 0) {
                                // create notification for assignment author
                                if (assignmentAuthor != null && assignmentAuthor.account != null && activityAuthor != null) {
                                    NotificationS.createOtherActivityRewardNotification(assignmentAuthor.account, assignmentAuthorReward, activityAuthor, assignment)
                                }
                            }
                        }
                    }
                }
            }
        })
    }

    def createAssignment(userAccountId: UUID, userPersonId: UUID, isCloaked: Boolean, params: AssignmentCreationParamsD, tokenizer: Tokenizer, userIsAdmin: Boolean): AssignmentVD = {
        if (userPersonId == null || userPersonId == null || params == null)
            return null

        val author = PersonS.loadPerson(userPersonId)

        if (params.title == null) {
            logger.warn("Assignment title is invalid.")
        }
        params.title = Sanitizer.assignmentTitle(params.title)
        if (params.title == null) {
            logger.warn("Assignment title is invalid.")
        }

        if (AssignmentQ.existsWithTitle(params.title)) {
            logger.warn("Assignment with title '" + params.title + "' already exists.")
            return null
        }

        if (params.description == null) {
            logger.warn("Assignment description is invalid.")
        }
        params.description = Sanitizer.assignmentDescription(params.description)
        if (params.title == null) {
            logger.warn("Assignment description is invalid.")
        }

        val userPerson = PersonS.loadPerson(userPersonId)
        if (userPerson == null) {
            logger.warn("User person " + userPersonId + " not found.")
            return null
        }

        var skill: SkillM = null
        if (params.skillId != null) {
            val existingSkillId = UUID.fromString(params.skillId)
            if (existingSkillId == null) {
                logger.warn("Invalid skill id '" + params.skillId + "'.")
                return null
            }

            skill = AbuseFilter.filter(Database.load(classOf[SkillM], existingSkillId), userIsAdmin)
            if (skill == null) {
                logger.warn("Cannot find skill " + existingSkillId + " for new assignment.")
                return null
            }
        }
        else {
            skill = SkillS.createSkill(author.id, params.newSkillParams.title, params.newSkillParams.description, params.newSkillParams.pictureToken, tokenizer)
            if (skill == null) {
                logger.warn("Cannot create skill '" + params.newSkillParams.title + "' for new assignment.")
                return null
            }
        }

        val assignment = new AssignmentM()
        assignment.creationTimestamp = TimeHelper.now
        assignment.author = author.id
        assignment.skill = skill.id
        assignment.title = params.title
        assignment.description = params.description
        val reward = new RewardM
        reward.timestamp = TimeHelper.now
        reward.donatingPerson = userPersonId
        if (author.id.toString == Config.SYS_OBJ_ID) {
            reward.amount = Config.SYS_REWARD
        }
        else {
            reward.amount = Balancing.getInitialAssignmentReward(Balancing.getLevel(author.xp))
        }
        assignment.rewards.add(reward)
        assignment.rewardsSum = reward.amount
        logger.info("Creating new assignment " + assignment.id + ".")
        Database.save(assignment)
        Search.instance.update(List(assignment))

        // all persons that are referenced in the sent data
        val personIds = Buffer()
        val persons = personIds.map(it => PersonS.loadPerson(it)).filter(_ != null).toBuffer
        persons += author
        logger.trace("" + persons.size + " persons referenced in assignment " + assignment.id + ".")

        val subscribed = if(userAccountId != null) SubscriptionQ.isSubscribed(userAccountId, assignment.id) else false

        val result = Presenter.present(assignment, skill, persons, List(), userPerson, userIsAdmin, subscribed)

        // make author a founder if founding assignment still exists
        try {
            if (author.founder != true) {
                val founderAssignment = Database.load(classOf[AssignmentM], UUID.fromString(Config.FOUNDER_ASSIGNMENT_ID))
                if (founderAssignment != null) {
                    val founderReward = math.max(0, founderAssignment.rewards.validItems.head.amount)
                    if (PersonU.setFounder(author, founderReward)) {
                        logger.info("Person " + author.id + " was granded founder status and gains " + founderReward + " xp.")
                    }
                }
            }
        }
        catch {
            case _: Throwable => {}
        }

        val account = Database.load(classOf[AccountM], userAccountId)
        if (account != null) {

            if (account.preferences != null && account.preferences.subscribeOwnAssignments) {
                SubscriptionS.subscribe(account.id, assignment.id)
            }

            if (!isCloaked && Config.IS_LIVE) {
                // try to post on facebook
                try {
                    if (account.facebookCredential != null && account.facebookCredential.accessToken != null) {
                        val picture = ImageHelper.getAbsoluteUrl(result.assignment.picture, ImageHelper.Size.MEDIUM)
                        val url = Config.META_URL + "?token=" + "assignment%3D" + result.assignment.id
                        val title = result.assignment.title
                        val description = assignment.description
                        Facebook.publishAction(logger, account.facebookCredential.accessToken, "create", "assignment", url, picture, title, description)
                    }
                }
                catch {
                    case _: Throwable => {}
                }

                // try to post on twitter
                try {
                    if (account.twitterCredential != null && account.twitterCredential.accessToken != null) {
                        val url = Config.META_URL + "?token=" + "assignment%3D" + result.assignment.id
                        val text = "I created \"" + trimEnd(result.assignment.title, 140 - 16 - url.length) + "\" on " + url
                        TwitterS.publish(logger, account.twitterCredential, text)
                    }
                }
                catch {
                    case _: Throwable => {}
                }
            }
        }

        return result
    }

    private def trimEnd(s: String, length: Int): String = {
        if (s.length < length)
            return s
        return s.substring(0, length)
    }

    def deleteAssignment(assignmentId: UUID, userPersonId: UUID, userIsAdmin: Boolean): Boolean = {
        if (assignmentId == null)
            return false

        val assignment = Database.load(classOf[AssignmentM], assignmentId)
        if (assignment == null) {
            logger.warn("Assignment " + assignmentId + " not found.")
            return false
        }

        if (!userIsAdmin) {
            if (assignment.author != userPersonId) {
                logger.warn("Person " + userPersonId + " is not the author of assignment " + assignment.id + " and thus cannot delete it.")
                return false
            }
            if (!assignment.activities.validItems.isEmpty) {
                logger.warn("Person " + userPersonId + " cannot delete assignment " + assignment.id + " because someone is already participating.")
                return false
            }
        }

        logger.info("Deleting assignment " + assignmentId + ".")
        BaseU.setDeleted(assignment)
        Search.instance.update(List(assignment))
        SubscriptionS.unsubscribeAll(assignment.id)

        return true
    }

    def speedupAssignment(assignmentId: UUID, days: Int, userIsAdmin: Boolean) {
        if (assignmentId == null)
            return

        val assignment = AbuseFilter.filter(Database.load(classOf[AssignmentM], assignmentId), userIsAdmin)
        if (assignment == null) {
            logger.warn("Assignment " + assignmentId + " not found.")
            return
        }

        logger.info("Moving all dates of assignment " + assignmentId + " forward by 1 day.")

        if (AssignmentU.speedup(assignment, days)) {
            assignment.activities.validItems.foreach(ref => {
                val activity = Database.load(classOf[ActivityM], ref.activity)
                ActivityU.speedup(activity, days)
            })
        }
    }

    def setAbuseReport(assignmentId: UUID, isAbuse: Boolean, userAccountId: UUID, userPersonId: UUID, userIsAdmin: Boolean): AssignmentVD = {
        if (assignmentId == null || userPersonId == null)
            return null

        val assignment = Database.load(classOf[AssignmentM], assignmentId)
        if (assignment == null) {
            logger.warn("Assignment " + assignmentId + " not found.")
            return null
        }

        // get existing report
        val reportOpt = assignment.abuseReports.validItems.find(report => report.author == userPersonId)

        if (isAbuse) {
            if (reportOpt.isEmpty) {
                logger.info("Creating abuse report for activity " + assignment.id + ".")
                val report = new AbuseReportM()
                report.author = userPersonId
                report.timestamp = TimeHelper.now
                AssignmentU.addAbuseReport(assignment, report)
            }
        }
        else {
            if (reportOpt.isDefined) {
                logger.info("Deleting abuse report for activity " + assignment.id + ".")
                AssignmentU.removeAbuseReport(assignment, reportOpt.get)
            }
        }

        return getAssignment(assignmentId, userAccountId, userPersonId, userIsAdmin)
    }

    def clearAbuseReports(assignmentId: UUID, userAccountId: UUID, userPersonId: UUID, userIsAdmin: Boolean): AssignmentVD = {
        if (assignmentId == null || userPersonId == null)
            return null

        val assignment = Database.load(classOf[AssignmentM], assignmentId)
        if (assignment == null) {
            logger.warn("Assignment " + assignmentId + " not found.")
            return null
        }

        logger.info("Deleting all abuse reports for assignment " + assignment.id + ".")
        AssignmentU.removeAbuseReports(assignment)

        return getAssignment(assignmentId, userAccountId, userPersonId, userIsAdmin)
    }

    def getWithMostAbuseReports(userAccountId: UUID, userPersonId: UUID, userIsAdmin: Boolean): Iterable[AssignmentVD] = {
        val assignmentIds = AssignmentQ.getWithMostAbuseReports(100)
        logger.trace("Found " + assignmentIds.size + " assignments with abuse reports.")
        return assignmentIds.map(it => getAssignment(it, userAccountId, userPersonId, userIsAdmin)).filter(it => it != null)
    }

    def getAssignmentsBySkill(skillId: UUID, offset: Int, userAccountId: UUID, userPersonId: UUID, userIsAdmin: Boolean): Iterable[AssignmentVD] = {
        if (skillId == null)
            return List()

        val assignmentIds = AssignmentQ.getBySkill(skillId, offset, Config.PAGE_SIZE)
        logger.trace("Found " + assignmentIds.size + " assignment ids with skill " + skillId + ".")
        return assignmentIds.map(it => getAssignment(it, userAccountId, userPersonId, userIsAdmin)).filter(it => it != null)
    }


}
