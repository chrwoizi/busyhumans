package com.c5000.mongopa.internal

import java.lang.reflect.Field
import java.lang.reflect.Method

class FieldDesc {
    var dbField: String = null
    var field: Field = null
    var getter: Method = null
    var setter: Method = null
    var listItemType: Class[_] = null
    var isIterable: Boolean = false
}