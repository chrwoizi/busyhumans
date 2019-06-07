package com.c5000.mastery.database.search

import com.c5000.mastery.database.models.{AssignmentM, SkillM, PersonM, UniqueIdModelBase}

class SearchBeanManager extends TSearchBeanManager {

    def modelClasses = List(
        classOf[PersonM],
        classOf[SkillM],
        classOf[AssignmentM]
    )

    def createBean[Model <: UniqueIdModelBase](model: Model): SearchBean = {
        val result = new SearchBean()
        result.id = model.id.toString
        if (model.isInstanceOf[PersonM]) {
            result.typ = SearchBeanTypes.PERSON
            result.value = model.asInstanceOf[PersonM].name
        }
        else if (model.isInstanceOf[SkillM]) {
            result.typ = SearchBeanTypes.SKILL
            result.value = model.asInstanceOf[SkillM].title
        }
        else if (model.isInstanceOf[AssignmentM]) {
            result.typ = SearchBeanTypes.ASSIGNMENT
            result.value = model.asInstanceOf[AssignmentM].title
        }
        return result
    }

}
