package com.c5000.mastery.backend

import _root_.java.net.{HttpURLConnection, URL}
import _root_.javax.servlet.{ServletOutputStream, WriteListener}
import com.c5000.mastery.shared.Config

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpServletResponseWrapper
import _root_.java.io._
import org.apache.commons.io.IOUtils
import services.HasServiceLogger

object JspRenderer extends HasServiceLogger {

    def render(url: String): String = {
        try {
            val conn = new URL(Config.BASE_URL + url).openConnection().asInstanceOf[HttpURLConnection]
            conn.setRequestMethod("GET")
            val is = conn.getInputStream
            try {
                return IOUtils.toString(is, "UTF-8")
            }
            finally {
                conn.getInputStream.close()
                conn.disconnect()
            }
        }
        catch {
            case ex: Throwable => {
                logger.error("Error while fetching '" + url + "' internally.", ex)
                return null
            }
        }
    }

    def render(url: String, request: HttpServletRequest, response: HttpServletResponse): String = {

        val customResponse = new CharArrayWriterResponse(response)
        request.getRequestDispatcher(url).forward(request, customResponse)
        return customResponse.getOutput
    }

    private class CharArrayWriterResponse(response: HttpServletResponse) extends HttpServletResponseWrapper(response) {

        private final val charArray = new CharArrayWriter

        override def getWriter = new PrintWriter(charArray)

        override def getOutputStream = new ServletOutputStream {

            def write(b: Int) {}

            override def isReady: Boolean = true

            override def setWriteListener(writeListener: WriteListener): Unit = null
        }

        def getOutput = charArray.toString
    }

}