package com.c5000.mastery.backend.services

import _root_.java.util.{TimeZone, UUID}
import com.c5000.mastery.backend.{JspRenderer, TimeHelper, NotificationType}
import com.c5000.mastery.database.Database
import com.c5000.mastery.database.models._
import com.c5000.mastery.database.queries.{SubscriptionQ, PersonQ, NotificationQ}
import com.c5000.mastery.database.updates.NotificationU
import com.c5000.mastery.shared.Config
import collection.mutable
import scala.collection.JavaConversions._
import it.sauronsoftware.cron4j.Scheduler

object NotificationS extends HasServiceLogger {

    private var schedulerInitialized = false

    def initScheduler() {
        synchronized {
            if (schedulerInitialized)
                return

            try {
                val s = new Scheduler()

                s.setTimeZone(TimeZone.getTimeZone("UTC"))

                // try to send pending notifications every hour
                s.schedule("0 * * * *", new Runnable() {
                    def run() {
                        sendEmails()
                    }
                })

                s.start()

                logger.info("Notifications scheduler initialized.")
                schedulerInitialized = true
            }
            catch {
                case ex: Throwable => {
                    logger.error("Error while creating notifications scheduler: " + ex)
                }
            }
        }
    }

    def sendEmails() {
        val accounts = NotificationQ.getUnsentAccounts
        if (!accounts.isEmpty) {
            logger.info("Processing notifications of " + accounts.size + " accounts")
            accounts.foreach(sendEmail(_))
        }
    }

    def sendEmail(accountId: UUID) {
        if (accountId == null) {
            logger.warn("Missing parameter to send notification email.")
            return
        }

        val account = Database.load(classOf[AccountM], accountId)
        if (account == null) {
            logger.warn("Could not find account " + accountId + " to load notifications.")
            return
        }

        try {
            if (!NotificationQ.getUnsent(accountId, getIncludeTypes(account)).isEmpty) {
                val timestamp = TimeHelper.now
                if (account.email != null) {
                    val html = JspRenderer.render("email/notifications.jsp?accountId=" + accountId.toString)
                    if (MailS.send(account.email, "Notifications", html)) {
                        logger.info("Sent notification email to account " + accountId + ".")
                        NotificationU.setSent(accountId, timestamp)
                    }
                    else {
                        logger.warn("Cannot send notifications to account " + accountId + " for some reason. Rescheduling notifications.")
                    }
                }
                else {
                    logger.info("Cannot send email to account " + accountId + " because the email address is unknown. Discarding notifications.")
                    NotificationU.setSent(accountId, timestamp)
                }
            }
        }
        catch {
            case ex: Throwable => {
                logger.error("Error while sending notifications email: " + ex)
            }
        }
    }

    def getNotifications(accountId: UUID): java.util.ArrayList[String] = {
        if (accountId == null) {
            logger.warn("Missing parameter to load notifications.")
            return null
        }

        val account = Database.load(classOf[AccountM], accountId)
        if (account == null) {
            logger.warn("Could not find account " + accountId + " to load notifications.")
            return null
        }

        val notifications = NotificationQ.getUnsent(accountId, getIncludeTypes(account)).map(Database.load(classOf[NotificationM], _)).filter(_ != null)
        val result = mutable.Buffer[String]()

        // activities by other users
        notifications.filter(_.typ == NotificationType.OTHER_ACTIVITY).map(_.html).foreach(result += _)

        // rewards
        val rewards = notifications.filter(item => item.typ == NotificationType.REWARD_ACTIVITY || item.typ == NotificationType.REWARD_OTHER_ACTIVITY)
        if (!rewards.isEmpty) {

            val rewardsSum = rewards.map(_.values.head.toInt).sum
            result += getRewardHtml(account.person, rewardsSum, rewards.map(_.html))

            val person = Database.load(classOf[PersonM], account.person)
            if (person == null) {
                logger.warn("Could not find person " + account.person + " to load notifications.")
                return null
            }

            // stats
            result += getStatsHtml(person.id, person.xp)
        }

        return new java.util.ArrayList(result)
    }

    private def getIncludeTypes(account: AccountM): Iterable[Int] = {
        val result = mutable.Buffer[Int]()

        if (account.preferences != null && account.preferences.notifyOtherActivity)
            result += NotificationType.OTHER_ACTIVITY

        if (account.preferences != null && account.preferences.notifyActivityReward)
            result += NotificationType.REWARD_ACTIVITY

        if (account.preferences != null && account.preferences.notifyOtherActivityReward)
            result += NotificationType.REWARD_OTHER_ACTIVITY

        return result
    }

