package com.c5000.mastery.shared.data.base;

import com.google.gwt.user.client.rpc.IsSerializable;

public class AssignmentCreationParamsD implements IsSerializable {

    /**
     * set either skillId or newSkillParams
     */
    public String skillId;

    /**
     * set either skillId or newSkillParams
     */
    public SkillCreationParamsD newSkillParams;

    /**
     * assignment title
     */
    public String title;

    /**
     * assignment description
     */
    public String description;

}
