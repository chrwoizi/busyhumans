package com.c5000.mastery.shared;

public class PublicDisqusConfig {

    public static final String SHORT_NAME_DEV = "busyhumansdev";
    public static final String SHORT_NAME_LIVE = "busyhumans";
    public static final String SHORT_NAME = Config.IS_LIVE ? SHORT_NAME_LIVE : SHORT_NAME_DEV;

}
