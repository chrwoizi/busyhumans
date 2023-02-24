package com.c5000.mastery.shared.data.combined;

import com.c5000.mastery.shared.data.base.*;
import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AchievementVD implements IsSerializable {
    public String id;
    public String owner;
    public SkillD skill;
    public ArrayList<AchievementActivityD> activities;
    public HashMap<String, PersonD> persons;
}
