package com.c5000.mastery.backend.twitter;

import com.c5000.mastery.shared.Config;

public class PrivateTwitterConfig {

    public static final String APP_KEY_DEV = "***REMOVED***";
    public static final String APP_KEY_LIVE = "***REMOVED***";
    public static final String APP_KEY = Config.IS_LIVE ? APP_KEY_LIVE : APP_KEY_DEV;

    public static final String APP_SECRET_DEV = "***REMOVED***";
    public static final String APP_SECRET_LIVE = "***REMOVED***";
    public static final String APP_SECRET = Config.IS_LIVE ? APP_SECRET_LIVE : APP_SECRET_DEV;

}
