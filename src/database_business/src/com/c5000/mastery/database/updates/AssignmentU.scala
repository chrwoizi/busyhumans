package com.c5000.mastery.database.updates

import com.c5000.mastery.database.Database
import com.c5000.mastery.database.models._
import com.mongodb.casbah._
import commons.MongoDBObject
import java.util

object AssignmentU extends BaseU(classOf[AssignmentM]) {

    def addBoost(assignmentId: util.UUID, reward: RewardM): Boolean = {
        val query = makeQuery(assignmentId,
            MongoDBObject("rewards" ->
                MongoDBObject("$not" ->
                    MongoDBObject("$elemMatch" ->
                        MongoDBObject("deleted" -> false, "donatingPerson" -> reward.donatingPerson)))))
        if(addItem(query, "rewards", reward)) {
            val query2 = makeQuery(assignmentId)
            increment(query2, "rewardsSum", reward.amount)
            return true
        }
        return false
    }

    def removeBoost(assignmentId: util.UUID, reward: RewardM): Boolean = {
        val query = makeQuery(assignmentId,
            MongoDBObject("rewards" ->
                MongoDBObject("$elemMatch" ->
                    MongoDBObject("id" -> reward.id, "deleted" -> false))))
        if(set(query, "rewards.$.deleted", true)) {
            val query2 = makeQuery(assignmentId)
            increment(query2, "rewardsSum", -reward.amount)
            return true
        }
        return false
    }

    def speedup(assignment: AssignmentM, days: Int): Boolean = {

        val query = makeQuery(assignment.id, MongoDBObject("creationTimestamp" -> assignment.creationTimestamp.toDate))
        set(query, "creationTimestamp", assignment.creationTimestamp.minusDays(days).toDate)

        assignment.creationTimestamp = assignment.creationTimestamp.minusDays(days)

        return Database.mongodb.getLastError().ok()
    }

    def addActivity(assignment: AssignmentM, activityRef: ActivityRefM, activity: ActivityM): Boolean = {
        var query = makeQuery(assignment.id)
        addItem(query, "activities", activityRef)
        if (Database.mongodb.getLastError().ok()) {
            assignment.activities.add(activityRef)
            set(query, "lastActivity", activity.timestamp.toDate)
            return true
        }
        return false
    }

    def removeActivity(assignment: AssignmentM, activity: ActivityRefM): Boolean = {
        val query = makeQuery(assignment.id,
            MongoDBObject("activities" ->
                MongoDBObject("$elemMatch" ->
                    MongoDBObject("deleted" -> false, "id" -> activity.id))))
        if (set(query, "activities.$.deleted", true)) {
            assignment.activities.setDeleted(activity)

            // update lastActivity
            val remaining = assignment.activities.validItems.map(Database.load(classOf[ActivityM], _))
            if(remaining.isEmpty)
                set(makeQuery(assignment.id), "lastActivity", null)
            else
                set(makeQuery(assignment.id), "lastActivity", remaining.maxBy(_.timestamp.getMillis).timestamp.toDate)

            return true
        }
        else return false
    }

    def addAbuseReport(assignment: AssignmentM, abuseReport: AbuseReportM) {
        val query = makeQuery(assignment.id)
        addItem(query, "abuseReports", abuseReport)
        if (!abuseReport.deleted) {
            increment(query, "abuseReportsCount", 1)
            assignment.abuseReportsCount += 1
        }
        assignment.abuseReports.add(abuseReport)
    }

    def removeAbuseReport(assignment: AssignmentM, abuseReport: AbuseReportM) {
        val query = makeQuery(assignment.id,
            MongoDBObject("abuseReports" ->
                MongoDBObject("$elemMatch" ->
                    MongoDBObject("id" -> abuseReport.id, "deleted" -> false))))
        set(query, "abuseReports.$.deleted", true)
        if (!abuseReport.deleted) {
            increment(makeQuery(assignment.id), "abuseReportsCount", -1)
            assignment.abuseReportsCount -= 1
        }
        assignment.abuseReports.setDeleted(abuseReport)
    }

    def removeAbuseReports(assignment: AssignmentM) {
        assignment.abuseReports.validItems.foreach(abuse => {
            removeAbuseReport(assignment, abuse)
        })
    }

}
