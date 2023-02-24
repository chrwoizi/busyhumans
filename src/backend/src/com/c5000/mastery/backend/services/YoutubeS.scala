package com.c5000.mastery.backend.services

import _root_.java.net.URL
import _root_.java.util.UUID
import com.c5000.mastery.backend.auth.{GoogleServerCredential, GoogleAuthProvider}
import com.c5000.mastery.backend.EncodingHelper
import com.c5000.mastery.backend.google.{YtAccessControl, PrivateGoogleConfig}
import com.c5000.mastery.database.Database
import com.c5000.mastery.database.models.AssignmentM
import com.c5000.mastery.shared.Config
import com.c5000.mastery.shared.data.base.{VideoUploadFormD, VideoD}
import com.google.gdata.client.youtube.YouTubeService
import com.google.gdata.data.media.mediarss._
import com.google.gdata.data.youtube._
import scala.collection.JavaConverters._

object YoutubeS extends HasServiceLogger {

    val YOUTUBE_UPLOADS_URL = "http://gdata.youtube.com/feeds/api/users/default/uploads"

    def getUserVideos(accountId: UUID): Iterable[VideoD] = {
        if (accountId == null)
            return null

        val credential = new GoogleAuthProvider().loadApiCredential(accountId)
        if (credential == null) {
            logger.warn("Account '" + accountId + "' is not authorized with google.")
            return null
        }

        val service = new YouTubeService("Busy Humans", PrivateGoogleConfig.YOUTUBE_DEVELOPER_KEY)
        service.setOAuth2Credentials(credential)

        val videoFeed = service.getFeed(new URL(YOUTUBE_UPLOADS_URL), classOf[VideoFeed])
        return videoFeed.getEntries.asScala.map(item => {
            Presenter.present(item)
        })
    }

    def getVideoUploadForm(accountId: UUID, personId: UUID, assignmentId: UUID): VideoUploadFormD = {
        if (accountId == null || assignmentId == null)
            return null

        val assignment = Database.load(classOf[AssignmentM], assignmentId)
        if (assignment == null) {
            logger.warn("Could not load assignment '" + assignmentId + "' for video upload.")
            return null
        }

        val part = assignment.activities.validItems.find(part => part.person == personId)
        if (part == null) {
            logger.warn("Person '" + personId + "' cannot upload a video for assignment '" + assignmentId + "'.")
            return null
        }

        val description = Config.BASE_URL_GWT + "#assignment=" + assignment.id
        val keywords = assignment.title.split(' ').filter(tag => 3 <= tag.length && tag.length <= 25)

        return getVideoUploadForm(accountId, assignmentId, assignment.title, description, keywords)
    }

    private def getVideoUploadForm(accountId: UUID, assignmentId: UUID, title: String, description: String, keywords: Iterable[String]): VideoUploadFormD = {

        val credential = new GoogleAuthProvider().loadApiCredential(accountId)
        if (credential == null) {
            logger.warn("Account '" + accountId + "' is not authorized with google.")
            return null
        }

        val service = new YouTubeService("Busy Humans", PrivateGoogleConfig.YOUTUBE_DEVELOPER_KEY)
        service.setOAuth2Credentials(credential)

        val newEntry = new VideoEntry
        val mg = newEntry.getOrCreateMediaGroup
        mg.setTitle(new MediaTitle())
        mg.getTitle.setPlainTextContent(title)
        mg.addCategory(new MediaCategory(YouTubeNamespace.CATEGORY_SCHEME, "People"))
        mg.setKeywords(new MediaKeywords())
        keywords.foreach(mg.getKeywords.addKeyword(_))
        mg.setDescription(new MediaDescription())
        mg.getDescription.setPlainTextContent(description)
        mg.addCategory(new MediaCategory(YouTubeNamespace.DEVELOPER_TAG_SCHEME, EncodingHelper.encode64(assignmentId)))
        newEntry.setExtension(new YtAccessControl("list", "denied"))

        val uploadUrl = new URL("http://gdata.youtube.com/action/GetUploadToken")
        val token = service.getFormUploadToken(uploadUrl, newEntry)

        val result = new VideoUploadFormD
        result.url = token.getUrl
        result.token = token.getToken
        return result
    }

    def getVideo(accountId: UUID, videoId: String): VideoD = {
        if (accountId == null || videoId == null)
            return null

        val credential = new GoogleAuthProvider().loadApiCredential(accountId)
        if (credential == null) {
            logger.info("Account '" + accountId + "' is not authorized with google.")
            return Presenter.presentVideoById(videoId)
        }

        val service = new YouTubeService("Busy Humans", PrivateGoogleConfig.YOUTUBE_DEVELOPER_KEY)
        service.setOAuth2Credentials(credential)

        val videoFeed = service.getFeed(new URL(YOUTUBE_UPLOADS_URL), classOf[VideoFeed])
        val video = videoFeed.getEntries.asScala.find(item => videoId.equals(Presenter.getVideoId(item)))
        if (video.isEmpty) {
            logger.info("Could not find video '" + videoId + "' for account '" + accountId + "'")
            return Presenter.presentVideoById(videoId)
        }
        else {
            return Presenter.present(video.get)
        }
    }

}
