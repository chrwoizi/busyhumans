package com.c5000.mastery.client.events;

import com.c5000.mastery.client.events.MasteryEvents.MasteryEvent;
import com.c5000.mastery.client.components.upload.FileUploadV;

/**
 * Invoked when a file upload has failed
 */
public class FileUploadFailedE implements MasteryEvent {

    public static enum Reason {
        UNKNOWN,
        SIZE,
        TYPE
    }

    public FileUploadV sender;
    public Reason reason;

    public FileUploadFailedE(FileUploadV sender, Reason reason) {
        this.sender = sender;
        this.reason = reason;
    }
}