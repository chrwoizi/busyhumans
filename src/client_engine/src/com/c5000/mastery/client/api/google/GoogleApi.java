package com.c5000.mastery.client.api.google;

import com.c5000.mastery.client.Loggers;
import com.c5000.mastery.client.SimpleAsyncCallback;
import com.c5000.mastery.client.auth.Auth;
import com.c5000.mastery.shared.data.auth.CredentialCheckResultD;
import com.c5000.mastery.shared.services.IMasteryS;
import com.google.gwt.core.client.JavaScriptObject;

public class GoogleApi {

    public static void login() {
        // open popup within the current user click event to avoid popup blockers
        final JavaScriptObject popup = openPopup("/empty.jsp", "busyhumans_googleauth", "width=800,height=400,resizable=yes");

        // get auth status from server
        IMasteryS.Instance.get().checkGoogleCredential(new SimpleAsyncCallback<CredentialCheckResultD>() {
            @Override
            public void onSuccess(CredentialCheckResultD result) {
                evaluateAuthResult(result, popup);
            }
        });
    }

    private static void evaluateAuthResult(CredentialCheckResultD result, JavaScriptObject popup) {
        if (result.valid) {
            // user was authorized all along, so we didn't really need the popup.
            closePopup(popup);
            Auth.google.setValidated(true);
        } else {
            if (result.authUrl == null) {
                Loggers.root.error("Not authorized with Google but no auth url given.");
                closePopup(popup);
            } else {
                // user needs to authorize. redirect popup to google login page
                redirectPopup(popup, result.authUrl);
            }
        }
    }

    private static native void closePopup(JavaScriptObject popup) /*-{
        popup.close();
    }-*/;

    private static native void redirectPopup(JavaScriptObject popup, String authUrl) /*-{
        popup.location = authUrl;
    }-*/;

    private static native JavaScriptObject openPopup(String url, String name, String params) /*-{
        var popup = window.open(url, name, params);
        window.oauth2callback = function (code) {
            @com.c5000.mastery.client.api.google.GoogleApi::popupCallback(Ljava/lang/String;Lcom/google/gwt/core/client/JavaScriptObject;)(code, popup);
        }
        return popup;
    }-*/;

    static void popupCallback(String code, final JavaScriptObject popup) {
        // the user has granted or denied the authorization on the google login page
        closePopup(popup);
        Auth.google.onCredentialChanged(code);
    }
}
