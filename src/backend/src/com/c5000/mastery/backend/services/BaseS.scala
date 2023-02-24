package com.c5000.mastery.backend.services

import _root_.java.util.UUID
import _root_.javax.servlet.http.HttpSession
import com.c5000.mastery.database.models.AccountM
import com.c5000.mastery.database.{Database, SpecialUsers}
import com.c5000.mastery.shared.AccessException
import com.google.gwt.user.server.rpc.RemoteServiceServlet
import scala.collection.JavaConverters._

object BaseS {

    protected def getUserAccountId(session: HttpSession): UUID = {
        val authData = AuthS.getAuthData(session)
        if (authData != null) {
            return authData.accountId
        }
        return null
    }

    def userIsAdmin(session: HttpSession): Boolean = SpecialUsers.isAdmin(getUserAccountId(session))

    def getCloak(session: HttpSession, accountId: UUID): UUID = {
        if (userIsAdmin(session)) {
            val account = Database.load(classOf[AccountM], accountId)
            if (account != null) {
                return account.cloak
            }
        }
        return null
    }

    def getUserPersonId(session: HttpSession): UUID = {
        val authData = AuthS.getAuthData(session)
        if (authData != null) {
            val cloak = getCloak(session, authData.accountId)
            if (cloak != null)
                return cloak
            return authData.personId
        }
        return null
    }

    def getUserEmail(session: HttpSession): String = {
        val authData = AuthS.getAuthData(session)
        if (authData != null) {
            return authData.email
        }
        return null
    }

}

class BaseS extends RemoteServiceServlet with HasServiceLogger {

    protected def getUserAccountId: UUID = BaseS.getUserAccountId(getThreadLocalRequest.getSession)

    protected def getUserPersonId: UUID = BaseS.getUserPersonId(getThreadLocalRequest.getSession)

    protected def getUserEmail: String = BaseS.getUserEmail(getThreadLocalRequest.getSession)

    protected def getCloak: UUID = BaseS.getCloak(getThreadLocalRequest.getSession, getUserAccountId)

    protected def userIsAdmin(): Boolean = SpecialUsers.isAdmin(getUserAccountId)

    protected def assertUserAccess() {
        if (getUserPersonId == null) {
            logger.warn("Tried to access user-restricted rpc")
            throw new AccessException
        }
    }

    protected def assertAdminAccess() {
        if (!userIsAdmin) {
            logger.warn("Tried to access admin-restricted rpc")
            throw new AccessException
        }
    }

    protected def onCall(method: String, params: Any*) {
        ServiceLogger.logHead.set(getThreadLocalRequest.getRemoteAddr + "/" + getUserAccountId + ": ")
        val paramsStr: String = if (params.isEmpty) "" else params.map(x => String.valueOf(x)).reduceLeft(_ + ", " + _)
        logger.trace(getClass.getName + "#" + method + "(" + paramsStr + ")")
    }

    protected def asArrayList[ItemType](iterable: Iterable[ItemType]): java.util.ArrayList[ItemType] = {
        if (iterable == null)
            return new java.util.ArrayList()
        return new java.util.ArrayList(iterable.asJavaCollection)
    }

}
