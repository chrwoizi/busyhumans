package com.c5000.mastery.client.api.facebook;

import com.google.gwt.core.client.JavaScriptObject;

public class AuthResponse extends JavaScriptObject {
    protected AuthResponse() {}

    public final native String accessToken() /*-{
        return this.accessToken;
    }-*/;

    public final native String userID() /*-{
        return this.userID;
    }-*/;

    public final native String signedRequest() /*-{
        return this.signedRequest;
    }-*/;

    public final native String expiresIn() /*-{
        return this.expiresIn;
    }-*/;
}
