package com.c5000.mastery.backend.facebook;

import com.c5000.mastery.shared.Config;

public class PrivateFacebookConfig {

    public static final String FACEBOOK_APP_SECRET_DEV = "***REMOVED***";
    public static final String FACEBOOK_APP_SECRET_LIVE = "***REMOVED***";
    public static final String FACEBOOK_APP_SECRET = Config.IS_LIVE ? FACEBOOK_APP_SECRET_LIVE : FACEBOOK_APP_SECRET_DEV;
    public static final boolean CHECK_FB_SIGNED_REQUEST = false;

}
