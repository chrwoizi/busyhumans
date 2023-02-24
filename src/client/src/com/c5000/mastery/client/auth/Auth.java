package com.c5000.mastery.client.auth;

import com.c5000.mastery.client.SimpleAsyncCallback;
import com.c5000.mastery.client.events.MasteryEvents;
import com.c5000.mastery.client.events.auth.AuthProviderCredentialChangedE;
import com.c5000.mastery.client.events.auth.MasteryAuthStatusChangedE;
import com.c5000.mastery.shared.Config;
import com.c5000.mastery.shared.data.auth.AuthProviderType;
import com.c5000.mastery.shared.data.auth.AuthResultD;
import com.c5000.mastery.shared.data.auth.AuthStatus;
import com.c5000.mastery.shared.services.IMasteryS;
import com.google.gwt.user.client.Window;

public class Auth {

    public static FacebookAuth facebook;
    public static GoogleAuth google;
    public static TwitterAuth twitter;
    public static AnonAuth anon;

    private static AuthProvider current;

    public static AuthStatus status = AuthStatus.NOT_AUTHORIZED;
    public static AuthInfo user;
    public static AuthProviderType currentProvider = AuthProviderType.NONE;

    public static void init() {
        if (!Config.ENABLE_LOGIN) return;

        facebook = new FacebookAuth();
        google = new GoogleAuth();
        twitter = new TwitterAuth();
        anon = new AnonAuth();

        watchLogout();

        IMasteryS.Instance.get().authWithSession(new SimpleAsyncCallback<AuthResultD>() {
            @Override
            public void onSuccess(AuthResultD result) {
                Auth.setMasteryAuth(result);
            }
        });
    }

    private static void watchLogout() {
        MasteryEvents.subscribeManually(new MasteryEvents.Listener() {
            @Override
            public void onEvent(MasteryEvents.MasteryEvent event) {
                if (event instanceof AuthProviderCredentialChangedE) {
                    AuthProviderCredentialChangedE e = (AuthProviderCredentialChangedE) event;
                    if (e.provider == current && !current.hasCredential()) {
                        logout();
                    }
                }
            }
        });
    }

    public static void reAuthAtMastery() {
        authAtMastery(current);
    }

    public static void authAtMastery(AuthProvider provider) {
        current = provider;
        if (current != null && current.hasCredential()) {
            current.authAtMastery();
        }
    }

    public static void setMasteryAuth(AuthResultD result) {
        if (result == null) {
            reset();
            MasteryEvents.dispatch(new MasteryAuthStatusChangedE());
        } else {
            boolean changed = Auth.status != result.status || (Auth.user == null || Auth.user.accountId.equals(result.accountId));
            Auth.status = result.status;
            Auth.currentProvider = result.provider;

            if(result.provider == AuthProviderType.ANON && anon.hasCredential() && status == AuthStatus.NOT_AUTHORIZED)
                Window.alert("Wrong username or password.");

            switch (result.status) {
                case NOT_AUTHORIZED:
                case ACCESS_DENIED:
                    reset();
                    break;
                default:
                    user = new AuthInfo();
                    user.isAdmin = result.isAdmin;
                    user.accountId = result.accountId;
                    user.personId = result.personId;
                    user.cloaks = result.cloaks;
                    user.username = result.anonUsername;
                    user.emailAddress = result.emailAddress;
                    break;
            }

            switch (result.provider) {
                case FACEBOOK:
                    current = facebook;
                    break;
                case TWITTER:
                    current = twitter;
                    break;
                case GOOGLE:
                    current = google;
                    break;
                case ANON:
                    current = anon;
                    break;
            }

            if (changed) {
                google.reset();
                if(result.accountId != null)
                google.getCredentialForCurrentUser();
                MasteryEvents.dispatch(new MasteryAuthStatusChangedE());
            }
        }
    }

    private static void reset() {
        if (!Config.ENABLE_LOGIN) return;

        facebook.reset();
        google.reset();
        twitter.reset();
        anon.reset();
        current = null;
        user = null;
    }

    public static void logout() {
        IMasteryS.Instance.get().unauth(new SimpleAsyncCallback<AuthResultD>() {
            @Override
            public void onSuccess(AuthResultD result) {
                Auth.setMasteryAuth(result);
            }
        });
    }
}
