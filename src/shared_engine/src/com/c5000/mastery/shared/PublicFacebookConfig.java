package com.c5000.mastery.shared;

public class PublicFacebookConfig {

    public static final String FACEBOOK_APP_ID_DEV = "344886702232015";
    public static final String FACEBOOK_APP_ID_LIVE = "346081838790719";
    public static final String FACEBOOK_APP_ID = Config.IS_LIVE ? FACEBOOK_APP_ID_LIVE : FACEBOOK_APP_ID_DEV;

    public static final String FACEBOOK_NAMESPACE_DEV = "busyhumansdev";
    public static final String FACEBOOK_NAMESPACE_LIVE = "busyhumans";
    public static final String FACEBOOK_NAMESPACE = Config.IS_LIVE ? FACEBOOK_NAMESPACE_LIVE : FACEBOOK_NAMESPACE_DEV;

    public static final String PERMISSIONS = "publish_actions";

}
