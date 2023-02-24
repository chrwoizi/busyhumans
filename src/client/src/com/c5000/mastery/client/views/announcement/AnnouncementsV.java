package com.c5000.mastery.client.views.announcement;

import com.c5000.mastery.client.events.AnnouncementClosedE;
import com.c5000.mastery.client.events.MasteryEvents;
import com.c5000.mastery.client.events.NewAnnouncementE;
import com.c5000.mastery.client.events.NoMoreAnnouncementsE;
import com.c5000.mastery.shared.data.base.AnnouncementD;
import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Widget;

import java.util.ArrayList;
import java.util.List;

public class AnnouncementsV extends Composite implements MasteryEvents.Listener {
    interface TopbarUiBinder extends UiBinder<Widget, AnnouncementsV> {}
    private static TopbarUiBinder uiBinder = GWT.create(TopbarUiBinder.class);

    @UiField HTMLPanel shown;

    private List<AnnouncementD> queue = new ArrayList<AnnouncementD>();

    public AnnouncementsV() {
        initWidget(uiBinder.createAndBindUi(this));
    }

    protected void onAttach() {
        super.onAttach();
        MasteryEvents.subscribeWidget(this);
    }

    @Override
    public void onEvent(MasteryEvents.MasteryEvent event) {
        if (event instanceof NewAnnouncementE) {
            queue.add(((NewAnnouncementE) event).announcement);
            if (shown.getWidgetCount() == 0 && queue.size() == 1)
                showNext();
        } else if (event instanceof AnnouncementClosedE) {
            showNext();
        }
    }

    private void showNext() {
        shown.clear();

        if (queue.isEmpty()) {
            MasteryEvents.dispatch(new NoMoreAnnouncementsE());
            return;
        }

        AnnouncementD next = queue.get(0);
        queue.remove(0);

        shown.add(new AnnouncementV(next));
    }

}
