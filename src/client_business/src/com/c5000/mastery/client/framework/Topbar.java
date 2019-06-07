package com.c5000.mastery.client.framework;

import com.c5000.mastery.client.auth.Auth;
import com.c5000.mastery.client.events.MasteryEvents;
import com.c5000.mastery.client.events.ShowPageE;
import com.c5000.mastery.client.events.auth.MasteryAuthStatusChangedE;
import com.c5000.mastery.client.pages.AboutP;
import com.c5000.mastery.client.pages.AdminP;
import com.c5000.mastery.client.pages.DiscoverP;
import com.c5000.mastery.client.pages.NewAssignmentP;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;

public class Topbar extends Composite implements MasteryEvents.Listener {
    interface ThisUiBinder extends UiBinder<Widget, Topbar> {}
    private static ThisUiBinder uiBinder = GWT.create(ThisUiBinder.class);

    public static final int INNER_HEIGHT_PX = 36;
    public static final String INNER_HEIGHT_CSS = INNER_HEIGHT_PX + "px";

    public static final int HEIGHT_PX = INNER_HEIGHT_PX;
    public static final String HEIGHT_CSS = HEIGHT_PX + "px";

    @UiField HTMLPanel outer;
    @UiField Image logoImage;
    @UiField Anchor about;
    @UiField Anchor discover;
    @UiField Anchor create;
    @UiField Anchor admin;

    public Topbar() {
        initWidget(uiBinder.createAndBindUi(this));
    }

    protected void onAttach() {
        super.onAttach();
        MasteryEvents.subscribeWidget(this);

        admin.setVisible(Auth.user != null && Auth.user.isAdmin);
    }

    @UiHandler("logoImage")
    void onLogoImageClick(ClickEvent event) {
        String href = Window.Location.getHref();
        int tokenIndex = href.indexOf("#");
        if (tokenIndex >= 0) {
            href = href.substring(0, tokenIndex);
        }
        Window.Location.assign(href);
    }

    @UiHandler("about")
    void aboutClick(ClickEvent event) {
        HistoryHelper.navigateOrRefresh("about");
    }

    @UiHandler("discover")
    void discoverClick(ClickEvent event) {
        HistoryHelper.navigateOrRefresh("discover");
    }

    @UiHandler("create")
    void createClick(ClickEvent event) {
        HistoryHelper.navigateOrRefresh("create");
    }

    @UiHandler("admin")
    void adminClick(ClickEvent event) {
        HistoryHelper.navigateOrRefresh("admin");
    }

    @Override
    public void onEvent(MasteryEvents.MasteryEvent event) {
        if (event instanceof ShowPageE) {
            Composite page = ((ShowPageE) event).page;
            if (page instanceof AboutP) {
                about.addStyleName("topbar-anchor-active");
            } else {
                about.removeStyleName("topbar-anchor-active");
            }
            if (page instanceof DiscoverP) {
                discover.addStyleName("topbar-anchor-active");
            } else {
                discover.removeStyleName("topbar-anchor-active");
            }
            if (page instanceof NewAssignmentP) {
                create.addStyleName("topbar-anchor-active");
            } else {
                create.removeStyleName("topbar-anchor-active");
            }
            if (page instanceof AdminP) {
                admin.addStyleName("topbar-anchor-active");
            } else {
                admin.removeStyleName("topbar-anchor-active");
            }
        }
        else if(event instanceof MasteryAuthStatusChangedE) {
            admin.setVisible(Auth.user != null && Auth.user.isAdmin);
        }
    }

}