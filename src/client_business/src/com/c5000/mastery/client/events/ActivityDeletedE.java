package com.c5000.mastery.client.events;

import com.c5000.mastery.client.events.MasteryEvents.MasteryEvent;
import com.c5000.mastery.shared.data.base.ActivityD;

/**
 * Invoked when an activity has been deleted
 */
public class ActivityDeletedE implements MasteryEvent {
    public ActivityD activity;

    public ActivityDeletedE(ActivityD activity) {
        this.activity = activity;
    }
}