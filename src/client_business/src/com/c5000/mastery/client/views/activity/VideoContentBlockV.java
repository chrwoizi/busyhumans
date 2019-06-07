package com.c5000.mastery.client.views.activity;

import com.c5000.mastery.client.SimpleAsyncCallback;
import com.c5000.mastery.client.events.MasteryEvents;
import com.c5000.mastery.client.events.NewContentBlockDeletedE;
import com.c5000.mastery.shared.data.base.ContentBlockD;
import com.c5000.mastery.shared.data.base.VideoD;
import com.c5000.mastery.shared.services.IMasteryS;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.IFrameElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.*;

public class VideoContentBlockV extends ContentBlockBaseV {
    interface ThisUiBinder extends UiBinder<Widget, VideoContentBlockV> {}
    private static ThisUiBinder uiBinder = GWT.create(ThisUiBinder.class);

    @UiField HTMLPanel outer;
    @UiField IFrameElement video;
    @UiField Button deleteButton;
    @UiField HTMLPanel videoPanel;
    @UiField HTMLPanel statusPanel;
    @UiField Label status;
    @UiField Label title;

    public VideoContentBlockV(ContentBlockD block, boolean canDelete) {
        super(block, canDelete);
        initWidget(uiBinder.createAndBindUi(this));
    }

    private void checkReady() {
        IMasteryS.Instance.get().getVideo(block.value.resource.resource, new SimpleAsyncCallback<VideoD>() {
            @Override
            public void onSuccess(VideoD result) {
                if (result != null) {
                    block.ready = result.ready;
                    block.error = result.error;
                } else {
                    block.ready = false;
                    block.error = "Could not find the video on YouTube.";
                }
                bind();
                if (!block.ready && block.error == null) {
                    new Timer() {
                        @Override
                        public void run() {
                            checkReady();
                        }
                    }.schedule(5000);
                }
            }
        });
    }

    protected void onAttach() {
        super.onAttach();
        bind();
        if (!block.ready) {
            checkReady();
        }
    }

    private void bind() {
        if(block.authentic)
            title.setText("Video uploaded by author");
        else
            title.setText("Video added via URL");
        videoPanel.setVisible(block.ready);
        statusPanel.setVisible(!block.ready);
        if (block.ready) {
            video.setSrc("http://www.youtube.com/embed/" + block.value.resource.resource + "?autoplay=0&origin=http://busyhumans.com;");
        } else {
            if (block.error != null) {
                status.setText(block.error);
                status.getElement().getStyle().setColor("#FF0000");
            } else {
                status.setText("Please wait while the video is being processed by YouTube...");
                status.getElement().getStyle().setColor("#000000");
            }
        }
        deleteButton.setVisible(canDelete);
    }

    @UiHandler("deleteButton")
    void deleteButtonClicked(ClickEvent event) {
        MasteryEvents.dispatch(new NewContentBlockDeletedE(this));
    }

}