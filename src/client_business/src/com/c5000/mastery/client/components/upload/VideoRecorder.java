package com.c5000.mastery.client.components.upload;

import com.c5000.mastery.client.SimpleAsyncCallback;
import com.c5000.mastery.client.auth.Auth;
import com.c5000.mastery.client.events.MasteryEvents;
import com.c5000.mastery.client.events.VideoRecordingCompleteE;
import com.c5000.mastery.shared.data.base.VideoD;
import com.c5000.mastery.shared.services.IMasteryS;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Widget;

import java.util.ArrayList;
import java.util.List;

public class VideoRecorder extends Composite {
    interface ThisUiBinder extends UiBinder<Widget, VideoRecorder> {}
    private static ThisUiBinder uiBinder = GWT.create(ThisUiBinder.class);

    @UiField HTMLPanel outer;
    @UiField HTMLPanel inner;
    @UiField Button cancel;

    private ArrayList<VideoD> knownVideos;
    private Timer checkingTimer;

    private SimpleAsyncCallback<Void> onRecordingBegin;
    private SimpleAsyncCallback<Void> onRecordingEnd;

    public VideoRecorder() {
        initWidget(uiBinder.createAndBindUi(this));
    }

    protected void onAttach() {
        super.onAttach();
    }

    public void start(SimpleAsyncCallback<Void> onRecordingBegin, SimpleAsyncCallback<Void> onRecordingEnd) {
        this.onRecordingBegin = onRecordingBegin;
        this.onRecordingEnd = onRecordingEnd;

        if (!Auth.google.credentialValidatedByServer())
            throw new RuntimeException("Must be authorized with google when clicking a " + VideoRecorder.class.getName() + " instance.");
        record();
    }

    @UiHandler("cancel")
    void cancelClicked(ClickEvent event) {
        stopCheckingTimer();
        onRecordingEnd();
    }

    private void stopCheckingTimer() {
        if (checkingTimer != null) {
            checkingTimer.cancel();
            checkingTimer = null;
        }
    }

    private void ensureCheckingTimer() {
        if (checkingTimer == null) {
            checkingTimer = new Timer() {
                @Override
                public void run() {
                    IMasteryS.Instance.get().getYoutubeVideos(new SimpleAsyncCallback<ArrayList<VideoD>>() {
                        @Override
                        public void onSuccess(ArrayList<VideoD> result) {
                            if (result != null && knownVideos != null) {
                                List<VideoD> newVideos = getUnknownVideos(result);
                                if (newVideos.size() == 1) {
                                    stopCheckingTimer();
                                    MasteryEvents.dispatch(new VideoRecordingCompleteE(VideoRecorder.this, newVideos.get(0)));
                                    onRecordingEnd();
                                } else {
                                    checkingTimer.schedule(5000);
                                }
                            }
                        }
                    });
                }
            };
            checkingTimer.schedule(5000);
        }
    }

    private List<VideoD> getUnknownVideos(ArrayList<VideoD> videos) {
        List<VideoD> result = new ArrayList<VideoD>();
        for (VideoD video : videos) {
            boolean known = false;
            for (VideoD knownVideo : knownVideos) {
                if (video.id.equals(knownVideo.id)) {
                    known = true;
                    break;
                }
            }
            if (!known)
                result.add(video);
        }
        return result;
    }

    private void record() {
        Window.open("http://www.youtube.com/my_webcam", "_blank", "");
        IMasteryS.Instance.get().getYoutubeVideos(new SimpleAsyncCallback<ArrayList<VideoD>>() {
            @Override
            public void onSuccess(ArrayList<VideoD> result) {
                if (result != null) {
                    knownVideos = result;
                    onRecordingBegin();
                    ensureCheckingTimer();
                } else {
                    onRecordingEnd();
                }
            }
        });
    }

    private void onRecordingBegin() {
        inner.setVisible(true);
        if (onRecordingBegin != null)
            onRecordingBegin.onSuccess(null);
    }

    private void onRecordingEnd() {
        inner.setVisible(false);
        if (onRecordingEnd != null)
            onRecordingEnd.onSuccess(null);
    }

    public void setEnabled(boolean enabled) {
        if (!enabled)
            cancelClicked(null);
    }
}