package com.c5000.mastery.backend.twitter

import com.c5000.mastery.backend.services.{ServiceLogger, HasServiceLogger}
import com.c5000.mastery.database.models.CredentialM
import com.c5000.mastery.shared.PublicTwitterConfig
import com.google.gson.{Gson, GsonBuilder}
import org.scribe.builder.api.TwitterApi
import org.scribe.builder.ServiceBuilder
import org.scribe.model.{Token, Verb, OAuthRequest}

object TwitterS extends HasServiceLogger{

    val service = new ServiceBuilder()
        .provider(classOf[TwitterApi.SSL])
        .apiKey(PrivateTwitterConfig.APP_KEY)
        .apiSecret(PrivateTwitterConfig.APP_SECRET)
        .callback(PublicTwitterConfig.OAUTH_REDIRECT)
        .build()

    private val gson = {
        val gsonBuilder = new GsonBuilder()
        gsonBuilder.create
    }

    def verifyCredentials(accessToken: Token): TwitterUser = {
        val request = new OAuthRequest(Verb.GET, "http://api.twitter.com/1/account/verify_credentials.json")
        service.signRequest(accessToken, request)
        val response = request.send()
        return gson.fromJson(response.getBody, classOf[TwitterUser])
    }

    def publish(logger: ServiceLogger, credential: CredentialM, text: String) {
        val accessToken = new Token(credential.accessToken, credential.accessTokenSecret)
        val request = new OAuthRequest(Verb.POST, "http://api.twitter.com/1/statuses/update.json")
        request.addBodyParameter("status", text)
        service.signRequest(accessToken, request)
        request.send()
    }

}
