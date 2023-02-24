package com.c5000.mastery.backend.services

import _root_.java.util.UUID
import com.c5000.mastery.database.Database
import com.c5000.mastery.database.models.{AccountM, AnnouncementM}
import com.c5000.mastery.database.queries.AnnouncementQ
import com.c5000.mastery.database.updates.AccountU
import com.c5000.mastery.shared.Config
import com.c5000.mastery.shared.data.base.AnnouncementD


object AccountS extends HasServiceLogger {

    def getAnnouncements(accountId: UUID): Iterable[AnnouncementD] = {

            var announcementIds: Iterable[UUID] = null

            if (accountId != null) {
                val account = Database.load(classOf[AccountM], accountId)
                if (account == null) {
                    logger.warn("Account " + accountId + " does not exist.")
                    return null
                }
                announcementIds = AnnouncementQ.getAfter(account.lastAnnouncement)
            }
            else {
                announcementIds = AnnouncementQ.getLatest()
            }

            val announcements = announcementIds.map(Database.load(classOf[AnnouncementM], _)).filter(_ != null)
            return announcements.map(Presenter.present(_))
        }

}
