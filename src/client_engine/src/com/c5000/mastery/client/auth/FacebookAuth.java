package com.c5000.mastery.client.auth;

import com.c5000.mastery.client.Loggers;
import com.c5000.mastery.client.SimpleAsyncCallback;
import com.c5000.mastery.client.api.facebook.AuthResponse;
import com.c5000.mastery.client.api.facebook.FBCore;
import com.c5000.mastery.client.api.facebook.FBEvent;
import com.c5000.mastery.client.api.facebook.LoginStatus;
import com.c5000.mastery.client.events.MasteryEvents;
import com.c5000.mastery.client.events.auth.AuthProviderCredentialChangedE;
import com.c5000.mastery.client.events.auth.AuthProviderCredentialValidChangedE;
import com.c5000.mastery.shared.data.auth.AuthResultD;
import com.c5000.mastery.shared.data.auth.AuthStatus;
import com.c5000.mastery.shared.services.IMasteryS;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;

public class FacebookAuth extends AuthProvider {

    private static FBCore fbCore = GWT.create(FBCore.class);
    private static FBEvent fbEvent = GWT.create(FBEvent.class);
    private boolean registeredChangeHandler = false;

    private String username;
    private String accessToken;

    public FacebookAuth() {
        getCredential();
    }

    @Override
    public boolean hasCredential() {
        return username != null && accessToken != null;
    }

    @Override
    public void authAtMastery() {
        IMasteryS.Instance.get().authWithFacebook(username, accessToken, new SimpleAsyncCallback<AuthResultD>() {
            @Override
            public void onSuccess(AuthResultD result) {
                Auth.setMasteryAuth(result);
            }
        });
    }

    @Override
    public void reset() {
        super.reset();
        username = null;
        accessToken = null;
    }

    private void onCredentialChanged(String username, String accessToken) {
        super.reset();
        this.username = username;
        this.accessToken = accessToken;
        MasteryEvents.dispatch(new AuthProviderCredentialChangedE(this));
    }

    private void getCredential() {
        fbCore.getLoginStatus(new SimpleAsyncCallback<JavaScriptObject>() {
            @Override
            public void onSuccess(JavaScriptObject js) {
                tryRegisterChangeHandler();
                LoginStatus result = (LoginStatus) js;
                onCredentialChanged(result.authResponse());
            }
        });
    }

    private void tryRegisterChangeHandler() {
        if (registeredChangeHandler)
            return;
        registeredChangeHandler = true;
        fbEvent.subscribe("auth.authResponseChange", new SimpleAsyncCallback<JavaScriptObject>() {
            @Override
            public void onSuccess(JavaScriptObject js) {
                LoginStatus result = (LoginStatus) js;
                onCredentialChanged(result.authResponse());
            }
        });
    }

    private void onCredentialChanged(AuthResponse result) {
        Loggers.auth.debug("FacebookAuth.onCredentialChanged: " + (result != null ? (result.userID() + " / " + result.accessToken()) : "null"));
        if (result != null) {
            if (result.userID() != null && result.accessToken() != null) {
                onCredentialChanged(result.userID(), result.accessToken());
            } else {
                onCredentialChanged(null, null);
            }
        } else {
            onCredentialChanged(null, null);
        }
    }
}
