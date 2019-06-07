package com.c5000.mastery.backend.services

import _root_.java.util.{Date, ArrayList, UUID}
import _root_.javax.servlet.http.{HttpServletRequest, Cookie}
import com.c5000.mastery.backend.anon.AnonCredentialHelper
import com.c5000.mastery.backend.auth._
import com.c5000.mastery.backend.{Tokenizer, Initializer, TimeHelper}
import com.c5000.mastery.database.Database
import com.c5000.mastery.database.models.{AssignmentM, PersonM, AnnouncementM, AccountM}
import com.c5000.mastery.database.queries.AssignmentQ
import com.c5000.mastery.database.search.{SearchBeanTypes, Search}
import com.c5000.mastery.database.updates.AccountU
import com.c5000.mastery.shared.Config
import com.c5000.mastery.shared.data.auth._
import com.c5000.mastery.shared.data.base._
import com.c5000.mastery.shared.data.combined._
import com.c5000.mastery.shared.services.IMasteryS
import scala.collection.JavaConversions._
import collection.mutable

class MasteryS extends BaseS with IMasteryS {

    override def init() {
        Initializer.init()
    }

    override def init(clientTime: Long): InitResultD = {
        onCall("init", clientTime)
        val startTime = TimeHelper.now.toDate.getTime

        // add other init code here

        val result = new InitResultD
        result.clockSync = new ClockSyncResultD()
        result.clockSync.serverThinkingDuration = TimeHelper.now.toDate.getTime - startTime
        result.clockSync.clientTime = clientTime
        result.clockSync.serverTime = TimeHelper.now.toDate.getTime

        return result
    }

    override def sync(clientTime: Long): SyncResultD = {
        onCall("sync", clientTime)

        val result = new SyncResultD()
        result.serverThinkingDuration = 0L
        result.clientTime = clientTime
        result.serverTime = TimeHelper.now.toDate.getTime
        result.announcements = asArrayList(AccountS.getAnnouncements(getUserAccountId))
        return result
    }

    override def syncClocks(clientTime: Long): ClockSyncResultD = {
        onCall("syncClocks", clientTime)

        val result = new ClockSyncResultD()
        result.serverThinkingDuration = 0L
        result.clientTime = clientTime
        result.serverTime = TimeHelper.now.toDate.getTime
        return result
    }

    override def authWithFacebook(username: String, accessToken: String): AuthResultD = {
        onCall("authWithFacebook", username, accessToken)

        auth(new FacebookAuthProvider, new FacebookClientCredential(username, accessToken))
    }

    override def authWithTwitter(verifier: String): AuthResultD = {
        onCall("authWithTwitter", verifier)

        auth(new TwitterAuthProvider, new TwitterClientCredential(verifier))
    }

    override def authWithAnon(username: String, passwordClear: String): AuthResultD = {
        onCall("authWithAnon", username, AnonCredentialHelper.getHash(passwordClear, username))

        auth(new AnonAuthProvider, new AnonClientCredential(username, passwordClear))
    }

    override def authWithSession(): AuthResultD = {
        onCall("authWithSession")

        auth(null, null)
    }

    override def authWithNewAnon(recaptchaChallenge: String, recaptchaResponse: String, username: String, passwordClear: String, name: String, pictureToken: String): AnonRegisterResultD = {
        onCall("authWithNewAnon", recaptchaChallenge, recaptchaResponse, username, AnonCredentialHelper.getHash(passwordClear, username), name, pictureToken)

        val result = new AnonRegisterResultD()
        val request: HttpServletRequest = getThreadLocalRequest
        AuthS.authWithNewAnon(
            request.getRemoteAddr,
            recaptchaChallenge,
            recaptchaResponse,
            username,
            passwordClear,
            name,
            pictureToken,
            result,
            getThreadLocalRequest.getSession,
            getThreadLocalResponse,
            getThreadLocalRequest.getCookies,
            (account: AccountM, user: Object) => {
                PersonS.updatePerson(account, user)
            }
        )

        setSpecialUsers(result)

        return result
    }

    def auth(authProvider: IAuthProvider, clientCredential: ClientCredential): AuthResultD = {

        val result = new AuthResultD()
        val request: HttpServletRequest = getThreadLocalRequest
        AuthS.setAuth(
            result,
            authProvider,
            clientCredential,
            request.getSession,
            getThreadLocalResponse,
            getThreadLocalRequest.getCookies,
            (account: AccountM, user: Object) => {
                PersonS.updatePerson(account, user)
            }
        )

        setSpecialUsers(result)

        return result
    }

