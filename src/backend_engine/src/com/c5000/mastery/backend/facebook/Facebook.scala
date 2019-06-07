package com.c5000.mastery.backend.facebook

import com.c5000.mastery.backend.services.ServiceLogger
import com.c5000.mastery.shared.{PublicFacebookConfig, Config}
import com.restfb.{Parameter, DefaultFacebookClient, FacebookClient}
import com.restfb.types.{FacebookType, Photo, User}

object Facebook {
    def getUser(accessToken: String): User = {
        val client: FacebookClient = new DefaultFacebookClient(accessToken)
        return client.fetchObject("me", classOf[User])
    }

    def getUserPictureSmall(username: String) = "https://graph.facebook.com/" + username + "/picture?type=small"

    def getUserPictureMedium(username: String) = "https://graph.facebook.com/" + username + "/picture?type=normal"

    def getUserPictureLarge(username: String) = "https://graph.facebook.com/" + username + "/picture?type=large"

    /**
     * @param picture picture url for the subject block
     * @param title text of the subject link
     * @param description gray text block further below
     */
    def publishAction(logger: ServiceLogger, accessToken: String,
                   action: String,
                   objectType: String,
                   objectUrl: String,
                   picture: String,
                   title: String,
                   description: String): FacebookType = {
        val client: FacebookClient = new DefaultFacebookClient(accessToken)
        val response: FacebookType = client.publish("me/" + PublicFacebookConfig.FACEBOOK_NAMESPACE + ":" + action, classOf[FacebookType],
            Parameter.`with`(objectType, objectUrl),
            Parameter.`with`("picture", picture),
            Parameter.`with`("name", title),
            Parameter.`with`("description", description)
        )
        return response
    }
}