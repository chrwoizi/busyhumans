package com.c5000.mastery.backend.google;

import com.c5000.mastery.shared.Config;

import java.util.Arrays;
import java.util.List;

public class PrivateGoogleConfig {

    public static final List<String> GOOGLE_SCOPE = Arrays.asList("http://gdata.youtube.com");

    public static final String GOOGLE_APP_ID_DEV = "***REMOVED***.apps.googleusercontent.com";
    public static final String GOOGLE_APP_ID_LIVE = "***REMOVED***.apps.googleusercontent.com";
    public static final String GOOGLE_APP_ID = Config.IS_LIVE ? GOOGLE_APP_ID_LIVE : GOOGLE_APP_ID_DEV;

    public static final String GOOGLE_APP_SECRET_DEV = "***REMOVED***";
    public static final String GOOGLE_APP_SECRET_LIVE = "***REMOVED***";
    public static final String GOOGLE_APP_SECRET = Config.IS_LIVE ? GOOGLE_APP_SECRET_LIVE : GOOGLE_APP_SECRET_DEV;

    public static final String GOOGLE_REDIRECT = Config.BASE_URL + "mastery/oauth2callback";

    public static final String YOUTUBE_DEVELOPER_KEY = "***REMOVED***";

}