    private def setSpecialUsers(result: AuthResultD) {
        result.status match {
            case AuthStatus.AUTHORIZED | AuthStatus.NEEDS_TOS_CONFIRMATION => {
                val userPersonId = getUserPersonId
                if (userPersonId != null) {
                    result.accountId = getUserAccountId.toString
                    result.personId = getUserPersonId.toString
                    result.emailAddress = getUserEmail
                    if (userIsAdmin()) {
                        result.isAdmin = true
                        result.cloaks = asArrayList(PersonS.getCloaks)
                    }
                    else {
                        result.isAdmin = false
                        result.cloaks = new ArrayList[CloakD]()
                    }
                }
            }
            case _ => {}
        }
    }

    override def unauth(): AuthResultD = {
        onCall("unauth")

        val result = new AuthResultD()
        result.status = AuthS.unauth(getThreadLocalRequest.getSession, getThreadLocalResponse, getUserAccountId)
        return result
    }

    override def confirmTos() {
        onCall("confirmTos")
        assertUserAccess()

        AuthS.confirmTos(getUserAccountId)
    }

    override def checkGoogleCredential: CredentialCheckResultD = {
        onCall("checkGoogleCredential")

        return AuthS.checkCredential(getUserAccountId, getThreadLocalRequest.getSession, new GoogleAuthProvider)
    }

    override def checkTwitterCredential: CredentialCheckResultD = {
        onCall("checkTwitterCredential")

        return AuthS.checkCredential(getUserAccountId, getThreadLocalRequest.getSession, new TwitterAuthProvider)
    }

    override def setGoogleCredential(authCode: String): java.lang.Boolean = {
        onCall("setGoogleCredential", authCode)

        return AuthS.validateCredential(getUserAccountId, getThreadLocalRequest.getSession, new GoogleAuthProvider, new GoogleClientCredential(authCode))
    }

    override def getMyself: PersonD = {
        onCall("getMyself")
        assertUserAccess()

        return PersonS.getPerson(getUserPersonId)
    }

    override def getPerson(personIdStr: String): PersonD = {
        onCall("getPerson", personIdStr)

        if(personIdStr == null)
            return null

        return PersonS.getPerson(UUID.fromString(personIdStr))
    }

    override def getPersonAchievements(personIdStr: String, offset: Int): java.util.ArrayList[AchievementVD] = {
        onCall("getPersonAchievements", personIdStr, offset)

        return asArrayList(AchievementS.getPersonAchievements(UUID.fromString(personIdStr), getUserAccountId, getUserPersonId, userIsAdmin(), offset))
    }

    override def getAchievement(achievementIdStr: String): AchievementVD = {
        onCall("getAchievement", achievementIdStr)

        return AchievementS.getAchievement(UUID.fromString(achievementIdStr), getUserAccountId, getUserPersonId, userIsAdmin())
    }

    override def getAssignment(assignmentIdStr: String): AssignmentVD = {
        onCall("getAssignment", assignmentIdStr)

        return AssignmentS.getAssignment(UUID.fromString(assignmentIdStr), getUserAccountId, getUserPersonId, userIsAdmin())
    }

    override def assignmentExists(title: String): Boolean = {
        onCall("assignmentExists", title)
        assertUserAccess()

        return AssignmentQ.existsWithTitle(title)
    }

    override def getCompletedAssignments(personIdStr: String, offset: Int): java.util.ArrayList[AssignmentVD] = {
        onCall("getCompletedAssignments", personIdStr, offset)

        return asArrayList(AssignmentS.getCompletedAssignments(UUID.fromString(personIdStr), getUserAccountId, getUserPersonId, userIsAdmin(), offset))
    }

    override def getActiveAssignments(offset: Int, sortBy: SortBy): java.util.ArrayList[AssignmentVD] = {
        onCall("getActiveAssignments", offset, sortBy)

        return asArrayList(AssignmentS.getActiveAssignments(getUserAccountId, getUserPersonId, userIsAdmin(), offset, sortBy))
    }

    override def getCreatedAssignments(personIdStr: String, offset: Int): java.util.ArrayList[AssignmentVD] = {
        onCall("getCreatedAssignments", personIdStr, offset)

        return asArrayList(AssignmentS.getCreatedAssignments(UUID.fromString(personIdStr), getUserAccountId, getUserPersonId, userIsAdmin(), offset))
    }

