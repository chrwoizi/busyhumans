package com.c5000.mastery.client.events;

import com.c5000.mastery.client.events.MasteryEvents.MasteryEvent;

/**
 * Invoked when a skill has been deleted
 */
public class SkillDeletedE implements MasteryEvent {
    public String skillId;

    public SkillDeletedE(String skillId) {
        this.skillId = skillId;
    }
}