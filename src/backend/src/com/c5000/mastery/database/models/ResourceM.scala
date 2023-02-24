package com.c5000.mastery.database.models

import com.c5000.mongopa.MpaModel
import com.c5000.mastery.shared.data.base.LicenseTypes

@MpaModel
class ResourceM extends UniqueIdModelBase {

    var resource: String = null
    var small: String = null
    var medium: String = null
    var large: String = null
    var hires: String = null

    var authorName: String = null
    var authorUrl: String = null
    var license: Int = LicenseTypes.UNKNOWN

}