    override def getNewAssignmentReward: java.lang.Integer = {
        onCall("getNewAssignmentReward")
        assertUserAccess()

        return PersonS.getNewAssignmentReward(getUserPersonId)
    }

    override def createAssignment(params: AssignmentCreationParamsD): AssignmentVD = {
        onCall("createAssignment", params)
        assertUserAccess()

        val tokenizer = new Tokenizer(getThreadLocalRequest.getSession, "createAssignment", false)
        val result = AssignmentS.createAssignment(getUserAccountId, getUserPersonId, getCloak != null, params, tokenizer, userIsAdmin())
        if (result != null)
            tokenizer.disposeTokens()
        return result
    }

    override def deleteAssignment(assignmentIdStr: String): Boolean = {
        onCall("deleteAssignment", assignmentIdStr)
        assertUserAccess()

        return AssignmentS.deleteAssignment(UUID.fromString(assignmentIdStr), getUserPersonId, userIsAdmin())
    }

    override def deleteSkill(skillIdStr: String): Boolean = {
        onCall("deleteSkill", skillIdStr)
        assertAdminAccess()

        return SkillS.deleteSkill(UUID.fromString(skillIdStr), getUserPersonId, userIsAdmin())
    }

    override def speedupAssignment(assignmentIdStr: String, days: Int): AssignmentVD = {
        onCall("speedupAssignment", assignmentIdStr, days)
        assertAdminAccess()

        AssignmentS.speedupAssignment(UUID.fromString(assignmentIdStr), days, userIsAdmin())
        return AssignmentS.getAssignment(UUID.fromString(assignmentIdStr), getUserAccountId, getUserPersonId, userIsAdmin())
    }

    override def createActivity(assignmentIdStr: String, contentBlocks: java.util.ArrayList[ContentBlockD]): ActivityVD = {
        onCall("createActivity", assignmentIdStr, contentBlocks)
        assertUserAccess()

        val uploadTokenizer = new Tokenizer(getThreadLocalRequest.getSession, "upload", false)
        val result = ActivityS.createActivity(UUID.fromString(assignmentIdStr), contentBlocks, getUserAccountId, getUserPersonId, getCloak != null, userIsAdmin(), uploadTokenizer)
        uploadTokenizer.disposeTokens()
        return result
    }

    override def deleteActivity(activityIdStr: String): ActivityDeletedVD = {
        onCall("deleteActivity", activityIdStr)
        assertUserAccess()

        return ActivityS.deleteActivity(UUID.fromString(activityIdStr), getUserPersonId, userIsAdmin())
    }

    override def rateActivity(activityIdStr: String, rating: Int): ActivityD = {
        onCall("rateActivity", activityIdStr, rating)
        assertUserAccess()

        return ActivityS.rate(UUID.fromString(activityIdStr), rating, getUserPersonId, userIsAdmin())
    }

    override def suggestExistingSkills(title: String): ArrayList[SkillD] = {
        onCall("suggestExistingSkills", title)
        assertUserAccess()

        return asArrayList(SkillS.suggestExistingSkills(title, getUserPersonId, userIsAdmin()))
    }

    override def suggestSkillPictures(skillTitle: String): ArrayList[TokenizedResourceD] = {
        onCall("suggestSkillPictures", skillTitle)
        assertUserAccess()

        val tokenizer = new Tokenizer(getThreadLocalRequest.getSession, "createAssignment", false)
        return asArrayList(FlickrS.suggestSkillPictures(skillTitle, tokenizer))
    }

    override def getYoutubeVideos: java.util.ArrayList[VideoD] = {
        onCall("getYoutubeVideos")
        assertUserAccess()

        return asArrayList(YoutubeS.getUserVideos(getUserAccountId))
    }

    override def getVideoUploadForm(assignmentIdStr: String): VideoUploadFormD = {
        onCall("getVideoUploadForm", assignmentIdStr)
        assertUserAccess()

        return YoutubeS.getVideoUploadForm(getUserAccountId, getUserPersonId, UUID.fromString(assignmentIdStr))
    }

    override def getVideo(videoId: String): VideoD = {
        onCall("getVideo", videoId)

        return YoutubeS.getVideo(getUserAccountId, videoId)
    }

    override def setAssignmentAbuseReport(assignmentIdStr: String, isAbuse: Boolean): AssignmentVD = {
        onCall("setAssignmentAbuseReport", assignmentIdStr, isAbuse)
        assertUserAccess()

        return AssignmentS.setAbuseReport(UUID.fromString(assignmentIdStr), isAbuse, getUserAccountId, getUserPersonId, userIsAdmin())
    }

