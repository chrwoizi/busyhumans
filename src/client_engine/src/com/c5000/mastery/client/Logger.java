package com.c5000.mastery.client;

public class Logger {

    private String name;
    private boolean active;

    public Logger(String name, boolean active) {
        this.name = name;
        this.active = active;
    }

    public native void debug(String line) /*-{
        if (this.@com.c5000.mastery.client.Logger::active) {
            if (console) {
                if (console.debug) {
                    console.debug(this.@com.c5000.mastery.client.Logger::name + ": " + line);
                }
                else if (console.log) {
                    console.log(this.@com.c5000.mastery.client.Logger::name + ": " + line);
                }
            }
        }
    }-*/;

    public native void info(String line) /*-{
        if (this.@com.c5000.mastery.client.Logger::active) {
            if (console) {
                if (console.info) {
                    console.info(this.@com.c5000.mastery.client.Logger::name + ": " + line);
                }
                else if (console.log) {
                    console.log(this.@com.c5000.mastery.client.Logger::name + ": " + line);
                }
            }
        }
    }-*/;

    public native void warn(String line) /*-{
        if (this.@com.c5000.mastery.client.Logger::active) {
            if (console) {
                if (console.warn) {
                    console.warn(this.@com.c5000.mastery.client.Logger::name + ": " + line);
                }
                else if (console.log) {
                    console.log(this.@com.c5000.mastery.client.Logger::name + ": " + line);
                }
            }
        }
    }-*/;

    public native void error(String line) /*-{
        if (this.@com.c5000.mastery.client.Logger::active) {
            if (console) {
                if (console.error) {
                    console.error(this.@com.c5000.mastery.client.Logger::name + ": " + line);
                }
                else if (console.log) {
                    console.log(this.@com.c5000.mastery.client.Logger::name + ": " + line);
                }
            }
        }
    }-*/;

    public native void error(String line, Throwable throwable) /*-{
        if (this.@com.c5000.mastery.client.Logger::active) {
            if (console) {
                if (console.error) {
                    console.error(this.@com.c5000.mastery.client.Logger::name + ": " + line + ": " + throwable.toString());
                }
                else if (console.log) {
                    console.log(this.@com.c5000.mastery.client.Logger::name + ": " + line + ": " + throwable.toString());
                }
            }
        }
    }-*/;
}
