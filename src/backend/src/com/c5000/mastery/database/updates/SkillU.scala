package com.c5000.mastery.database.updates

import com.c5000.mastery.database.models._
import com.mongodb.client.model.Filters

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
            Filters.elemMatch("abuseReports",
                Filters.and(Filters.eq("id", abuseReport.id), Filters.eq("deleted", false))))
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
