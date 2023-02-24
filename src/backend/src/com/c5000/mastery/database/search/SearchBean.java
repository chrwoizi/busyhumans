package com.c5000.mastery.database.search;

import com.c5000.mastery.database.search.base.IndexedSearchBean;
import org.apache.solr.client.solrj.beans.Field;

public class SearchBean extends IndexedSearchBean {

    public @Field("int_type") int typ = 0;
    public @Field("text_value") String value = null;

}
