package com.c5000.mastery.database.models

import java.util.UUID
import com.c5000.mongopa.{MpaList, MpaListField, MpaModel}

@MpaModel(collection = "ACHIEVEMENT")
class AchievementM extends UniqueIdModelBase {

    var owner: UUID = null
    var skill: UUID = null

    @MpaListField(itemType = classOf[ListItemM])
    var activities: MpaList[ListItemM] = new MpaList

}
