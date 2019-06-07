package com.c5000.mastery.client.events;

import com.c5000.mastery.client.events.MasteryEvents.MasteryEvent;
import com.c5000.mastery.shared.data.base.PersonD;

/**
 * Invoked when a person attribute has changed
 */
public class PersonChangedE implements MasteryEvent {
    public PersonD person;

    public PersonChangedE(PersonD person) {
        this.person = person;
    }
}