package com.c5000.mastery.database.models

import com.c5000.mongopa.{MpaList, MpaListField, MpaModel}
import org.joda.time.DateTime
import java.util.UUID

@MpaModel(collection = "SKILL")
class SkillM extends UniqueIdModelBase {

    var author: UUID = null
    var creationTimestamp: DateTime = null

    var title: String = null
    var description: ResourceM = null
    var picture: ResourceM = null

    @MpaListField(itemType = classOf[AbuseReportM])
    var abuseReports: MpaList[AbuseReportM] = new MpaList
    var abuseReportsCount: Int = 0

}
