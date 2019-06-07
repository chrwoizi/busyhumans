package com.c5000.mastery.database.models

import org.joda.time.DateTime
import java.util.UUID
import com.c5000.mongopa.{MpaList, MpaListField, MpaModel}

@MpaModel(collection = "ASSIGNMENT")
class AssignmentM extends UniqueIdModelBase {

    var creationTimestamp: DateTime = null
    var author: UUID = null

    var skill: UUID = null
    var title: String = null
    var description: String = null

    @MpaListField(itemType = classOf[RewardM])
    var rewards: MpaList[RewardM] = new MpaList
    var rewardsSum: java.lang.Integer = 0

    @MpaListField(itemType = classOf[ActivityRefM])
    var activities: MpaList[ActivityRefM] = new MpaList
    var lastActivity: DateTime = null

    @MpaListField(itemType = classOf[AbuseReportM])
    var abuseReports: MpaList[AbuseReportM] = new MpaList
    var abuseReportsCount: Int = 0

}
