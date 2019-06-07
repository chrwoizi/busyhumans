package com.c5000.mastery.client.events;

import com.c5000.mastery.client.events.MasteryEvents.MasteryEvent;
import com.c5000.mastery.shared.data.base.SkillD;

/**
 * Invoked when an existing skill has been selected in the assignment creation process
 */
public class SkillSelectedE implements MasteryEvent {
    public SkillD skill;

    public SkillSelectedE(SkillD skill) {
        this.skill = skill;
    }
}