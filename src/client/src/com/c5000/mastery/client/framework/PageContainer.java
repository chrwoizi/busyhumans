package com.c5000.mastery.client.framework;

import com.c5000.mastery.client.auth.Auth;
import com.c5000.mastery.client.events.MasteryEvents;
import com.c5000.mastery.client.events.MasteryEvents.Listener;
import com.c5000.mastery.client.events.MasteryEvents.MasteryEvent;
import com.c5000.mastery.client.events.NavigationE;
import com.c5000.mastery.client.events.RefreshPageE;
import com.c5000.mastery.client.events.ShowPageE;
import com.c5000.mastery.client.events.auth.MasteryAuthStatusChangedE;
import com.c5000.mastery.client.pages.*;
import com.c5000.mastery.shared.data.auth.AuthStatus;
import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Widget;

public class PageContainer extends Composite implements Listener {
    interface TopbarUiBinder extends UiBinder<Widget, PageContainer> {}
    private static TopbarUiBinder uiBinder = GWT.create(TopbarUiBinder.class);

    public static final int PADDING_TOTAL_PX = 32;
    public static final String PADDING_CSS = (PADDING_TOTAL_PX / 2) + "px";

    public static final int PADDING_TOP_PX = Topbar.HEIGHT_PX + PADDING_TOTAL_PX / 2;
    public static final String PADDING_TOP_CSS = PADDING_TOP_PX + "px";

    public static final int BORDER_WIDTH_PX = 1;
    public static final String BORDER_WIDTH_CSS = BORDER_WIDTH_PX + "px";

    public static final int WIDTH_PX = 600;
    public static final String WIDTH_CSS = WIDTH_PX + "px";

    @UiField HTMLPanel pageContainer;

    private String lastNavigationToken = "";

    public PageContainer() {
        initWidget(uiBinder.createAndBindUi(this));
    }

    protected void onAttach() {
        super.onAttach();
        MasteryEvents.subscribeWidget(this);
    }

    @Override
    public void onEvent(MasteryEvent event) {
        if (event instanceof NavigationE) {
            String token = ((NavigationE) event).token;
            lastNavigationToken = token;
            switch (Auth.status) {
                case NEEDS_TOS_CONFIRMATION:
                    MasteryEvents.dispatch(new ShowPageE(new AgreementP()));
                    break;
                default:
                    if (token.startsWith("about")) {
                        MasteryEvents.dispatch(new ShowPageE(new AboutP()));
                    } else if (token.startsWith("person=")) {
                        String personId = token.substring("person=".length());
                        MasteryEvents.dispatch(new ShowPageE(new PersonP(personId)));
                    } else if (token.startsWith("discover")) {
                        MasteryEvents.dispatch(new ShowPageE(new DiscoverP()));
                    } else if (token.startsWith("create")) {
                        MasteryEvents.dispatch(new ShowPageE(new NewAssignmentP()));
                    } else if (token.startsWith("assignment=")) {
                        String assignmentId = token.substring("assignment=".length());
                        MasteryEvents.dispatch(new ShowPageE(new AssignmentP(assignmentId)));
                    } else if (token.startsWith("category=")) {
                        String skillId = token.substring("category=".length());
                        MasteryEvents.dispatch(new ShowPageE(new SkillP(skillId)));
                    } else if (token.startsWith("imprint")) {
                        MasteryEvents.dispatch(new ShowPageE(new ImprintP()));
                    } else if (token.startsWith("legal")) {
                        MasteryEvents.dispatch(new ShowPageE(new LegalP()));
                    } else if (token.startsWith("admin")) {
                        MasteryEvents.dispatch(new ShowPageE(new AdminP()));
                    } else if (token.startsWith("register") && Auth.status == AuthStatus.NOT_AUTHORIZED) {
                        MasteryEvents.dispatch(new ShowPageE(new AnonRegisterP()));
                    } else if (token.startsWith("preferences")) {
                        MasteryEvents.dispatch(new ShowPageE(new PreferencesP()));
                    } else {
                        MasteryEvents.dispatch(new ShowPageE(new DiscoverP()));
                    }
                    break;
            }
        } else if (event instanceof RefreshPageE) {
            MasteryEvents.dispatch(new NavigationE(lastNavigationToken));
        } else if (event instanceof MasteryAuthStatusChangedE) {
            MasteryEvents.dispatch(new RefreshPageE());
        } else if (event instanceof ShowPageE) {
            Composite page = ((ShowPageE) event).page;
            pageContainer.clear();
            if (page != null) {
                pageContainer.add(page);
            }
        }
    }

}
