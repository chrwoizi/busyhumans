package com.c5000.mastery.shared.data.combined;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.ArrayList;

public class SearchResultsD implements IsSerializable {
    public ArrayList<SearchResultD> results;
    public boolean hasMore;
}
