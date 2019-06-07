package com.c5000.mastery.client.api.facebook;

import com.google.gwt.core.client.JavaScriptObject;

public class LoginStatus extends JavaScriptObject {
    protected LoginStatus() {}

    public final native String status() /*-{
        return this.status;
    }-*/;

    public final native AuthResponse authResponse() /*-{
        return this.authResponse;
    }-*/;
}
