package com.c5000.mastery.shared.data.combined;

import com.c5000.mastery.shared.data.base.PersonD;
import com.c5000.mastery.shared.data.base.SkillD;
import com.google.gwt.user.client.rpc.IsSerializable;

public class SearchResultD implements IsSerializable {
    public PersonD person;
    public AssignmentVD assignment;
    public SkillD skill;
}
