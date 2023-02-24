package com.c5000.mastery.shared.data.base;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.Date;

public class AnnouncementD implements IsSerializable {
    public String id;
    public Date showTime;
    public Date hideTime;
    public String text;
    public boolean maintenance;
}
