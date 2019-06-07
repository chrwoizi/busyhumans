package com.c5000.mastery.database.models

import com.c5000.mongopa.MpaModel

@MpaModel(collection = "CITY")
class CityM extends UniqueIdModelBase {

    var facebookId: String = null
    var name: String = null

}
