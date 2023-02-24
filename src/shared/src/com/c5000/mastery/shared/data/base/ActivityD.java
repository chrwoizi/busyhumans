package com.c5000.mastery.shared.data.base;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.ArrayList;
import java.util.Date;

public class ActivityD implements IsSerializable {
    public String id;
    public String authorId;
    public String achievementId;
    public String assignmentId;
    public ArrayList<ContentBlockD> contentBlocks;
    public int likes;
    public int dislikes;
    public int myRating;
    public Date rewardDueDate;
    public Integer rewarded;
    public boolean canDelete;
    public boolean hasReportedAbuse;
    public int abuseReports;
}
