package com.c5000.mastery.client.components.upload;

import com.c5000.mastery.client.SimpleAsyncCallback;
import com.c5000.mastery.client.auth.Auth;
import com.c5000.mastery.client.events.MasteryEvents;
import com.c5000.mastery.client.events.VideoSelectedE;
import com.c5000.mastery.shared.data.base.VideoD;
import com.c5000.mastery.shared.services.IMasteryS;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.*;

import java.util.ArrayList;

public class VideoSelect extends Composite implements MasteryEvents.Listener {
    interface ThisUiBinder extends UiBinder<Widget, VideoSelect> {}
    private static ThisUiBinder uiBinder = GWT.create(ThisUiBinder.class);

    @UiField HTMLPanel outer;
    @UiField HTMLPanel inner;
    @UiField HorizontalPanel videos;
    @UiField Button cancel;

    private SimpleAsyncCallback<Void> onSelectingBegin;
    private SimpleAsyncCallback<Void> onSelectingEnd;

    public VideoSelect() {
        initWidget(uiBinder.createAndBindUi(this));
    }

    protected void onAttach() {
        super.onAttach();
        MasteryEvents.subscribeWidget(this);
    }

    public void start(SimpleAsyncCallback<Void> onRecordingBegin, SimpleAsyncCallback<Void> onRecordingEnd) {
        this.onSelectingBegin = onRecordingBegin;
        this.onSelectingEnd = onRecordingEnd;

        if(!Auth.google.credentialValidatedByServer())
            throw new RuntimeException("Must be authorized with google when clicking a " + VideoRecorder.class.getName() + " instance.");
        select();
    }

    @Override
    public void onEvent(MasteryEvents.MasteryEvent event) {
        if(event instanceof VideoSelectedE) {
            VideoSelectedE e = (VideoSelectedE) event;
            if(e.sender == this) {
                onSelectingEnd();
            }
        }
    }

    @UiHandler("cancel")
    void cancelClicked(ClickEvent event) {
        onSelectingEnd();
    }

    private void select() {
        videos.clear();
        IMasteryS.Instance.get().getYoutubeVideos(new SimpleAsyncCallback<ArrayList<VideoD>>() {
            @Override
            public void onSuccess(ArrayList<VideoD> result) {
                if (result != null) {
                    videos.clear();
                    onSelectingBegin();
                    for (VideoD video : result) {
                        if(video.embeddable && video.error == null) {
                            videos.add(new SelectableVideoV(video, VideoSelect.this));
                        }
                    }
                } else {
                    onSelectingEnd();
                }
            }
        });
    }

    private void onSelectingBegin() {
        inner.setVisible(true);
        if(onSelectingBegin != null)
            onSelectingBegin.onSuccess(null);
    }

    private void onSelectingEnd() {
        inner.setVisible(false);
        if(onSelectingEnd != null)
            onSelectingEnd.onSuccess(null);
    }

    public void setEnabled(boolean enabled) {
        if(!enabled)
            cancelClicked(null);
    }
}