package com.c5000.mastery.shared.data.base;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.Date;

public class AssignmentD implements IsSerializable {
    public String id;
    public String authorId;
    public String skillId;
    public String title;
    public ResourceD picture;
    public String description;
    public int reward;
    public Date creationTimestamp;
    public boolean canComplete;
    public boolean hasCompleted;
    public Integer boosted;
    public boolean hasReportedAbuse;
    public int abuseReports;
    public boolean subscribed;
}
