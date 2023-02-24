package com.c5000.mastery.database.versions

import org.slf4j.LoggerFactory
import com.c5000.mastery.database.models.{PersonM, SubscriptionM, AssignmentM}
import com.c5000.mastery.database.Database
import com.c5000.mastery.database.queries.SubscriptionQ
import com.c5000.mastery.shared.Config
import java.util.UUID

/**
 * subscribe assignment authors
 */
object Version15 {
    private val log = LoggerFactory.getLogger(getClass)

    def update() {
        Database.forEach(classOf[AssignmentM], includeDeleted = false) {
            assignment => {
                if (assignment.author != null) {
                    val person = Database.load(classOf[PersonM], assignment.author)
                    if (person != null) {
                        if (person.account != null && person.account != UUID.fromString(Config.SYS_OBJ_ID)
                            && !SubscriptionQ.isSubscribed(person.account, assignment.id)) {
                            val subscription = new SubscriptionM
                            subscription.timestamp = assignment.creationTimestamp
                            subscription.account = person.account
                            subscription.assignment = assignment.id
                            log.info("Account " + person.account + " subscribes to assignment " + assignment.id)
                            Database.save(subscription)
                        }
                    }
                }
            }
        }
    }
}
