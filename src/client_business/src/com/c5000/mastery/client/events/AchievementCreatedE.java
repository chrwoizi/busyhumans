package com.c5000.mastery.client.events;

import com.c5000.mastery.client.events.MasteryEvents.MasteryEvent;
import com.c5000.mastery.shared.data.combined.AchievementVD;

/**
 * Invoked when an achievement has been created
 */
public class AchievementCreatedE implements MasteryEvent {
    public String personId;
    public AchievementVD achievement;

    public AchievementCreatedE(String personId, AchievementVD achievement) {
        this.personId = personId;
        this.achievement = achievement;
    }
}