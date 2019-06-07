package com.c5000.mastery.database.models

import org.joda.time.DateTime
import java.util.UUID
import com.c5000.mongopa.{MpaList, MpaListField, MpaModel}

@MpaModel(collection = "ACTIVITY")
class ActivityM extends UniqueIdModelBase {

    var author: UUID = null
    var timestamp: DateTime = null

    var achievement: UUID = null
    var assignment: UUID = null

    @MpaListField(itemType = classOf[ContentBlockM])
    var contentBlocks: MpaList[ContentBlockM] = new MpaList

    @MpaListField(itemType = classOf[RatingM])
    var ratings: MpaList[RatingM] = new MpaList

    @MpaListField(itemType = classOf[AbuseReportM])
    var abuseReports: MpaList[AbuseReportM] = new MpaList
    var abuseReportsCount: Int = 0

    /**
     * the ratings at this date will be used to determine the reward
     */
    var rewardDueDate: DateTime = null

    /**
     * before rating due date: null
     * after rating due date: [-sum(assignment.rewards.amount), +sum(assignment.rewards.amount)] based on ratings on the result activity
     */
    var rewarded: java.lang.Integer = null

}
