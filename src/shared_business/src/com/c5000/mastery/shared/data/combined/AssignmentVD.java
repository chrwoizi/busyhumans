package com.c5000.mastery.shared.data.combined;

import com.c5000.mastery.shared.data.base.*;
import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AssignmentVD implements IsSerializable {
    public AssignmentD assignment;
    public SkillD skill;
    public HashMap<String, PersonD> persons;
    public ArrayList<ActivityD> activities;
    public PersonD userPerson;
}
