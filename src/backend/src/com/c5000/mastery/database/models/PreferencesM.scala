package com.c5000.mastery.database.models

import com.c5000.mongopa.MpaModel
import org.joda.time.DateTime

@MpaModel
class PreferencesM extends UniqueIdModelBase {

    var subscribeOwnAssignments: Boolean = true
    var notifyOtherActivity: Boolean = true
    var notifyActivityReward: Boolean = true
    var notifyOtherActivityReward: Boolean = true

}
