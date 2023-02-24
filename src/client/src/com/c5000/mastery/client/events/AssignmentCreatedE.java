package com.c5000.mastery.client.events;

import com.c5000.mastery.client.events.MasteryEvents.MasteryEvent;
import com.c5000.mastery.shared.data.base.PersonD;
import com.c5000.mastery.shared.data.combined.AssignmentVD;

/**
 * Invoked when a assignment has been created
 */
public class AssignmentCreatedE implements MasteryEvent {
    public PersonD person;
    public AssignmentVD assignment;
    public boolean createdByThisUser;

    public AssignmentCreatedE(PersonD person, AssignmentVD assignment, boolean createdByThisUser) {
        this.person = person;
        this.assignment = assignment;
        this.createdByThisUser = createdByThisUser;
    }
}