package com.c5000.mastery.client.components.upload;

import com.c5000.mastery.client.events.MasteryEvents;
import com.c5000.mastery.client.events.VideoSelectedE;
import com.c5000.mastery.shared.data.base.VideoD;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;

public class SelectableVideoV extends Composite {
    interface ThisUiBinder extends UiBinder<Widget, SelectableVideoV> {}
    private static ThisUiBinder uiBinder = GWT.create(ThisUiBinder.class);

    @UiField FocusPanel outer;
    @UiField Image picture;

    private VideoD video;
    private VideoSelect parent;

    public SelectableVideoV(VideoD video, VideoSelect parent) {
        this.video = video;
        this.parent = parent;
        initWidget(uiBinder.createAndBindUi(this));
    }

    protected void onAttach() {
        super.onAttach();
        if(video.ready) {
            picture.setUrl("http://i2.ytimg.com/vi/" + video.id + "/default.jpg");
        }
        else {
            picture.setUrl("/static/default-video.png");
        }
        outer.setTitle(video.title);
    }

    @UiHandler("outer")
    void outerClicked(ClickEvent event) {
        MasteryEvents.dispatch(new VideoSelectedE(parent, video));
    }

}