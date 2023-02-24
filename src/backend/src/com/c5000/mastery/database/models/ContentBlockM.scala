package com.c5000.mastery.database.models

import com.c5000.mongopa.{MpaList, MpaListField, MpaModel}


@MpaModel
class ContentBlockM extends UniqueIdModelBase {

    /**
     * @see ContentBlockD
     */
    var typ: Int = 0

    var value: ResourceM = null

    /**
     * the content was uploaded by the activity author
     */
    var authentic: java.lang.Boolean = null

}
