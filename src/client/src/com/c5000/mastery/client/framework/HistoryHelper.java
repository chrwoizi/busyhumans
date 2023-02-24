package com.c5000.mastery.client.framework;

import com.google.gwt.user.client.History;

public class HistoryHelper {
    public static void navigateOrRefresh(String token) {
        if (History.getToken().equals(token)) {
            History.fireCurrentHistoryState();
        } else {
            History.newItem(token, true);
        }
    }
}
