package com.c5000.mastery.database.search.base

import scala.collection.JavaConverters._
import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.client.solrj.impl.BinaryRequestWriter
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer


abstract class SearchBase[Bean <: IndexedSearchBean](beanClass: Class[Bean]) {

    private final val SOLR_BASE_URL = "http://localhost:8080/solr/"

    private var server: CommonsHttpSolrServer = null

    def init() {
        // CommonsHttpSolrServer is thread-safe and if you are using the following constructor,
        // you *MUST* re-use the same instance for all requests.  If instances are created on
        // the fly, it can cause a connection leak. The recommended practice is to keep a
        // static instance of CommonsHttpSolrServer per solr server url and share it for all requests.
        // See https://issues.apache.org/jira/browse/SOLR-861 for more details
        server = new CommonsHttpSolrServer(SOLR_BASE_URL)
        // Be sure you have also enabled the "BinaryUpdateRequestHandler" in your solrconfig.xml for example like:
        // <requestHandler name="/update/javabin" class="solr.BinaryUpdateRequestHandler" />
        server.setRequestWriter(new BinaryRequestWriter())
    }

    /**
     * Deletes all data
     */
    protected def wipe() {
        server.deleteByQuery("*:*")
        server.commit(true, true)
    }

    /**
     * adds objects to the search index.
     * @param beans list of objects that must have fields annotated with @Field
     */
    protected def add(beans: Iterable[Bean], commit: Boolean, waitForCommit: Boolean) {
        if (beans.size > 0) {
            server.addBeans(beans.asJava.iterator)
            if (commit)
                server.commit(waitForCommit, waitForCommit)
        }
    }

    /**
     * removes objects from the search index.
     */
    protected def delete(beans: Iterable[_ <: IndexedSearchBean], commit: Boolean, waitForCommit: Boolean) {
        if (beans.size > 0) {
            val srv = server; // required
            beans.foreach(it => {
                srv.deleteById(it.asInstanceOf[IndexedSearchBean].id)
            })
            if (commit)
                server.commit(waitForCommit, waitForCommit)
        }
    }

    /**
     * removes objects from the search index.
     */
    protected def delete(query: String, commit: Boolean, waitForCommit: Boolean) {
        server.deleteByQuery(query)
        if (commit)
            server.commit(waitForCommit, waitForCommit)
    }

    protected def commit(waitForCommit: Boolean) {
        server.commit(waitForCommit, waitForCommit)
    }

    protected def rollback(waitForCommit: Boolean) {
        server.commit(waitForCommit, waitForCommit)
    }

    /**
     * searches for objects.
     */
    protected def query(query: String, filterQuery: String, maxResults: Int): Iterable[Bean] = {
        val q = new SolrQuery(query)
        if (filterQuery != null)
            q.setFilterQueries(filterQuery)
        q.setRows(maxResults)
        val response = server.query(q)
        return response.getBeans(beanClass).asScala
    }

    /**
     * searches for objects and returns only the number of matches.
     */
    protected def queryCount(query: String): Long = {
        val q = new SolrQuery(query)
        q.setRows(0)
        val response = server.query(q)
        return response.getResults.getNumFound
    }

}
