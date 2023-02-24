package com.c5000.mastery.backend.services

import com.aetrion.flickr.photos.licenses.License
import com.aetrion.flickr.photos.{Photo, SearchParameters}
import com.aetrion.flickr.{Flickr, REST}
import com.c5000.mastery.backend.Tokenizer
import com.c5000.mastery.shared.Config
import com.c5000.mastery.shared.data.base.{LicenseTypes, ResourceD, TokenizedResourceD}

import scala.collection.JavaConverters._
import collection.mutable


object FlickrS extends HasServiceLogger {

    private val flickrApiKey = "***REMOVED***"
    private val flickrSharedSecret = "***REMOVED***"

    private val LICENSE_NONE = "No known copyright restrictions"
    private val LICENSE_CC_BY = "Attribution License"
    private val LICENSE_CC_BY_SA = "Attribution-ShareAlike License"
    private val LICENSE_CC_BY_ND = "Attribution-NoDerivs License"

    private val SQUARE_IMAGE_SUFFIX_75 = "_s.jpg"
    private val SQUARE_IMAGE_SUFFIX_150 = "_q.jpg"

    def suggestSkillPictures(skillTitle: String, tokenizer: Tokenizer): Iterable[TokenizedResourceD] = {
        if(!Config.ENABLE_FLICKR) {
            val resource = new ResourceD
            resource.resource = "static/default-skill.png"
            resource.small = resource.resource
            resource.medium = resource.resource
            resource.large = resource.resource
            resource.authorName = "busyhumans.com"
            resource.authorUrl = "http://busyhumans.com"
            resource.license = 41
            return List(tokenizer.tokenize(resource))
        }

        try {
            val flickr = new Flickr(flickrApiKey, flickrSharedSecret, new REST())

            // get known licenses
            val licenses = flickr.getLicensesInterface.getInfo.asInstanceOf[java.util.Collection[License]]
            val licensesById = mutable.HashMap[String, License]()
            val licenseTypes = mutable.HashMap[License, Int]()
            licenses.asScala.foreach(license => {
                license.getName match {
                    case LICENSE_NONE =>
                        licensesById.put(license.getId, license)
                        licenseTypes.put(license, LicenseTypes.NONE)
                    case LICENSE_CC_BY =>
                        licensesById.put(license.getId, license)
                        licenseTypes.put(license, LicenseTypes.CC_BY_30)
                    case LICENSE_CC_BY_SA =>
                        licensesById.put(license.getId, license)
                        licenseTypes.put(license, LicenseTypes.CC_BY_SA_30)
                    case LICENSE_CC_BY_ND =>
                        licensesById.put(license.getId, license)
                        licenseTypes.put(license, LicenseTypes.CC_BY_ND_30)
                    case _ => {}
                }
            })

            // include all known licenses
            var licensesParam = ""
            licenseTypes.foreach(license => {
                if (!licensesParam.isEmpty) {
                    licensesParam += ","
                }
                licensesParam += license._1.getId
            })

            if (!licensesParam.isEmpty) {
                val params = new SearchParameters()
                params.setLicense(licensesParam)
                params.setText(skillTitle)
                params.setSort(SearchParameters.RELEVANCE)
                params.setMedia("photos")
                params.setExtrasLicense(true)
                params.setExtrasOwnerName(true)
                val perPage = 10
                val pageIndex = 0
                val photos = flickr.getPhotosInterface.search(params, perPage, pageIndex)
                return photos.asScala.map(_photo => {
                    val photo = _photo.asInstanceOf[Photo]
                    val result = new ResourceD
                    result.small = photo.getSmallSquareUrl
                    result.medium = photo.getSmallSquareUrl.replace(SQUARE_IMAGE_SUFFIX_75, SQUARE_IMAGE_SUFFIX_150)
                    result.large = photo.getMediumUrl
                    result.hires = if(photo.getOriginalSecret.length > 8) photo.getOriginalUrl else photo.getLargeUrl
                    result.resource = result.medium
                    result.authorName = photo.getOwner.getUsername
                    result.authorUrl = "http://www.flickr.com/people/" + photo.getOwner.getUsername.replace(" ", "")

                    result.license = LicenseTypes.UNKNOWN
                    val license = licensesById.get(photo.getLicense)
                    if (license.isDefined) {
                        val licenseType = licenseTypes.get(license.get)
                        if (licenseType.isDefined) {
                            result.license = licenseType.get
                        }
                    }
                    tokenizer.tokenize(result)
                })
            }
        }

        return List()
    }

}
