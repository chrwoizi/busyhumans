package com.c5000.mastery.shared.data.base;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.Date;

public class PersonD implements IsSerializable {
    public String id;
    public String name;
    public ResourceD picture;
    public int xp;
    public int level;
    public float levelProgress;
    public int levelXp;
    public int nextLevelXp;
    public int newAssignmentReward;
    public Date joinDate;
    public Integer createdAssignments;
    public Integer completedAssignments;
    public boolean founder;
}
