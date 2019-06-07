package com.c5000.mastery.client.framework;

import com.c5000.mastery.client.SimpleAsyncCallback;
import com.c5000.mastery.client.auth.Auth;
import com.c5000.mastery.client.components.AddThisV;
import com.c5000.mastery.client.components.tagcloud.TagCloudV;
import com.c5000.mastery.client.events.NewAnnouncementE;
import com.c5000.mastery.client.events.MasteryEvents;
import com.c5000.mastery.client.events.NoMoreAnnouncementsE;
import com.c5000.mastery.client.events.auth.MasteryAuthStatusChangedE;
import com.c5000.mastery.client.views.account.EmailV;
import com.c5000.mastery.shared.data.base.TagD;
import com.c5000.mastery.shared.services.IMasteryS;
import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Widget;

import java.util.ArrayList;

public class SideBar extends Composite implements MasteryEvents.Listener {
    interface TopbarUiBinder extends UiBinder<Widget, SideBar> {}
    private static TopbarUiBinder uiBinder = GWT.create(TopbarUiBinder.class);

    public static final int PADDING_TOTAL_PX = 32;
    public static final String PADDING_CSS = (PADDING_TOTAL_PX / 2) + "px";

    public static final int PADDING_TOP_PX = Topbar.HEIGHT_PX + PADDING_TOTAL_PX / 2;
    public static final String PADDING_TOP_CSS = PADDING_TOP_PX + "px";

    public static final int WIDTH_PX = MainV.WIDTH_PX - PageContainer.WIDTH_PX - PageContainer.PADDING_TOTAL_PX - PageContainer.BORDER_WIDTH_PX;
    public static final String WIDTH_CSS = WIDTH_PX + "px";

    @UiField HTMLPanel email;
    @UiField TagCloudV categories;
    @UiField HTMLPanel social;
    @UiField HTMLPanel announcements;

    public SideBar() {
        initWidget(uiBinder.createAndBindUi(this));
    }

    protected void onAttach() {
        super.onAttach();
        MasteryEvents.subscribeWidget(this);

        social.add(new AddThisV(null, true));

        IMasteryS.Instance.get().getSkillTagCloud(new SimpleAsyncCallback<ArrayList<TagD>>() {
            @Override
            public void onSuccess(ArrayList<TagD> result) {
                categories.set(result, 7, 15);
            }
        });
    }

    private void bindEmail() {
        if (Auth.user != null && Auth.user.emailAddress == null) {
            email.setVisible(true);
        } else {
            email.setVisible(false);
        }
    }

    @Override
    public void onEvent(MasteryEvents.MasteryEvent event) {
        if(event instanceof NewAnnouncementE) {
            announcements.setVisible(true);
        }
        else if(event instanceof NoMoreAnnouncementsE) {
            announcements.setVisible(false);
        }
        else if(event instanceof MasteryAuthStatusChangedE) {
            bindEmail();
        }
    }

}
