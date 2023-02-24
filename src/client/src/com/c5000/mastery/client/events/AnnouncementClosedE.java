package com.c5000.mastery.client.events;

import com.c5000.mastery.shared.data.base.AnnouncementD;

public class AnnouncementClosedE implements MasteryEvents.MasteryEvent {

    public AnnouncementD announcement;

    public AnnouncementClosedE(AnnouncementD announcement) {
        this.announcement = announcement;
    }
}
