package com.c5000.mastery.database.search

import com.c5000.mastery.database.models.UniqueIdModelBase

trait TSearchBeanManager {

    def modelClasses: Iterable[Class[_ <: UniqueIdModelBase]]

    def createBean[Model <: UniqueIdModelBase](model: Model): SearchBean

}
