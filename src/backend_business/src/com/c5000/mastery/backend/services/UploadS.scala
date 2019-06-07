package com.c5000.mastery.backend.services

import com.c5000.mastery.backend.{Tokenizer, Initializer}
import com.c5000.mastery.database.FileActions
import com.c5000.mastery.shared.data.base.{ResourceD, TokenizedResourceD, LicenseTypes}
import com.c5000.mastery.shared.FileParts
import org.apache.commons.fileupload.FileItem
import javax.servlet.http.HttpServletRequest
import java.util.UUID
import gwtupload.server.{UploadServlet, UploadAction}
import scala.collection.JavaConversions._
import gwtupload.server.exceptions.UploadCanceledException

class UploadS extends UploadAction with HasServiceLogger {

    override def init() {
        Initializer.init()
    }

    override def executeAction(request: HttpServletRequest, sessionFiles: java.util.List[FileItem]): String = {
        try {
            var allowed = false
            val isForRegister = request.getParameter("register") == "true"

            if (isForRegister) {
                allowed = true
                val authData = AuthS.getAuthData(request.getSession)
                if (authData != null) {
                    if (authData.uploadedRegistrationPictureId != null) {
                        logger.info("About to receive new profile picture for anon user. Deleting old picture with id " + authData.uploadedRegistrationPictureId + ".")
                        FileActions.deleteImage(authData.uploadedRegistrationPictureId)
                        authData.uploadedRegistrationPictureId = null
                    }
                }
            }

            val person = PersonS.loadPerson(BaseS.getUserPersonId(request.getSession))
            if (person != null) {
                allowed = true
            }

            if (!allowed) {
                return ""
            }

            if (!sessionFiles.isEmpty) {
                val item = sessionFiles.head
                if (!item.isFormField) {
                    val id = UUID.randomUUID()
                    logger.info("Received file " + item.getName + " as " + id + ". Checking mime type...")
                    if (FileActions.isImage(item.getContentType)) {
                        logger.info("Received file " + id + " is an image. Processing...")

                        FileActions.saveImage(id, item.getInputStream)
                        logger.info("Image processing done. Saved to db as " + id + ".")

                        if (isForRegister) {
                            val authData = AuthS.getAuthData(request.getSession)
                            if (authData != null) {
                                authData.uploadedRegistrationPictureId = id
                            }
                        }

                        val resource = new TokenizedResourceD
                        resource.token = id.toString
                        val image = new ResourceD
                        image.resource = "mastery/res/dynamic/" + id.toString
                        image.small = image.resource + "/" + FileParts.SMALL
                        image.medium = image.resource + "/" + FileParts.MEDIUM
                        image.large = image.resource + "/" + FileParts.LARGE
                        image.hires = image.resource + "/" + FileParts.HIRES
                        resource.resource = image
                        if (person != null) {
                            resource.resource.authorName = person.name
                            resource.resource.authorUrl = "#person=" + person.id
                        }
                        resource.resource.license = LicenseTypes.CC_BY_30
                        val tokenizer = new Tokenizer(request.getSession, "upload", false)
                        tokenizer.tokenize(resource.resource, resource.token)

                        return id.toString
                    }
                    else {
                        logger.warn("Unknown mime type " + item.getContentType + " for received file " + id + ". Discarding.")
                        throw new UploadCanceledException
                    }
                }
            }
            return ""
        }
        finally {
            UploadServlet.removeSessionFileItems(request)
        }
    }
}