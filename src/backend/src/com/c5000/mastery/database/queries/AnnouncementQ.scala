package com.c5000.mastery.database.queries

import java.util.UUID
import com.c5000.mastery.database.models.AnnouncementM
import org.joda.time.DateTime
import com.c5000.mastery.backend.TimeHelper
import com.mongodb.client.model.Filters

object AnnouncementQ extends BaseQ(classOf[AnnouncementM]) {

    /**
     * @return announcement ids where the timestamp is after the given time
     */
    def getAfter(time: DateTime): Iterable[UUID] = {
        var query = Filters.and(
            Filters.eq("deleted", false),
            Filters.lt("showTime", TimeHelper.now.toDate),
            Filters.or(
                Filters.eq("hideTime", null),
                Filters.gt("hideTime", TimeHelper.now.toDate)
            )
        )

        if (time != null) {
            query = Filters.and(
                query,
                Filters.gt("showTime", time.toDate)
            )
        }

        return getIds(query, 0, 100, "showTime", 1)
    }

    def getLatest(): Iterable[UUID] = {
        val query = Filters.and(
            Filters.eq("deleted", false),
            Filters.lt("showTime", TimeHelper.now.toDate),
            Filters.gt("hideTime", TimeHelper.now.toDate)
        )

        return getIds(query, 0, 1, "showTime", -1)
    }

}