    override def setActivityAbuseReport(activityIdStr: String, isAbuse: Boolean): ActivityD = {
        onCall("setActivityAbuseReport", activityIdStr, isAbuse)
        assertUserAccess()

        return ActivityS.setAbuseReport(UUID.fromString(activityIdStr), isAbuse, getUserPersonId, userIsAdmin())
    }

    override def setSkillAbuseReport(activityIdStr: String, isAbuse: Boolean): SkillD = {
        onCall("setSkillAbuseReport", activityIdStr, isAbuse)
        assertUserAccess()

        return SkillS.setAbuseReport(UUID.fromString(activityIdStr), isAbuse, getUserPersonId, userIsAdmin())
    }

    override def getAbusedAssignments: ArrayList[AssignmentVD] = {
        onCall("getAbusedAssignments")
        assertAdminAccess()

        return asArrayList(AssignmentS.getWithMostAbuseReports(getUserAccountId, getUserPersonId, userIsAdmin()))
    }

    override def getAbusedActivities: ArrayList[ActivityVD] = {
        onCall("getAbusedActivities")
        assertAdminAccess()

        return asArrayList(ActivityS.getWithMostAbuseReports(getUserPersonId, userIsAdmin()))
    }

    override def getAbusedSkills: ArrayList[SkillD] = {
        onCall("getAbusedSkills")
        assertAdminAccess()

        return asArrayList(SkillS.getWithMostAbuseReports(getUserPersonId, userIsAdmin()))
    }

    override def clearAssignmentAbuseReports(assignmentIdStr: String): AssignmentVD = {
        onCall("clearAssignmentAbuseReports", assignmentIdStr)
        assertAdminAccess()

        return AssignmentS.clearAbuseReports(UUID.fromString(assignmentIdStr), getUserAccountId, getUserPersonId, userIsAdmin())
    }

    override def clearActivityAbuseReports(activityIdStr: String): ActivityD = {
        onCall("clearActivityAbuseReports", activityIdStr)
        assertAdminAccess()

        return ActivityS.clearAbuseReports(UUID.fromString(activityIdStr), getUserPersonId, userIsAdmin())
    }

    override def clearSkillAbuseReports(activityIdStr: String): SkillD = {
        onCall("clearSkillAbuseReports", activityIdStr)
        assertAdminAccess()

        return SkillS.clearAbuseReports(UUID.fromString(activityIdStr), getUserPersonId, userIsAdmin())
    }

    override def search(searchTerm: String): SearchResultsD = {
        onCall("search", searchTerm)

        val max = 5
        val ids = Search.instance.queryIds(searchTerm, max + 1)
        val results = new SearchResultsD
        results.results = asArrayList(ids.take(max).map(it => {
            var item = new SearchResultD
            it._1 match {
                case SearchBeanTypes.PERSON => {
                    if (it._2 != UUID.fromString(Config.SYS_OBJ_ID)) {
                        item.person = PersonS.getPerson(it._2)
                    }
                    else {
                        // system user must not show up in search results
                        item = null
                    }
                }
                case SearchBeanTypes.ASSIGNMENT => {
                    item.assignment = AssignmentS.getAssignment(it._2, getUserAccountId, getUserPersonId, userIsAdmin())
                }
                case SearchBeanTypes.SKILL => {
                    item.skill = SkillS.getSkill(it._2, getUserPersonId, userIsAdmin())
                }
                case _ => {
                    item = null
                }
            }
            item
        }).filter(_ != null))
        results.hasMore = ids.size > max
        return results
    }

    override def getSkill(skillIdStr: String): SkillD = {
        onCall("getSkill", skillIdStr)

        return SkillS.getSkill(UUID.fromString(skillIdStr), getUserPersonId, userIsAdmin())
    }

    override def getAssignmentsBySkill(skillIdStr: String, offset: Int): java.util.ArrayList[AssignmentVD] = {
        onCall("getAssignmentsBySkill", skillIdStr, offset)

        return asArrayList(AssignmentS.getAssignmentsBySkill(UUID.fromString(skillIdStr), offset, getUserAccountId, getUserPersonId, userIsAdmin()))
    }

    override def boostAssignment(assignmentId: String, boost: Int): AssignmentVD = {
        onCall("boostAssignment", assignmentId, boost)
        assertUserAccess()

        return AssignmentS.boostAssignment(UUID.fromString(assignmentId), boost, getUserAccountId, getUserPersonId, userIsAdmin())
    }

