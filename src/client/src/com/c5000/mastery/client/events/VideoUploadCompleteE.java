package com.c5000.mastery.client.events;

import com.c5000.mastery.client.components.upload.VideoUpload;
import com.c5000.mastery.client.events.MasteryEvents.MasteryEvent;

/**
 * Invoked when a file upload has finished
 */
public class VideoUploadCompleteE implements MasteryEvent {

    public VideoUpload sender;
    public String fileId;

    public VideoUploadCompleteE(VideoUpload sender, String fileId) {
        this.sender = sender;
        this.fileId = fileId;
    }
}