package com.c5000.mastery.database.updates

import com.c5000.mastery.database.models._

import java.util.UUID
import com.mongodb.client.model.{Filters, Updates}

object ActivityU extends BaseU(classOf[ActivityM]) {

    def addRating(activity: ActivityM, rating: RatingM) {
        val query = makeQuery(activity.id)
        addItem(query, "ratings", rating)
        activity.ratings.add(rating)
    }

    def removeRating(activity: ActivityM, rating: RatingM) {
        val query = makeQuery(activity.id,
            Filters.elemMatch("ratings",
                    Filters.and(Filters.eq("id", rating.id), Filters.eq("deleted", false))))
        set(query, "ratings.$.deleted", true)
        activity.ratings.setDeleted(rating)
    }

    def addAbuseReport(activity: ActivityM, abuseReport: AbuseReportM) {
        val query = makeQuery(activity.id)
        addItem(query, "abuseReports", abuseReport)
        if (!abuseReport.deleted) {
            increment(query, "abuseReportsCount", 1)
            activity.abuseReportsCount += 1
        }
        activity.abuseReports.add(abuseReport)
    }

    def removeAbuseReport(activity: ActivityM, abuseReport: AbuseReportM) {
        val query = makeQuery(activity.id,
            Filters.elemMatch("abuseReports",
                    Filters.and(Filters.eq("id", abuseReport.id), Filters.eq("deleted", false))))
        set(query, "abuseReports.$.deleted", true)
        if (!abuseReport.deleted) {
            increment(makeQuery(activity.id), "abuseReportsCount", -1)
            activity.abuseReportsCount -= 1
        }
        activity.abuseReports.setDeleted(abuseReport)
    }

    def removeAbuseReports(activity: ActivityM) {
        activity.abuseReports.validItems.foreach(abuse => {
            removeAbuseReport(activity, abuse)
        })
    }

    def reward(activity: ActivityM, personId: UUID, rewarded: Int): Boolean = {
        val query = makeQuery(activity.id, Filters.eq("rewarded", null))
        if(set(query, "rewarded", rewarded)) {
            activity.rewarded = rewarded
            PersonU.incrementXp(activity.author, activity.rewarded)
            return true
        }
        return false
    }

    def speedup(activity: ActivityM, days: Int) {

        val query = makeQuery(activity.id, Filters.eq("timestamp", activity.timestamp.toDate))
        setMulti(query, Updates.combine(
            Updates.set("timestamp", activity.timestamp.minusDays(days).toDate),
            Updates.set("rewardDueDate", activity.rewardDueDate.minusDays(days).toDate)
        ))

        activity.timestamp = activity.timestamp.minusDays(days)
        activity.rewardDueDate = activity.rewardDueDate.minusDays(days)
    }

}
