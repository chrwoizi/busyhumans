package com.c5000.mastery.backend

import _root_.java.util
import _root_.javax.servlet.http.HttpSession
import com.c5000.mastery.shared.data.base.{TokenizedResourceD, ResourceD}
import collection.mutable

/**
 * Issues tokens and recalls them later to verify a resource that has been bounced by the client
 */
class Tokenizer(session: HttpSession, tokenGroup: String, clearTokenGroup: Boolean) {

    if (clearTokenGroup)
        disposeTokens()

    def tokenize(resource: AnyRef, token: String) = {

        var tokens = session.getAttribute(tokenGroup).asInstanceOf[mutable.HashMap[String, AnyRef]]
        if (tokens == null) {
            tokens = mutable.HashMap[String, AnyRef]()
            session.setAttribute(tokenGroup, tokens)
        }

        tokens.put(token, resource)
    }

    def detokenize(token: String): AnyRef = {
        val tokens = session.getAttribute(tokenGroup).asInstanceOf[mutable.HashMap[String, AnyRef]]
        if (tokens != null) {
            val result = tokens.get(token)
            if (result.isDefined)
                return result.get
        }
        return null
    }

    def tokenize(resource: ResourceD): TokenizedResourceD = {

        val result = new TokenizedResourceD
        result.resource = resource
        result.token = util.UUID.randomUUID().toString

        tokenize(result.resource, result.token)

        return result
    }

    def detokenize(resource: TokenizedResourceD): Boolean = {
        val tmp = detokenize(resource.token).asInstanceOf[ResourceD]
        if (tmp != null) {
            resource.resource = tmp
            return true
        }
        return false
    }

    def disposeTokens() {
        session.removeAttribute(tokenGroup)
    }

}
