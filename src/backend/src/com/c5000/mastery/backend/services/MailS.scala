package com.c5000.mastery.backend.services

import _root_.java.util
import util.{UUID, Properties}
import _root_.javax.mail.internet.{InternetAddress, MimeMessage}
import _root_.javax.mail._
import com.c5000.mastery.backend.JspRenderer

object MailS extends HasServiceLogger {

    private class SMTPAuthenticator extends javax.mail.Authenticator {
        override def getPasswordAuthentication: PasswordAuthentication = {
            val username = System.getenv("EMAIL_USER")
            val password = System.getenv("EMAIL_PASSWORD")
            return new PasswordAuthentication(username, password)
        }
    }

    def send(address: String, subject: String, html: String): Boolean = {
        val props = new Properties()
        props.put("mail.smtp.host", System.getenv("EMAIL_HOST"))
        props.put("mail.smtp.port", System.getenv("EMAIL_PORT"))
        props.put("mail.smtp.ssl.trust", System.getenv("EMAIL_SSL_TRUST"))
        props.put("mail.smtp.starttls.enable", System.getenv("EMAIL_STARTTLS"))
        props.put("mail.smtp.auth", System.getenv("EMAIL_AUTH"))
        try {
            val auth = new SMTPAuthenticator()
            val session = Session.getInstance(props, auth)

            val message = new MimeMessage(session)
            message.setFrom(new InternetAddress(System.getenv("EMAIL_ADDRESS"), System.getenv("EMAIL_NAME")))
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(address))
            message.setSubject(subject)
            message.setContent(html, "text/html; charset=UTF-8")
            Transport.send(message)
            return true
        }
        catch {
            case ex: MessagingException => {
                logger.error("Error while sending email to '" + address + "': " + ex)
            }
        }
        return false
    }
}
