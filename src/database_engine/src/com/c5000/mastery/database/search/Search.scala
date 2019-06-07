package com.c5000.mastery.database.search

import com.c5000.mastery.database.models._
import base.SearchBase
import java.util
import com.c5000.mastery.database.Database
import collection.mutable
import org.slf4j.LoggerFactory

object Search {

    private var exists = false

    var instance: Search = null

    def init() {
        if (exists)
            throw new RuntimeException("The Search class must not be created more than once to avoid connection leaks.")
        instance = new Search()
        instance.init()
        exists = true
    }
}

class Search extends SearchBase[SearchBean](classOf[SearchBean]) {

    val log = LoggerFactory.getLogger(getClass)

    var beanManager: TSearchBeanManager = null

    def rebuildIndex() {
        log.info("rebuilding search index...")

        delete("*:*", commit = false, waitForCommit = false)

        if (beanManager == null) {
            throw new Exception("The beanManager has not been set.")
        }

        val beans = mutable.Buffer[SearchBean]()
        beanManager.modelClasses.foreach(modelClass =>
            Database.forEach(modelClass)(model => {
                log.trace("adding '" + model.id + "' to search index")
                beans += beanManager.createBean(model)
            }))
        add(beans, commit = true, waitForCommit = false)

        log.info("search index has been rebuilt")
    }

    def update(items: Iterable[_ <: UniqueIdModelBase]) {

        if (beanManager == null) {
            throw new Exception("The beanManager has not been set.")
        }

        val deleted = items.filter(_.deleted)
        if (!deleted.isEmpty) {
            delete(deleted.map(beanManager.createBean(_)), commit = false, waitForCommit = false)
        }

        val alive = items.filter(!_.deleted)
        if (!alive.isEmpty) {
            add(alive.map(beanManager.createBean(_)), commit = false, waitForCommit = false)
        }

        commit(waitForCommit = false)
    }

    def queryIds(typ: Int, value: String, maxResults: Int): Iterable[util.UUID] = {
        val clean = cleanup(value)
        val queryStr = if (!clean.isEmpty) "text_value:\"" + clean + "\"" else "*:*"
        val filterStr = "int_type:" + typ
        return queryIds(queryStr, filterStr, maxResults).map(_._2)
    }

    def queryIds(value: String, maxResults: Int): Iterable[(Int, util.UUID)] = {
        val clean = cleanup(value)
        val queryStr = if (!clean.isEmpty) "text_value:\"" + clean + "\"" else "*:*"
        return queryIds(queryStr, null, maxResults)
    }

    private def queryIds(query: String, filterQuery: String, maxResults: Int): Iterable[(Int, util.UUID)] = {
        return super.query(query, filterQuery, maxResults).map(it => (it.typ, util.UUID.fromString(it.id)))
    }

    private def cleanup(query: String): String = {
        return query.replace("\"", "").replace("\'", "").trim
    }

}
