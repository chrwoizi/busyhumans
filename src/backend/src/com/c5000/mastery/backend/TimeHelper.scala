package com.c5000.mastery.backend

import org.joda.time.{DateTimeZone, DateTime}


object TimeHelper {

    def now: DateTime = DateTime.now(DateTimeZone.UTC)

}
