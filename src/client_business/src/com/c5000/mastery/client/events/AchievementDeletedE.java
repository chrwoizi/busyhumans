package com.c5000.mastery.client.events;

import com.c5000.mastery.client.events.MasteryEvents.MasteryEvent;
import com.c5000.mastery.shared.data.base.ActivityD;
import com.c5000.mastery.shared.data.combined.AchievementVD;

/**
 * Invoked when an achievement has been deleted
 */
public class AchievementDeletedE implements MasteryEvent {
    public String achievementId;

    public AchievementDeletedE(String achievementId) {
        this.achievementId = achievementId;
    }
}