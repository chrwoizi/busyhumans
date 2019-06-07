package com.c5000.mastery.shared.data.base;

import com.google.gwt.user.client.rpc.IsSerializable;

public class SkillD implements IsSerializable {
    public String id;
    public ResourceD picture;
    public String title;
    public ResourceD description;
    public boolean hasReportedAbuse;
    public int abuseReports;
}
