package com.c5000.mastery.backend.services

import _root_.java.io.{InputStream, FileInputStream, File}
import _root_.java.util.regex.Pattern
import _root_.java.util.UUID
import _root_.javax.servlet.http.{HttpServletRequest, HttpServletResponse, HttpServlet}
import _root_.org.slf4j.LoggerFactory
import com.c5000.mastery.backend.Initializer
import com.c5000.mastery.database.Database
import com.c5000.mastery.shared.Config


class ResourceS extends HttpServlet {

    val logger = LoggerFactory.getLogger(getClass)

    override def init() {
        Initializer.init()
    }

    def getType(path: String): String = {
        val slash = path.indexOf('/')
        if (slash == -1)
            return null
        return path.substring(0, slash)
    }

    def getFile(path: String): String = {
        val slash = path.indexOf('/')
        if (slash == -1)
            return null
        return path.substring(slash + 1)
    }

    override def doGet(request: HttpServletRequest, response: HttpServletResponse) {

        response.setHeader("Etag", Config.VERSION.toString)

        val ifNoneMatch = request.getHeader("If-None-Match")
        if (ifNoneMatch != null && headerContains(ifNoneMatch, Config.VERSION.toString)) {
            send304(request, response)
        }

        if (request.getPathInfo == null || request.getPathInfo.size < 1) {
            send400(request, response)
            return
        }

        val path = request.getPathInfo.substring(1)
        val requestType = getType(path)
        val file = getFile(path)

        if (requestType == null || file == null) {
            send400(request, response)
            return
        }

        val splitFile = file.split("/")

        // parse the resource id
        val idStr = if (splitFile.length == 2) splitFile(0) else file
        var id: UUID = null
        try {
            id = UUID.fromString(idStr)
        }
        catch {
            case _ => {}
        }
        if (id == null) {
            send400(request, response)
            return
        }

        // parse the resource part
        val part = if (splitFile.length == 2) splitFile(1) else null

        // parse the resource type
        requestType match {
            case "static" => {
                logger.trace(request.getRemoteAddr + ": Request for static resource file " + file + ".")
                sendFsFile(id, part, request, response)
            }
            case "dynamic" => {
                logger.trace(request.getRemoteAddr + ": Request for dynamic resource file " + id + "/" + part + ".")
                sendDbFile(id, part, request, response)
            }
            case _ => {
                send400(request, response)
                return
            }
        }
    }

    def headerContains(field: String, value: String): Boolean = {
        if (field.startsWith("\"")) {
            val regex = Pattern.compile("([^,\\s\\\"]+)")
            val matcher = regex.matcher(field)
            while (matcher.find()) {
                if (matcher.group(0) == value)
                    return true
            }
            return false
        } else if (field == "*") {
            return true
        } else {
            return field == value
        }
    }

    def sendFsFile(resourceId: UUID, part: String, request: HttpServletRequest, response: HttpServletResponse) {

        if (part.contains("..") || part.contains("/")) {
            send404(request, response)
            return
        }

        var filename: String = null
        if (part != null) {
            filename = resourceId + "." + part + ".png"
        }
        else {
            filename = resourceId + ".png"
        }

        val (fileStream, fileSize) = openFsFile("static/" + filename)
        if (fileStream == null) {
            send404(request, response)
            return
        }

        try {
            sendPng(fileStream, fileSize, response)
        }
        finally {
            fileStream.close()
        }
    }

    def openFsFile(path: String): (InputStream, Int) = {
        try {
            val file = new File(path)
            return (new FileInputStream(file), file.length.asInstanceOf[Int])
        }
        catch {
            case x: Throwable => {}
        }
        return (null, 0)
    }

    def sendDbFile(resourceId: UUID, part: String, request: HttpServletRequest, response: HttpServletResponse) {

        val fileOpt = Database.loadFile(resourceId, part)
        if (fileOpt.isEmpty) {
            send404(request, response)
            return
        }
        val file = fileOpt.get
        try {
            sendFile(file.stream, file.size, file.contentType, response)
        }
        finally {
            file.stream.close()
        }
    }

    def sendPng(fileStream: InputStream, size: Int, response: HttpServletResponse) {
        sendFile(fileStream, size, "image/png", response)
    }

    def sendFile(fileStream: InputStream, size: Int, contentType: String, response: HttpServletResponse) {
        response.setContentType(contentType)
        response.setContentLength(size)

        val os = response.getOutputStream

        val buf = new Array[Byte](1024)
        var count = fileStream.read(buf)
        while (count >= 0) {
            os.write(buf, 0, count)
            count = fileStream.read(buf)
        }
    }

    def send400(request: HttpServletRequest, response: HttpServletResponse) {
        logger.warn(request.getRemoteAddr + ": Invalid resource request: '" + request.getPathInfo + "'.")
        response.sendError(400)
    }

    def send404(request: HttpServletRequest, response: HttpServletResponse) {
        logger.warn(request.getRemoteAddr + ": Resource not found: '" + request.getPathInfo + "'.")
        response.sendError(404)
    }

    def send304(request: HttpServletRequest, response: HttpServletResponse) {
        logger.trace(request.getRemoteAddr + ": Resource already cached: '" + request.getPathInfo + "'.")
        response.sendError(304)
    }
}
