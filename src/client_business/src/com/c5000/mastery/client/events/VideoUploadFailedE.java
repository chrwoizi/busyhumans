package com.c5000.mastery.client.events;

import com.c5000.mastery.client.components.upload.VideoUpload;
import com.c5000.mastery.client.events.MasteryEvents.MasteryEvent;

/**
 * Invoked when a file upload has failed
 */
public class VideoUploadFailedE implements MasteryEvent {

    public static enum Reason {
        UNKNOWN
    }

    public VideoUpload sender;
    public Reason reason;

    public VideoUploadFailedE(VideoUpload sender, Reason reason) {
        this.sender = sender;
        this.reason = reason;
    }
}