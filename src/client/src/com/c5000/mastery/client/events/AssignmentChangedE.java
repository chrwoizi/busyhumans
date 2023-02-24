package com.c5000.mastery.client.events;

import com.c5000.mastery.client.events.MasteryEvents.MasteryEvent;
import com.c5000.mastery.shared.data.base.PersonD;
import com.c5000.mastery.shared.data.combined.AssignmentVD;

/**
 * Invoked when a assignment has been changed
 */
public class AssignmentChangedE implements MasteryEvent {
    public AssignmentVD assignment;

    public AssignmentChangedE(AssignmentVD assignment) {
        this.assignment = assignment;
    }
}