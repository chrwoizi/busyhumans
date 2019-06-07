package com.c5000.mastery.client;

import com.c5000.mastery.shared.Config;

public class Loggers {

    public static final Logger root = new Logger("mastery", !Config.IS_LIVE);
    public static final Logger auth = new Logger("mastery-auth", !Config.IS_LIVE);

}
