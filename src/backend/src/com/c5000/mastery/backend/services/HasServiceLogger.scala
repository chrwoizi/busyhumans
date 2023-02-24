package com.c5000.mastery.backend.services

import _root_.org.slf4j.LoggerFactory

trait HasServiceLogger {

    private val baseLogger = LoggerFactory.getLogger(getClass)
    protected def logger = ServiceLogger.getLogger(baseLogger)

}
