package com.c5000.mastery.client.events;

import com.c5000.mastery.client.events.MasteryEvents.MasteryEvent;
import com.c5000.mastery.shared.data.base.PersonD;
import com.c5000.mastery.shared.data.combined.AssignmentVD;

/**
 * Invoked when a assignment has been deleted
 */
public class AssignmentDeletedE implements MasteryEvent {
    public String assignmentId;

    public AssignmentDeletedE(String assignmentId) {
        this.assignmentId = assignmentId;
    }
}