    override def getRankings: RankingsD = {
        onCall("getRankings")

        return PersonS.getRankings(getUserPersonId)
    }

    override def setCloak(accountIdStr: String) {
        onCall("setCloak", accountIdStr)
        assertAdminAccess()

        val accountId = if (accountIdStr != null && !accountIdStr.isEmpty) UUID.fromString(accountIdStr) else null
        logger.info("Account " + getUserAccountId + " cloaks as " + accountId)
        AccountU.setCloak(getUserAccountId, accountId)
    }

    override def getSkillTagCloud: ArrayList[TagD] = {
        onCall("getSkillTagCloud")

        return asArrayList(SkillS.getSkillTagCloud)
    }

    override def createTestPerson(name: String, picture: String): String = {
        onCall("createTestPerson", name, picture)

        return PersonS.createTestPerson(name, UUID.fromString(picture)).id.toString
    }

    override def announcementClosed(showTime: Date) {
        onCall("announcementClosed", showTime)

        if (showTime != null) {
            val accountId = getUserAccountId
            if (accountId != null) {
                AccountU.setLastAnnouncement(accountId, showTime)
            }
        }
    }

    override def createAnnouncement(text: String, showInSeconds: Int, hideInSeconds: java.lang.Integer, isMaintenance: Boolean) {
        onCall("createAnnouncement", text, showInSeconds, hideInSeconds, isMaintenance)
        assertAdminAccess()

        if (text == null || text.isEmpty || showInSeconds < 0 || (hideInSeconds != null && hideInSeconds <= 0)) {
            logger.warn("Wrong parameters for new announcement")
            return
        }

        val announcement = new AnnouncementM
        announcement.showTime = TimeHelper.now.plusSeconds(showInSeconds)
        announcement.hideTime = if(hideInSeconds != null)TimeHelper.now.plusSeconds(hideInSeconds) else null
        announcement.text = text
        announcement.maintenance = isMaintenance
        Database.save(announcement)
    }

    override def getPersonAdminInfos: ArrayList[PersonAdminInfoD] = {
        onCall("getPersonAdminInfos")
        assertAdminAccess()

        val result = mutable.Buffer[PersonAdminInfoD]()
        Database.forEach(classOf[PersonM]) { person => {
            val account = Database.load(classOf[AccountM], person.account)
            if(account != null) {
                result += Presenter.presentAdminInfo(person, account)
            }
        }}

        return asArrayList(result.sortBy(-_.person.joinDate.getTime))
    }

    override def setEmailAddress(email: String, oldPasswordClear: String, newPasswordClear: String): EmailSetResult = {
        onCall("setEmailAddress", email)
        assertUserAccess()

        return AuthS.setEmailAddress(getUserAccountId, email, oldPasswordClear, newPasswordClear, getThreadLocalRequest.getSession)
    }

    override def subscribeToAllAssignments() {
        onCall("subscribeToAllAssignments")
        assertAdminAccess()

        SubscriptionS.subscribeToAll(getUserAccountId)
    }

    override def subscribe(assignmentIdStr: String) {
        onCall("subscribe", assignmentIdStr)
        assertUserAccess()

        SubscriptionS.subscribe(getUserAccountId, UUID.fromString(assignmentIdStr))
    }

    override def unsubscribe(assignmentIdStr: String) {
        onCall("unsubscribe", assignmentIdStr)
        assertUserAccess()

        SubscriptionS.unsubscribe(getUserAccountId, UUID.fromString(assignmentIdStr))
    }

    override def setPreferences(preferences: PreferencesD) {
        onCall("setPreferences", preferences)
        assertUserAccess()

        val accountId: UUID = getUserAccountId
        logger.info("Changing preferences of account " + accountId)
        AccountU.setPreferences(getUserAccountId, Presenter.unpresent(preferences))
    }

    override def getPreferences: PreferencesD = {
        onCall("getPreferences")
        assertUserAccess()

        val account = Database.load(classOf[AccountM], getUserAccountId)
        if(account == null || account.preferences == null) {
            logger.warn("Cannot find preferences of account " + getUserAccountId)
            return null
        }

        return Presenter.present(account.preferences)
    }

    override def sendNotifications() {
        onCall("sendNotifications")
        assertAdminAccess()

        // add dummy notification for admin
        NotificationS.createActivityRewardNotification(
            getUserAccountId, 0,
            Database.load(classOf[AssignmentM], UUID.fromString(Config.FOUNDER_ASSIGNMENT_ID)))

        NotificationS.sendEmails()
    }
}


