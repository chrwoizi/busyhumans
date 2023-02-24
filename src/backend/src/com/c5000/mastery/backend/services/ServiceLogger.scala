package com.c5000.mastery.backend.services

import _root_.org.slf4j.Logger

object ServiceLogger {

    /**
     * text that will be added to any log output of getLogger(_)
     * must be set by the service on each service method call
     */
    var logHead: ThreadLocal[String] = new ThreadLocal[String]()

    /**
     * @param logger named logger, e.g. LoggerFactory.get(getClass)
     * @return a logger that includes the logHead of the calling thread
     */
    def getLogger(logger: Logger): ServiceLogger = {
        var header = logHead.get()
        if(header == null)
            header = ""
        return new ServiceLogger(logger, header)
    }
}

class ServiceLogger(var logger: Logger, var header: String) {

    def trace(text: String) {
        logger.trace(header + text)
    }

    def debug(text: String) {
        logger.debug(header + text)
    }

    def info(text: String) {
        logger.info(header + text)
    }

    def warn(text: String) {
        logger.warn(header + text)
    }

    def error(text: String) {
        logger.error(header + text)
    }

    def error(text: String, t: Throwable) {
        logger.debug(header + text)
    }
}
