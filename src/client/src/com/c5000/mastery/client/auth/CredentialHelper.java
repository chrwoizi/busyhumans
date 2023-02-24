package com.c5000.mastery.client.auth;

public class CredentialHelper {

    public static String hashAnonPassword(String username, String password) {
        if(username == null || password == null)
            return null;
        return hash(password + username);
    }

    private static native String hash(String clear) /*-{
        CryptoJS = $wnd.CryptoJS;
        var hash = CryptoJS.SHA256(clear);
        var base64 = hash.toString(CryptoJS.enc.Base64);
        return base64;
    }-*/;

}
