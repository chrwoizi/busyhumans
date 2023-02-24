package com.c5000.mastery.client.api.facebook;

import com.c5000.mastery.client.SimpleAsyncCallback;
import com.c5000.mastery.client.auth.Auth;
import com.c5000.mastery.shared.PublicFacebookConfig;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.rpc.AsyncCallback;

public class FacebookApi {

    private static final boolean STATUS = false;
    private static final boolean COOKIE = true;
    private static final boolean XFBML = true;

    private static FBCore fbCore = GWT.create(FBCore.class);

    private static SimpleAsyncCallback<Void> onInit;

    public static void init(SimpleAsyncCallback<Void> onComplete) {
        onInit = onComplete;
        if (isFbScriptLoaded()) {
            onFbScriptLoaded();
        } else {
            registerFbScriptLoadListener();
        }
    }

    public static void login() {
        fbCore.login(new AsyncCallback<JavaScriptObject>() {
            @Override
            public void onFailure(Throwable caught) {
            }

            @Override
            public void onSuccess(JavaScriptObject js) {
                LoginStatus result = (LoginStatus) js;
                if (result.status().equals("connected")) {
                    Auth.authAtMastery(Auth.facebook);
                }
            }
        }, PublicFacebookConfig.PERMISSIONS);
    }

    private static native boolean isFbScriptLoaded() /*-{
        return $wnd.FB != undefined;
    }-*/;

    private static native void registerFbScriptLoadListener() /*-{
        $wnd.fbAsyncInit = function () {
            @com.c5000.mastery.client.api.facebook.FacebookApi::onFbScriptLoaded()();
        }
    }-*/;

    private static void onFbScriptLoaded() {
        fbCore.init(PublicFacebookConfig.FACEBOOK_APP_ID, STATUS, COOKIE, XFBML);
        onInit.onSuccess(null);
    }

}
