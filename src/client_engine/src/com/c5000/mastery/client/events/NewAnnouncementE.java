package com.c5000.mastery.client.events;

import com.c5000.mastery.shared.data.base.AnnouncementD;

public class NewAnnouncementE implements MasteryEvents.MasteryEvent {

    public AnnouncementD announcement;

    public NewAnnouncementE(AnnouncementD announcement) {
        this.announcement = announcement;
    }
}
