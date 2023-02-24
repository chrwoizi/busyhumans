package com.c5000.mastery.client.events;

import com.c5000.mastery.client.events.MasteryEvents.MasteryEvent;
import com.c5000.mastery.client.components.upload.FileUploadV;

/**
 * Invoked when a file upload has finished
 */
public class FileUploadCompleteE implements MasteryEvent {

    public FileUploadV sender;
    public String fileId;

    public FileUploadCompleteE(FileUploadV sender, String fileId) {
        this.sender = sender;
        this.fileId = fileId;
    }
}