package com.c5000.mastery.backend.services

import _root_.java.util.UUID
import com.c5000.mastery.database.models.ResourceM
import com.c5000.mastery.shared.data.base.{TokenizedResourceD, ResourceD}

object ResourcePresenter {

    def present(resource: ResourceM, token: UUID, isImage: Boolean): TokenizedResourceD = {
        val result = new TokenizedResourceD
        if (token != null)
            result.token = token.toString
        result.resource = present(resource)
        return result
    }

    def present(resource: ResourceM): ResourceD = {
        var result = new ResourceD
        result.resource = resource.resource
        result.small = resource.small
        result.medium = resource.medium
        result.large = resource.large
        result.hires = resource.hires
        result.authorName = resource.authorName
        result.authorUrl = resource.authorUrl
        result.license = resource.license
        return result
    }

    def unpresent(resource: ResourceD): ResourceM = {
        val result = new ResourceM
        result.resource = resource.resource
        result.small = resource.small
        result.medium = resource.medium
        result.large = resource.large
        result.hires = resource.hires
        result.authorName = resource.authorName
        result.authorUrl = resource.authorUrl
        result.license = resource.license
        return result
    }

}
