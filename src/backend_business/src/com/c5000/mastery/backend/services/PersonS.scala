package com.c5000.mastery.backend.services

import _root_.java.lang.String
import _root_.java.util.UUID
import com.c5000.mastery.backend.anon.AnonUser
import com.c5000.mastery.backend.facebook.Facebook
import com.c5000.mastery.backend.TimeHelper
import com.c5000.mastery.backend.twitter.TwitterUser
import com.c5000.mastery.database.Database
import com.c5000.mastery.database.models._
import com.c5000.mastery.database.queries.{PersonQ, AssignmentQ, CityQ}
import com.c5000.mastery.database.search.Search
import com.c5000.mastery.database.updates.{AccountU, PersonU}
import com.c5000.mastery.shared.data.auth.CloakD
import com.c5000.mastery.shared.data.base.{ActivityD, LicenseTypes, PersonD}
import com.c5000.mastery.shared.data.combined.{RankingD, RankingsD}
import com.restfb.types.User
import org.joda.time.{DateMidnight, DateTimeConstants, DateTime}
import util.Random

object PersonS extends HasServiceLogger {

    def getNewAssignmentReward(personId: UUID): java.lang.Integer = {
        if (personId == null)
            return null

        val person = Database.load(classOf[PersonM], personId)
        if (person == null) {
            logger.warn("Could not find person " + personId + " to get initial assignment reward amount.")
            return null
        }

        return Balancing.getInitialAssignmentReward(Balancing.getLevel(person.xp))
    }

    def lastRewardXpQuotaRechargeTime: DateTime = {
        return DateMidnight.now().withDayOfWeek(DateTimeConstants.MONDAY).toDateTime
    }

    def loadPerson(personId: UUID): PersonM = {
        if (personId == null)
            return null

        val person = Database.load(classOf[PersonM], personId)
        if (person == null) {
            return null
        }

        return person
    }

    def getPerson(personId: UUID): PersonD = {
        val person = loadPerson(personId)
        if (person == null) {
            logger.warn("Person " + personId + " not found.")
            return null
        }

        logger.trace("Person " + person.id + " found as '" + person.name + "'.")

        val createdAssignments = AssignmentQ.getCreatedCount(person.id)
        val completedAssignments = AssignmentQ.getCompletedCount(person.id)

        return Presenter.present(person, createdAssignments, completedAssignments)
    }

    def createPictureResource(small: String, medium: String, large: String, hires: String, person: PersonM, license: Int): ResourceM = {
        val result = new ResourceM
        result.resource = large
        result.small = small
        result.medium = medium
        result.large = large
        result.hires = hires
        result.authorName = person.name
        result.authorUrl = "#person=" + person.id
        result.license = license
        return result
    }

    def updatePerson(account: AccountM, user: Object): PersonM = {
        if (account == null || user == null || !(user.isInstanceOf[User] || user.isInstanceOf[TwitterUser] || user.isInstanceOf[AnonUser])) {
            logger.error("illegal arguments for PersonS.updatePerson")
            return null
        }
        logger.trace("updating person for account '" + account.id + "'.")

        // get person
        var person = loadPerson(account.person)
        val isNew = person == null
        if (isNew) {
            logger.info("no person found for account '" + account.id + "'. creating new.")
            person = createPerson(account)
        }

        if(user.isInstanceOf[User]) {
            popuplate(person, user.asInstanceOf[User])
        }
        else if(user.isInstanceOf[TwitterUser]) {
            popuplate(person, user.asInstanceOf[TwitterUser])
        }
        else if(user.isInstanceOf[AnonUser]) {
            popuplate(person, user.asInstanceOf[AnonUser])
        }

        if(person.picture.authorName == null) {
            person.picture.authorName = person.name
            person.picture.authorUrl = "#person=" + person.id.toString
        }

        logger.info("saving person '" + person.id + "'.")
        if (isNew)
            Database.save(person)
        else
            PersonU.updatePersonDetails(person)
        Search.instance.update(List(person))

        return person
    }

    def popuplate(person: PersonM, user: User) {

        // base data
        person.name = getName(user.getFirstName, user.getMiddleName, user.getLastName)
        person.picture = createPictureResource(
            Facebook.getUserPictureSmall(user.getId),
            Facebook.getUserPictureMedium(user.getId),
            Facebook.getUserPictureLarge(user.getId),
            Facebook.getUserPictureLarge(user.getId),
            person,
            LicenseTypes.FACEBOOK)
        val birthday = user.getBirthdayAsDate
        person.birthday = if (birthday != null) new DateTime(birthday) else null
        person.gender = user.getGender
        person.locale = user.getLocale
        person.timezone = user.getTimezone
        if (user.getLocation != null &&
            user.getLocation.getId != null &&
            !user.getLocation.getId.isEmpty &&
            user.getLocation.getName != null &&
            !user.getLocation.getName.isEmpty) {
            person.city = getCityId(user.getLocation.getId, user.getLocation.getName)
        }

        // contacts
        if (user.getEmail != null && !user.getEmail.isEmpty) {
            addContact(person, "email", user.getEmail)
        }
        if (user.getLink != null && !user.getLink.isEmpty) {
            addContact(person, "facebook", user.getLink)
        }
    }

