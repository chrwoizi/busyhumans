package com.c5000.mastery.client.events;

import com.c5000.mastery.client.events.MasteryEvents.MasteryEvent;
import com.c5000.mastery.shared.data.base.SkillD;

/**
 * Invoked when a skill has been changed
 */
public class SkillChangedE implements MasteryEvent {
    public SkillD skill;

    public SkillChangedE(SkillD skill) {
        this.skill = skill;
    }
}