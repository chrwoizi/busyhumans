package com.c5000.mastery.backend.services

import _root_.java.util.UUID
import com.c5000.mastery.backend.TimeHelper
import com.c5000.mastery.database.Database
import com.c5000.mastery.database.models.SubscriptionM
import com.c5000.mastery.database.queries.{AssignmentQ, SubscriptionQ}
import com.c5000.mastery.shared.data.base.SortBy


object SubscriptionS extends HasServiceLogger {

    def subscribeToAll(accountId: UUID) {
        if (accountId == null) {
            logger.warn("Missing parameter for subscribing to all assignments")
            return
        }

        AssignmentQ.getAll(0, 999, SortBy.NEWEST)
            .filter(!SubscriptionQ.isSubscribed(accountId, _))
            .foreach(subscribe(accountId, _))
    }

    def subscribe(accountId: UUID, assignmentId: UUID) {
        if (accountId == null || assignmentId == null) {
            logger.warn("Missing parameter for subscribing to assignment")
            return
        }

        if (SubscriptionQ.isSubscribed(accountId, assignmentId))
            return

        val subscription = new SubscriptionM
        subscription.timestamp = TimeHelper.now
        subscription.account = accountId
        subscription.assignment = assignmentId
        logger.info("Account " + accountId + " subscribes to assignment " + assignmentId)
        Database.save(subscription)
    }

    def unsubscribe(accountId: UUID, assignmentId: UUID) {
        if (accountId == null || assignmentId == null) {
            logger.warn("Missing parameter for unsubscribing from assignment")
            return
        }

        if (!SubscriptionQ.isSubscribed(accountId, assignmentId))
            return

        val id = SubscriptionQ.get(accountId, assignmentId)
        if (id == null) {
            logger.warn("Cannot find subscription of account " + accountId + " to assignment " + assignmentId + " while unsubscribing")
            return
        }

        val subscription = Database.load(classOf[SubscriptionM], id)
        if (subscription == null) {
            logger.warn("Cannot load subscription of account " + accountId + " to assignment " + assignmentId + " while unsubscribing")
            return
        }

        logger.info("Account " + accountId + " unsubscribes from assignment " + assignmentId)
        subscription.deleted = true
        Database.save(subscription)
    }

    def unsubscribeAll(assignmentId: UUID) {
        if (assignmentId == null) {
            logger.warn("Missing parameter for unsubscribing all from assignment")
            return
        }

        SubscriptionQ.getByAssignment(assignmentId).map(Database.load(classOf[SubscriptionM], _)).filter(_ != null)
            .foreach(subscription => unsubscribe(subscription.account, subscription.assignment))
    }
}
