package com.c5000.mastery.client.events;

import com.c5000.mastery.client.components.upload.VideoRecorder;
import com.c5000.mastery.client.components.upload.VideoUpload;
import com.c5000.mastery.client.events.MasteryEvents.MasteryEvent;
import com.c5000.mastery.shared.data.base.VideoD;

/**
 * Invoked when a video was recorded
 */
public class VideoRecordingCompleteE implements MasteryEvent {

    public VideoRecorder sender;
    public VideoD video;

    public VideoRecordingCompleteE(VideoRecorder sender, VideoD video) {
        this.sender = sender;
        this.video = video;
    }
}