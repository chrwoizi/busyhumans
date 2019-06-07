package com.c5000.mastery.client.events;

import com.c5000.mastery.client.events.MasteryEvents.MasteryEvent;
import com.c5000.mastery.shared.data.base.ActivityD;
import com.c5000.mastery.shared.data.base.PersonD;
import com.c5000.mastery.shared.data.combined.AssignmentVD;

/**
 * Invoked when an activity has been created
 */
public class ActivityCreatedE implements MasteryEvent {
    public ActivityD activity;
    public PersonD author;

    public ActivityCreatedE(ActivityD activity, PersonD author) {
        this.activity = activity;
        this.author = author;
    }
}