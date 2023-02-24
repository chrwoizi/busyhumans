package com.c5000.mastery.client.events;

import com.c5000.mastery.client.events.MasteryEvents.MasteryEvent;
import com.c5000.mastery.shared.data.base.SkillD;

/**
 * Invoked when a skill has been selected or changed in the assignment creation process
 */
public class SelectedSkillChangedE implements MasteryEvent {
    public boolean isValid;

    public SelectedSkillChangedE(boolean isValid) {
        this.isValid = isValid;
    }
}