package com.c5000.mastery.database.updates

import com.mongodb.casbah.commons.MongoDBObject
import com.c5000.mastery.database.models._

object SkillU extends BaseU(classOf[SkillM]) {

    def addAbuseReport(skill: SkillM, abuseReport: AbuseReportM) {
        val query = makeQuery(skill.id)
        addItem(query, "abuseReports", abuseReport)
        if (!abuseReport.deleted) {
            increment(query, "abuseReportsCount", 1)
            skill.abuseReportsCount += 1
        }
        skill.abuseReports.add(abuseReport)
    }

    def removeAbuseReport(skill: SkillM, abuseReport: AbuseReportM) {
        val query = makeQuery(skill.id,
            MongoDBObject("abuseReports" ->
                MongoDBObject("$elemMatch" ->
                    MongoDBObject("id" -> abuseReport.id, "deleted" -> false))))
        set(query, "abuseReports.$.deleted", true)
        if (!abuseReport.deleted) {
            increment(makeQuery(skill.id), "abuseReportsCount", -1)
            skill.abuseReportsCount -= 1
        }
        skill.abuseReports.setDeleted(abuseReport)
    }

    def removeAbuseReports(skill: SkillM) {
        skill.abuseReports.validItems.foreach(abuse => {
            removeAbuseReport(skill, abuse)
        })
    }

}
