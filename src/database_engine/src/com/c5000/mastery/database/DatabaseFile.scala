package com.c5000.mastery.database

import java.util.UUID
import java.io.InputStream

class DatabaseFile {
    var id: UUID = null
    var part: String = null
    var stream: InputStream = null
    var size: Int = 0
    var contentType: String = null
}
