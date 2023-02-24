package com.c5000.mastery.client.framework;

import com.c5000.mastery.client.SimpleAsyncCallback;
import com.c5000.mastery.client.auth.Auth;
import com.c5000.mastery.client.components.WidgetToggleButton;
import com.c5000.mastery.client.components.picture.BorderlessPictureV;
import com.c5000.mastery.client.events.CloseLoginMenuE;
import com.c5000.mastery.client.events.MasteryEvents;
import com.c5000.mastery.client.events.ShowPageE;
import com.c5000.mastery.client.events.auth.MasteryAuthStatusChangedE;
import com.c5000.mastery.client.pages.PersonP;
import com.c5000.mastery.shared.ImageHelper;
import com.c5000.mastery.shared.data.base.PersonD;
import com.c5000.mastery.shared.services.IMasteryS;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.*;

public class AccountMenu extends Composite implements MasteryEvents.Listener {
    interface ThisUiBinder extends UiBinder<Widget, AccountMenu> {}
    private static ThisUiBinder uiBinder = GWT.create(ThisUiBinder.class);

    @UiField HTMLPanel loggedIn;
    @UiField HTMLPanel loggedOut;
    @UiField HTMLPanel denied;
    @UiField BorderlessPictureV avatar;
    @UiField Label name;
    @UiField WidgetToggleButton loginMenuButton;
    @UiField HTMLPanel myselfAnchorInner;
    @UiField Anchor myselfAnchor;
    @UiField LoginDropdown loginDropdown;
    @UiField Button preferencesButton;
    @UiField Button logoutButton;

    public AccountMenu() {
        initWidget(uiBinder.createAndBindUi(this));
        avatar.setSize(26, 26, true, true);
    }

    protected void onAttach() {
        super.onAttach();
        MasteryEvents.subscribeWidget(this);
        avatar.setLicenseVisibility(false);
        setVisibilities();
        myselfAnchor.getElement().appendChild(myselfAnchorInner.getElement());
    }

    private void setVisibilities() {
        loggedIn.setVisible(false);
        loggedOut.setVisible(false);
        denied.setVisible(false);
        switch (Auth.status) {
            case AUTHORIZED:
            case NEEDS_TOS_CONFIRMATION:
                loggedIn.setVisible(true);
                break;
            case NOT_AUTHORIZED:
                loggedOut.setVisible(true);
                break;
            case ACCESS_DENIED:
                denied.setVisible(true);
                break;
        }
        if (!loggedOut.isVisible()) {
            loginDropdown.setVisible(false);
            loginDropdown.reset();
            loginMenuButton.setDown(false);
        }
    }

    @Override
    public void onEvent(MasteryEvents.MasteryEvent event) {
        if (event instanceof MasteryAuthStatusChangedE) {
            setVisibilities();
            if (loggedIn.isVisible() && Auth.user != null) {
                IMasteryS.Instance.get().getPerson(Auth.user.personId, new SimpleAsyncCallback<PersonD>() {
                    @Override
                    public void onSuccess(PersonD result) {
                        avatar.set(result.picture, ImageHelper.Size.SMALL);
                        name.setText(result.name);
                    }
                });
            }
        } else if (event instanceof ShowPageE) {
            Composite page = ((ShowPageE) event).page;
            if (page instanceof PersonP && ((PersonP) page).isMe()) {
                myselfAnchor.addStyleName("topbar-anchor-active");
            } else {
                myselfAnchor.removeStyleName("topbar-anchor-active");
            }
        } else if (event instanceof CloseLoginMenuE) {
            loginDropdown.setVisible(false);
            loginDropdown.reset();
            loginMenuButton.setDown(false);
        }
    }

    @UiHandler("myselfAnchor")
    void myselfAnchorClick(ClickEvent event) {
        HistoryHelper.navigateOrRefresh("person=me");
    }

    @UiHandler("loginMenuButton")
    void loginMenuButtonClick(ClickEvent event) {
        loginDropdown.setVisible(loginMenuButton.isDown());
        loginDropdown.reset();
    }

    @UiHandler("logoutButton")
    void logoutButtonClick(ClickEvent event) {
        Auth.logout();
    }

    @UiHandler("preferencesButton")
    void preferencesButtonClick(ClickEvent event) {
        HistoryHelper.navigateOrRefresh("preferences");
    }

}