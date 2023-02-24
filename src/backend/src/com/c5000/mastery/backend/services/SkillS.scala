package com.c5000.mastery.backend.services

import _root_.java.util.UUID
import com.c5000.mastery.backend.{Tokenizer, TimeHelper}
import com.c5000.mastery.database.models._
import com.c5000.mastery.database.Database
import com.c5000.mastery.database.queries.{PersonQ, SkillQ, AssignmentQ}
import com.c5000.mastery.database.search.{SearchBeanTypes, Search}
import com.c5000.mastery.database.updates.{SkillU, BaseU}
import com.c5000.mastery.shared.Sanitizer
import com.c5000.mastery.shared.data.base._
import collection.mutable

object SkillS extends HasServiceLogger {

    def suggestExistingSkills(title: String, userPersonId: UUID, userIsAdmin: Boolean): Iterable[SkillD] = {
        if (title == null || title.isEmpty || userPersonId == null)
            return null

        val suggestions = Search.instance.queryIds(SearchBeanTypes.SKILL, title, 3)
        logger.trace("Found " + suggestions.size + " matching skills for title '" + title + "'.")
        val skills = suggestions.map(skillId => AbuseFilter.filter(Database.load(classOf[SkillM], skillId), userIsAdmin)).filter(_ != null)
        logger.trace("Loaded " + suggestions.size + " matching skills for title '" + title + "'.")
        return skills.map(Presenter.present(_, userPersonId))
    }

    def createSkill(author: UUID, title: String, description: SkillDescriptionD, pictureToken: String, tokenizer: Tokenizer): SkillM = {
        if (author == null || tokenizer == null) {
            logger.error("author or tokenizer not set")
            return null
        }

        val cleanTitle = Sanitizer.skillTitle(title)
        if (cleanTitle == null) {
            logger.warn("skill title is invalid")
            return null
        }

        if (SkillQ.existsWithTitle(cleanTitle)) {
            logger.warn("skill with title '" + cleanTitle + "' already exists.")
            return null
        }

        if (description == null) {
            logger.warn("no skill description")
            return null
        }
        description.description = Sanitizer.skillDescription(description.description)
        if (description.description == null) {
            logger.warn("invalid skill description")
            return null
        }

        if (pictureToken == null) {
            logger.warn("picture token missing")
            return null
        }

        val picture = tokenizer.detokenize(pictureToken).asInstanceOf[ResourceD]
        if (picture == null) {
            logger.warn("Picture token for new skill is unknown.")
            return null
        }

        val skill = new SkillM()
        skill.creationTimestamp = TimeHelper.now
        skill.author = author
        skill.title = cleanTitle
        skill.description = Presenter.unpresent(description)
        skill.picture = ResourcePresenter.unpresent(picture)

        logger.info("Creating new skill " + cleanTitle + " as " + skill.id + ".")
        Database.save(skill)
        Search.instance.update(List(skill))
        return skill
    }

    def getSkill(skillId: UUID, userPersonId: UUID, userIsAdmin: Boolean): SkillD = {
        if (skillId == null)
            return null

        logger.trace("Loading skill " + skillId + ".")

        val skill = AbuseFilter.filter(Database.load(classOf[SkillM], skillId), userIsAdmin)
        if (skill == null) {
            logger.warn("Skill " + skillId + " not found.")
            return null
        }

        return Presenter.present(skill, userPersonId)
    }

    def deleteSkill(skillId: UUID, userPersonId: UUID, userIsAdmin: Boolean): Boolean = {
        if (skillId == null || userPersonId == null)
            return false

        val skill = Database.load(classOf[SkillM], skillId)
        if (skill == null) {
            logger.warn("Skill " + skillId + " not found.")
            return false
        }

        val assignmentIds = AssignmentQ.getBySkill(skill.id, 0, 999999)
        logger.info("Deleting " + assignmentIds.size + " assignments while deleting skill " + skill.id + ".")
        assignmentIds.foreach(AssignmentS.deleteAssignment(_, userPersonId, userIsAdmin))

        val achievementIds = PersonQ.getAchievementsBySkill(skill.id, 999999)
        logger.info("Deleting " + achievementIds.size + " achievements while deleting skill " + skill.id + ".")
        achievementIds.foreach(AchievementS.deleteAchievement(_, userPersonId, userIsAdmin))

        logger.info("Deleting skill " + skillId + ".")
        BaseU.setDeleted(skill)
        Search.instance.update(List(skill))

        return true
    }

    def setAbuseReport(skillId: UUID, isAbuse: Boolean, userPersonId: UUID, userIsAdmin: Boolean): SkillD = {
        if (skillId == null || userPersonId == null)
            return null

        val skill = Database.load(classOf[SkillM], skillId)
        if (skill == null) {
            logger.warn("Skill " + skillId + " not found.")
            return null
        }

        // get existing report
        val reportOpt = skill.abuseReports.validItems.find(report => report.author == userPersonId)

        if (isAbuse) {
            if (reportOpt.isEmpty) {
                logger.info("Creating abuse report for skill " + skill.id + ".")
                val report = new AbuseReportM()
                report.author = userPersonId
                report.timestamp = TimeHelper.now
                SkillU.addAbuseReport(skill, report)
            }
        }
        else {
            if (reportOpt.isDefined) {
                logger.info("Deleting abuse report for skill " + skill.id + ".")
                SkillU.removeAbuseReport(skill, reportOpt.get)
            }
        }

        return Presenter.present(skill, userPersonId = userPersonId)
    }

    def clearAbuseReports(skillId: UUID, userPersonId: UUID, userIsAdmin: Boolean): SkillD = {
        if (skillId == null || userPersonId == null)
            return null

        val skill = Database.load(classOf[SkillM], skillId)
        if (skill == null) {
            logger.warn("Skill " + skillId + " not found.")
            return null
        }

        logger.info("Deleting all abuse reports for skill " + skill.id + ".")
        SkillU.removeAbuseReports(skill)

        return Presenter.present(skill, userPersonId = userPersonId)
    }

    def getWithMostAbuseReports(userPersonId: UUID, userIsAdmin: Boolean): Iterable[SkillD] = {
        val skillIds = SkillQ.getWithMostAbuseReports(100)
        logger.trace("Found " + skillIds.size + " skills with abuse reports.")
        return skillIds.map(it => getSkill(it, userPersonId, userIsAdmin)).filter(it => it != null)
    }

    def getSkillTagCloud: Iterable[TagD] = {
        val result = mutable.Buffer[TagD]()
        Database.forEach(classOf[SkillM]) {
            skill => {
                val tag = new TagD
                tag.label = skill.title
                tag.url = "#category=" + skill.id.toString
                tag.weight = AssignmentQ.getCountBySkill(skill.id)
                result += tag
            }
        }
        return result.sortBy(-_.weight).take(30).sortBy(_.label)
    }
}
