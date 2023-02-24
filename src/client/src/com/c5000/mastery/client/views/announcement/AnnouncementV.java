package com.c5000.mastery.client.views.announcement;

import com.c5000.mastery.client.DateConverter;
import com.c5000.mastery.client.SimpleAsyncCallback;
import com.c5000.mastery.client.Sync;
import com.c5000.mastery.client.events.AnnouncementClosedE;
import com.c5000.mastery.client.events.MasteryEvents;
import com.c5000.mastery.shared.data.base.AnnouncementD;
import com.c5000.mastery.shared.services.IMasteryS;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

public class AnnouncementV extends Composite {
    interface ThisUiBinder extends UiBinder<Widget, AnnouncementV> {}
    private static ThisUiBinder uiBinder = GWT.create(ThisUiBinder.class);

    @UiField Label text;
    @UiField Button close;

    private AnnouncementD data;
    private Timer timer;

    public AnnouncementV(AnnouncementD data) {
        this.data = data;
        initWidget(uiBinder.createAndBindUi(this));
    }

    protected void onAttach() {
        super.onAttach();
        bind();
        timer = new Timer() {
            @Override
            public void run() {
                bind();
            }
        };
        timer.scheduleRepeating(1000);
    }

    protected void onDetach() {
        super.onDetach();
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    private void bind() {
        text.setText(getRichText());
        close.setVisible(!data.maintenance || data.hideTime == null || Sync.serverTime().after(data.hideTime));
    }

    private String getRichText() {
        String result = data.text;
        if (data.showTime != null) {
            result = result.replace("{show}", DateConverter.toNaturalFormatPast(data.showTime) + " ago");
        }
        if (data.hideTime != null) {
            if (data.hideTime != null && Sync.serverTime().after(data.hideTime)) {
                result = result.replace("{hide}", "now");
            } else {
                result = result.replace("{hide}", "in " + DateConverter.toNaturalFormatFuture(data.hideTime));
            }
        }
        return result;
    }

    @UiHandler("close")
    void closeClicked(ClickEvent event) {
        MasteryEvents.dispatch(new AnnouncementClosedE(data));
        IMasteryS.Instance.get().announcementClosed(data.showTime, new SimpleAsyncCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
            }
        });
    }

}
