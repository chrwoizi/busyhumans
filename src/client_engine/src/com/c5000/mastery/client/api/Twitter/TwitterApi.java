package com.c5000.mastery.client.api.Twitter;

import com.c5000.mastery.client.Loggers;
import com.c5000.mastery.client.SimpleAsyncCallback;
import com.c5000.mastery.client.auth.Auth;
import com.c5000.mastery.shared.data.auth.CredentialCheckResultD;
import com.c5000.mastery.shared.services.IMasteryS;
import com.google.gwt.core.client.JavaScriptObject;

public class TwitterApi {

    public static void login() {
        // open popup within the current user click event to avoid popup blockers
        final JavaScriptObject popup = openPopup("/empty.jsp", "busyhumans_twitterauth", "width=784,height=702,resizable=yes");

        // get auth status from server
        IMasteryS.Instance.get().checkTwitterCredential(new SimpleAsyncCallback<CredentialCheckResultD>() {
            @Override
            public void onSuccess(CredentialCheckResultD result) {
                if (result.valid) {
                    // user was authorized all along, so we didn't really need the popup.
                    closePopup(popup);
                    Auth.twitter.setValidated(true);
                    Auth.authAtMastery(Auth.twitter);
                } else {
                    if (result.authUrl == null) {
                        Loggers.root.error("Not authorized with Twitter but no auth url given.");
                        closePopup(popup);
                    } else {
                        // user needs to authorize. redirect popup to twitter login page
                        redirectPopup(popup, result.authUrl);
                    }
                }
            }
        });
    }

    private static native void closePopup(JavaScriptObject popup) /*-{
        popup.close();
    }-*/;

    private static native void redirectPopup(JavaScriptObject popup, String authUrl) /*-{
        popup.location = authUrl;
    }-*/;

    private static native JavaScriptObject openPopup(String url, String name, String params) /*-{
        var popup = window.open(url, name, params);
        window.oauth1callback = function (verifier) {
            @com.c5000.mastery.client.api.Twitter.TwitterApi::popupCallback(Ljava/lang/String;Lcom/google/gwt/core/client/JavaScriptObject;)(verifier, popup);
        }
        return popup;
    }-*/;

    static void popupCallback(String verifier, final JavaScriptObject popup) {
        // the user has granted or denied the authorization on the twitter login page
        closePopup(popup);
        Auth.twitter.onCredentialChanged(verifier);
        Auth.authAtMastery(Auth.twitter);
    }
}
