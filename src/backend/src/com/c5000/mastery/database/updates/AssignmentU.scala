package com.c5000.mastery.database.updates

import com.c5000.mastery.database.Database
import com.c5000.mastery.database.models._
import com.mongodb.client.model.Filters

import java.util

object AssignmentU extends BaseU(classOf[AssignmentM]) {

    def addBoost(assignmentId: util.UUID, reward: RewardM): Boolean = {
        val query = makeQuery(assignmentId,
            Filters.not(Filters.elemMatch("rewards", Filters.and(
                Filters.eq("deleted", false), Filters.eq("donatingPerson", reward.donatingPerson)))))
        if(addItem(query, "rewards", reward)) {
            val query2 = makeQuery(assignmentId)
            increment(query2, "rewardsSum", reward.amount)
            return true
        }
        return false
    }

    def removeBoost(assignmentId: util.UUID, reward: RewardM): Boolean = {
        val query = makeQuery(assignmentId,
            Filters.elemMatch("rewards",
                Filters.and(Filters.eq("id", reward.id), Filters.eq("deleted", false))))
        if(set(query, "rewards.$.deleted", true)) {
            val query2 = makeQuery(assignmentId)
            increment(query2, "rewardsSum", -reward.amount)
            return true
        }
        return false
    }

    def speedup(assignment: AssignmentM, days: Int): Boolean = {

        val query = makeQuery(assignment.id, Filters.eq("creationTimestamp", assignment.creationTimestamp.toDate))
        val ok = set(query, "creationTimestamp", assignment.creationTimestamp.minusDays(days).toDate)

        assignment.creationTimestamp = assignment.creationTimestamp.minusDays(days)

        return ok
    }

    def addActivity(assignment: AssignmentM, activityRef: ActivityRefM, activity: ActivityM): Boolean = {
        var query = makeQuery(assignment.id)
        val ok = addItem(query, "activities", activityRef)
        if (ok) {
            assignment.activities.add(activityRef)
            set(query, "lastActivity", activity.timestamp.toDate)
            return true
        }
        return false
    }

    def removeActivity(assignment: AssignmentM, activity: ActivityRefM): Boolean = {
        val query = makeQuery(assignment.id,
            Filters.elemMatch("activities",
                Filters.and(Filters.eq("deleted", false), Filters.eq("id", activity.id))))
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
            Filters.elemMatch("abuseReports",
                Filters.and(Filters.eq("id", abuseReport.id), Filters.eq("deleted", false))))
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
