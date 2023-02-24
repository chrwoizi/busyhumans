package com.c5000.mastery.backend.services

import _root_.java.net.URLEncoder
import _root_.java.util.UUID
import _root_.javax.servlet.http.{HttpServletRequest, HttpServletResponse, HttpServlet}
import _root_.org.slf4j.LoggerFactory
import com.c5000.mastery.backend.Initializer
import com.c5000.mastery.shared.ImageHelper.Size
import com.c5000.mastery.shared.{PublicFacebookConfig, ImageHelper, Config}


class MetaS extends HttpServlet {

    val logger = LoggerFactory.getLogger(getClass)

    override def init() {
        Initializer.init()
    }

    override def doGet(request: HttpServletRequest, response: HttpServletResponse) {
        try {
            val queryString = request.getQueryString
            val assignmentToken = "token=assignment%3D"
            if (queryString != null && queryString.contains(assignmentToken)) {
                var token = queryString.substring(queryString.indexOf(assignmentToken) + assignmentToken.length())
                val nextParam = token.indexOf("&")
                if(nextParam >= 0)
                    token = token.substring(0, nextParam)
                val assignmentId = token
                val assignment = AssignmentS.getAssignment(UUID.fromString(assignmentId), null, null, userIsAdmin = false)

                val image = ImageHelper.getAbsoluteUrl(assignment.assignment.picture, Size.LARGE)

                request.setAttribute("title", assignment.assignment.title)
                request.setAttribute("description", assignment.assignment.description)
                request.setAttribute("og:title", assignment.assignment.title)
                request.setAttribute("og:description", assignment.assignment.description)
                request.setAttribute("og:type", PublicFacebookConfig.FACEBOOK_NAMESPACE + ":assignment")
                request.setAttribute("og:image", image)
                request.setAttribute("og:url", Config.META_URL + "?token=" + URLEncoder.encode("assignment=" + assignment.assignment.id, "UTF-8"))
                request.setAttribute("og:site_name", "BUSY HUMANS")
            }

            request.getRequestDispatcher("/meta.jsp").forward(request, response)
        }
        catch {
            case _: Throwable => {
                response.sendRedirect(Config.BASE_URL_GWT)
            }
        }
    }

}
