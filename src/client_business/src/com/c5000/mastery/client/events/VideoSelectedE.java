package com.c5000.mastery.client.events;

import com.c5000.mastery.client.components.upload.VideoSelect;
import com.c5000.mastery.client.events.MasteryEvents.MasteryEvent;
import com.c5000.mastery.shared.data.base.VideoD;

/**
 * Invoked when a video was selected
 */
public class VideoSelectedE implements MasteryEvent {

    public VideoSelect sender;
    public VideoD video;

    public VideoSelectedE(VideoSelect sender, VideoD video) {
        this.sender = sender;
        this.video = video;
    }
}