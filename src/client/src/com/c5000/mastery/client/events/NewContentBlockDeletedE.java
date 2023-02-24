package com.c5000.mastery.client.events;

import com.c5000.mastery.client.events.MasteryEvents.MasteryEvent;
import com.c5000.mastery.client.views.activity.ContentBlockBaseV;
import com.c5000.mastery.client.views.activity.ImageContentBlockV;

/**
 * Invoked when a content block of a new activity has been deleted
 */
public class NewContentBlockDeletedE implements MasteryEvent {
    public ContentBlockBaseV view;

    public NewContentBlockDeletedE(ContentBlockBaseV view) {
        this.view = view;
    }
}