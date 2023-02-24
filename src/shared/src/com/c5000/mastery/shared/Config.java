package com.c5000.mastery.shared;

public class Config {

    public static final boolean IS_LIVE = true;

    /**
     * Latest data version
     */
    public static final int VERSION = 17;

    /**
     * unix time in milliseconds when the terms of service have been changed
     */
    public static final long CURRENT_TOS_VERSION = 1342275766000L;

    public static final String SYS_OBJ_ID = "00000000-0000-0000-0000-000000000000";

    /**
     * reward for assignments created by system
     */
    public static final int SYS_REWARD = 25;

    public static final String GWT_CODE_SVR = "gwt.codesvr=192.168.178.60:9997";

    public static final String BASE_URL_DEV = "http://localhost:8888/";
    public static final String BASE_URL_LIVE = "http://busyhumans.woizischke.com/";
    public static final String BASE_URL = IS_LIVE ? BASE_URL_LIVE : BASE_URL_DEV;
    public static final String BASE_URL_GWT = IS_LIVE ? BASE_URL_LIVE : (BASE_URL_DEV + "?" + GWT_CODE_SVR);

    public static final String META_URL_DEV = BASE_URL_DEV + "meta";
    public static final String META_URL_LIVE = BASE_URL_LIVE + "meta";
    public static final String META_URL = IS_LIVE ? META_URL_LIVE : META_URL_DEV;

    public static final int MAX_UPLOADED_IMAGE_WIDTH_HIRES = 2000;
    public static final int MAX_UPLOADED_IMAGE_HEIGHT_HIRES = 2000;

    public static final int MAX_UPLOADED_IMAGE_WIDTH_LARGE = 400;
    public static final int MAX_UPLOADED_IMAGE_HEIGHT_LARGE = 400;

    public static final int MAX_UPLOADED_IMAGE_WIDTH_MEDIUM = 120;
    public static final int MAX_UPLOADED_IMAGE_HEIGHT_MEDIUM = 120;

    public static final int MAX_UPLOADED_IMAGE_WIDTH_SMALL = 50;
    public static final int MAX_UPLOADED_IMAGE_HEIGHT_SMALL = 50;

    public static final int ANON_PASSWORD_LENGTH_MIN = 8;
    public static final int ANON_PASSWORD_LENGTH_MAX = 64;

    public static final int ANON_NAME_LENGTH_MIN = 2;
    public static final int ANON_NAME_LENGTH_MAX = 48;

    public static final String EMAIL_REGEX = "^[^@]+@[^@]+\\.[^@\\.]+$";

    public static final int PAGE_SIZE = 10;

    public static final String FOUNDER_ASSIGNMENT_ID = "5e7285c2-02e0-4bc9-8d0c-2ac781615e15";

    public static final String AUTH_COOKIE = "mpsid";

    public static final boolean ENABLE_PICTURES = false;

    public static final boolean ENABLE_CAPTCHAS = false;

    public static final boolean ENABLE_SEARCH = false;

    public static final boolean ENABLE_FLICKR = false;

    public static final boolean ENABLE_LOGIN = false;

    public static final boolean ENABLE_SOCIAL = false;

}
