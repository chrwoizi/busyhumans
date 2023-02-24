package com.c5000.mastery.backend.services

import com.c5000.mastery.database.models.{SkillM, AssignmentM, ActivityM}

object AbuseFilter {

    val HIDE_AT_ABUSE_REPORTS = 5

    def filter(activity: ActivityM, userIsAdmin: Boolean): ActivityM = {
        if (activity == null || (!userIsAdmin && activity.abuseReportsCount >= HIDE_AT_ABUSE_REPORTS))
            return null
        return activity
    }

    def filter(assignment: AssignmentM, userIsAdmin: Boolean): AssignmentM = {
        if (assignment == null || (!userIsAdmin && assignment.abuseReportsCount >= HIDE_AT_ABUSE_REPORTS))
            return null
        return assignment
    }

    def filter(skill: SkillM, userIsAdmin: Boolean): SkillM = {
        if (skill == null || (!userIsAdmin && skill.abuseReportsCount >= HIDE_AT_ABUSE_REPORTS))
            return null
        return skill
    }

}