    def popuplate(person: PersonM, user: TwitterUser) {

        // base data
        person.name = user.name
        val picture = user.profile_image_url
        person.picture = createPictureResource(
            picture,
            picture.replaceFirst("_normal\\.jpg$", "_reasonably_small.jpg"),
            picture.replaceFirst("_normal\\.jpg$", "_reasonably_small.jpg"),
            picture.replaceFirst("_normal\\.jpg$", ".jpg"),
            person,
            LicenseTypes.TWITTER)
        if (user.location != null &&
            user.location != null &&
            !user.location.isEmpty) {
            person.city = getCityId(user.location)
        }

        // contacts
        if (user.url != null && !user.url.isEmpty) {
            addContact(person, "twitter", user.url)
        }
    }

    def popuplate(person: PersonM, user: AnonUser) {

        // base data
        person.name = user.name
        person.picture = user.picture
    }

    /**
     * concatenates first, middle and last name
     */
    private def getName(first: String, middle: String, last: String): String = {
        val _first = if (first != null) first.trim() else ""
        val _middle = if (middle != null) middle.trim() else ""
        val _last = if (last != null) last.trim() else ""
        var result: String = _first
        if (!_middle.isEmpty) {
            if (!result.isEmpty)
                result += " "
            result += _middle
        }
        if (!_last.isEmpty) {
            if (!result.isEmpty)
                result += " "
            result += _last
        }
        return result
    }

    private def createPerson(account: AccountM): PersonM = {

        val person = new PersonM()
        person.account = account.id
        person.registrationTimestamp = TimeHelper.now

        logger.info("assigning person '" + person.id + "' to account '" + account.id + "'.")
        AccountU.setPerson(account, person.id)

        return person
    }

    private def addContact(person: PersonM, typ: String, value: String) {

        val existingContactOpt = person.contacts.validItems.find(_.typ == typ)
        var contact: ContactM = null
        if (existingContactOpt.isDefined) {
            logger.info("contact '" + typ + "' of person '" + person.id + "' already exists. updating.")
            contact = existingContactOpt.get
        }
        else {
            logger.info("contact '" + typ + "' of person '" + person.id + "' does not exist. creating new.")
            contact = new ContactM()
            contact.typ = typ
            person.contacts.add(contact)
        }

        contact.timestamp = TimeHelper.now
        contact.value = value
    }

    private def getCityId(name: String): UUID = {
        logger.trace("looking for city '" + name + "'.")

        val existingCityId = CityQ.getByName(name)
        var cityId: UUID = null
        if (existingCityId != null) {
            logger.trace("city '" + name + "' exists.")
            cityId = existingCityId
        }
        else {
            logger.info("city '" + name + "' does not exist. creating new.")
            val city = new CityM()
            city.name = name
            Database.save(city)
            cityId = city.id
        }
        return cityId
    }

    private def getCityId(facebookId: String, name: String): UUID = {
        logger.trace("looking for city '" + facebookId + "'.")

        val existingCityId = CityQ.getByFacebookId(facebookId)
        var cityId: UUID = null
        if (existingCityId != null) {
            logger.trace("city '" + facebookId + "' exists.")
            cityId = existingCityId
        }
        else {
            logger.info("city '" + facebookId + "' does not exist. creating new.")
            val city = new CityM()
            city.facebookId = facebookId
            city.name = name
            Database.save(city)
            cityId = city.id
        }
        return cityId
    }

    def getRankings(userPersonId: UUID): RankingsD = {

        val personIds = PersonQ.getByRank(5)
        logger.trace("Found " + personIds.size + " persons in rankings.")

        val result = new RankingsD
        var rank = 0
        var lastXp = Int.MaxValue
        result.rankings = new java.util.ArrayList[RankingD]
        personIds.map(Database.load(classOf[PersonM], _)).filter(_ != null).foreach(person => {
            val ranking = new RankingD
            if (person.xp < lastXp)
                rank += 1
            lastXp = person.xp
            ranking.rank = rank
            ranking.person = Presenter.present(person)
            result.rankings.add(ranking)
        })
        if(userPersonId != null && personIds.find(_ == userPersonId).isEmpty) {
            val person = Database.load(classOf[PersonM], userPersonId)
            if(person != null && person.xp > 0) {
                val ranking = new RankingD
                ranking.rank = PersonQ.getRank(person.xp) + 1
                ranking.person = Presenter.present(person)
                result.rankings.add(ranking)
            }
        }
        return result
    }

    def createTestPerson(name: String, picture: UUID): PersonM = {
        val person = new PersonM
        person.account = null
        person.registrationTimestamp = TimeHelper.now.minusHours(new Random().nextInt(72))
        person.locale = "en_US"
        person.timezone = 2
        person.name = name
        person.picture = new ResourceM
        person.picture.authorName = name
        person.picture.authorUrl = "#person=" + person.id.toString
        person.picture.license = LicenseTypes.FACEBOOK
        person.picture.resource = "mastery/res/dynamic/" + picture.toString
        person.picture.small = person.picture.resource + "/small"
        person.picture.medium = person.picture.resource + "/medium"
        person.picture.large = person.picture.resource + "/large"
        person.picture.hires = person.picture.resource + "/hires"
        Database.save(person)
        Search.instance.update(List(person))
        return person
    }

    def getCloaks: Iterable[CloakD] = {
        return PersonQ.getCloaks.map(Database.load(classOf[PersonM], _)).filter(_ != null).map(Presenter.presentCloak(_))
    }

}
