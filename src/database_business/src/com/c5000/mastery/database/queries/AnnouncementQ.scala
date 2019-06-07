package com.c5000.mastery.database.queries

import java.util.UUID
import com.mongodb.casbah.commons.MongoDBObject
import com.c5000.mastery.database.models.AnnouncementM
import org.joda.time.DateTime
import com.mongodb.casbah._
import com.c5000.mastery.backend.TimeHelper

object AnnouncementQ extends BaseQ(classOf[AnnouncementM]) {

    /**
     * @return announcement ids where the timestamp is after the given time
     */
    def getAfter(time: DateTime): Iterable[UUID] = {
        var query = $and(
            MongoDBObject("deleted" -> false),
            MongoDBObject("showTime" -> MongoDBObject("$lt" -> TimeHelper.now.toDate)),
            $or(
                MongoDBObject("hideTime" -> null),
                MongoDBObject("hideTime" -> MongoDBObject("$gt" -> TimeHelper.now.toDate))
            )
        )

        if (time != null) {
            query = $and(
                query,
                MongoDBObject("showTime" -> MongoDBObject("$gt" -> time.toDate))
            )
        }

        return getIds(query, 0, 100, "showTime", 1)
    }

    def getLatest(): Iterable[UUID] = {
        val query = $and(
            MongoDBObject("deleted" -> false),
            MongoDBObject("showTime" -> MongoDBObject("$lt" -> TimeHelper.now.toDate)),
            MongoDBObject("hideTime" -> MongoDBObject("$gt" -> TimeHelper.now.toDate))
        )

        return getIds(query, 0, 1, "showTime", -1)
    }

}
