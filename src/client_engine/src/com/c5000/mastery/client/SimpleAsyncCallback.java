package com.c5000.mastery.client;

import com.c5000.mastery.client.auth.Auth;
import com.c5000.mastery.shared.AccessException;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;

public abstract class SimpleAsyncCallback<T> implements AsyncCallback<T> {
    @Override
    public void onFailure(Throwable caught) {
        if (caught instanceof AccessException) {
            switch (Auth.status) {
                case AUTHORIZED:
                case NEEDS_TOS_CONFIRMATION:
                    Auth.reAuthAtMastery();
                    return;
                default:
                    break;
            }
        }
        else {
            Window.Location.reload();
        }
    }
}
