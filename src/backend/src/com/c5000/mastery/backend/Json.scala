package com.c5000.mastery.backend

import com.google.gson.Gson
import com.google.gson.GsonBuilder

object Json {
    def parse[T](clazz: Class[T], json: String): T = {
        val builder: GsonBuilder = new GsonBuilder
        val gson: Gson = builder.create
        return gson.fromJson(json, clazz).asInstanceOf[T]
    }
}