    def createOtherActivityNotifications(author: PersonM, assignment: AssignmentM) {
        if (author == null || assignment == null) {
            logger.error("Missing parameter to create activity notifications")
            return
        }

        val subscriptions = SubscriptionQ.getByAssignment(assignment.id)
        subscriptions.map(Database.load(classOf[SubscriptionM], _))
            .filter(subscription => subscription != null && subscription.account != author.account)
            .foreach(subscription => createOtherActivityNotification(subscription.account, author, assignment))
    }

    def createOtherActivityNotification(accountId: UUID, author: PersonM, assignment: AssignmentM) {
        if (accountId == null || author == null || assignment == null) {
            logger.error("Missing parameter to create activity notification")
            return
        }

        val notification = new NotificationM
        notification.account = accountId
        notification.timestamp = TimeHelper.now
        notification.typ = NotificationType.OTHER_ACTIVITY
        notification.html = getOtherActivityHtml(author.id, author.name, assignment.id, assignment.title)
        notification.values = List(author.id.toString, author.name, assignment.id.toString, assignment.title)
        logger.info("Creating other-activity notification for account " + accountId)
        Database.save(notification)
    }

    def createActivityRewardNotification(accountId: UUID, xp: Int, assignment: AssignmentM) {
        if (accountId == null || assignment == null) {
            logger.error("Missing parameter to create activity-reward notification")
            return
        }

        val notification = new NotificationM
        notification.account = accountId
        notification.timestamp = TimeHelper.now
        notification.typ = NotificationType.REWARD_ACTIVITY
        notification.html = getActivityRewardHtml(xp, assignment.id, assignment.title)
        notification.values = List(xp.toString, assignment.id.toString, assignment.title)
        logger.info("Creating activity-reward notification for account " + accountId)
        Database.save(notification)
    }

    def createOtherActivityRewardNotification(accountId: UUID, xp: Int, author: PersonM, assignment: AssignmentM) {
        if (accountId == null || author == null || assignment == null) {
            logger.error("Missing parameter to create other-activity-reward notification")
            return
        }

        val notification = new NotificationM
        notification.account = accountId
        notification.timestamp = TimeHelper.now
        notification.typ = NotificationType.REWARD_OTHER_ACTIVITY
        notification.html = getOtherActivityRewardHtml(xp, author.id, author.name, assignment.id, assignment.title)
        notification.values = List(xp.toString, author.id.toString, author.name, assignment.id.toString, assignment.title)
        logger.info("Creating other-activity-reward notification for account " + accountId)
        Database.save(notification)
    }

    /**
     * sanitizes user input for use as the content of html tags. this does not prevent attacks when the user content is used as an html attribute or in js script.
     */
    private def sanitize(text: String): String =
        text.replace("<", "&lt;").replace(">", "&gt;")

    private def a(token: String, content: String) = "<a href=\"" + Config.BASE_URL_GWT + "#" + token + "\" style=\"font-family: Arial; font-size: 10pt;\">" + content + "</a>"

    private def li(content: String) = "<li style=\"margin-top: 10px; margin-bottom: 10px; font-family: Arial; font-size: 10pt;\">" + content + "</li>"

    private def getOtherActivityHtml(authorId: UUID, authorName: String, assignmentId: UUID, assignmentTitle: String): String =
        a("person=" + authorId, sanitize(authorName)) + " has completed " + a("assignment=" + assignmentId, sanitize(assignmentTitle)) + "."

    private def getActivityRewardHtml(xp: Int, assignmentId: UUID, assignmentTitle: String): String =
        "" + xp + " XP for your activity on " + a("assignment=" + assignmentId, sanitize(assignmentTitle)) + "."

    private def getOtherActivityRewardHtml(xp: Int, authorId: UUID, authorName: String, assignmentId: UUID, assignmentTitle: String): String =
        "" + xp + " XP for " + a("person=" + authorId, sanitize(authorName)) + "'s activity on " + a("assignment=" + assignmentId, sanitize(assignmentTitle)) + "."

    private def getRewardHtml(personId: UUID, xp: Int, sources: Iterable[String]): String =
        a("person=" + personId, "You") + " gained " + xp + " XP:<ul>" + sources.map(li(_)).reduceLeft(_ + _) + "</ul>"

    private def getStatsHtml(personId: UUID, xp: Int): String = {

        def place(i: Int): String =
            if (i / 10 == 1) "th"
            else (i % 10) match {
                case 1 => "st"
                case 2 => "nd"
                case 3 => "rd"
                case _ => "th"
            }

        val level = Balancing.getLevel(xp)
        val rank = PersonQ.getRank(xp)
        return a("person=" + personId, "You") + " have " + xp + " XP which puts you on level " + level + " and " + rank + place(rank) + " place worldwide."
    }

}
