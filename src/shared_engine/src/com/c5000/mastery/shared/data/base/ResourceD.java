package com.c5000.mastery.shared.data.base;

import com.google.gwt.user.client.rpc.IsSerializable;

public class ResourceD implements IsSerializable {

    public String resource;
    public String small;
    public String medium;
    public String large;
    public String hires;

    public String authorName;
    public String authorUrl;

    /**
     * @see com.c5000.mastery.shared.data.base.LicenseTypes
     */
    public int license;
}
