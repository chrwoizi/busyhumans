package com.c5000.mongopa.internal


class ClassDesc {
    var clazz: Class[_] = null
    var dbCollection: String = null
    var key: FieldDesc = null
    var deletedFlag: FieldDesc = null
    var columns: List[FieldDesc] = null
}