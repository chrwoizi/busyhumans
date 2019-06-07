package com.c5000.mastery.client.framework;

import com.c5000.mastery.client.api.Twitter.TwitterApi;
import com.c5000.mastery.client.api.facebook.FacebookApi;
import com.c5000.mastery.client.auth.Auth;
import com.c5000.mastery.client.events.CloseLoginMenuE;
import com.c5000.mastery.client.events.MasteryEvents;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.*;

public class LoginDropdown extends Composite {
    private static AccountDropdownUiBinder uiBinder = GWT.create(AccountDropdownUiBinder.class);
    interface AccountDropdownUiBinder extends UiBinder<Widget, LoginDropdown> {}

    @UiField Button facebook;
    @UiField Button twitter;
    @UiField HTMLPanel loginSelection;
    @UiField HTMLPanel anonLoginForm;
    @UiField Button anon;
    @UiField Button register;
    @UiField Button anonLogin;
    @UiField Button anonLoginCancel;
    @UiField TextBox username;
    @UiField PasswordTextBox password;

    public LoginDropdown() {
        initWidget(uiBinder.createAndBindUi(this));
    }

    @UiHandler("facebook")
    void facebookClicked(ClickEvent event) {
        FacebookApi.login();
    }

    @UiHandler("twitter")
    void twitterClicked(ClickEvent event) {
        TwitterApi.login();
    }

    @UiHandler("anon")
    void anonClicked(ClickEvent event) {
        loginSelection.setVisible(false);
        anonLoginForm.setVisible(true);
        username.setFocus(true);
    }

    @UiHandler("anonLoginCancel")
    void anonLoginCancelClicked(ClickEvent event) {
        loginSelection.setVisible(true);
        anonLoginForm.setVisible(false);
    }

    @UiHandler("anonLogin")
    void anonLoginClicked(ClickEvent event) {
        login();
    }

    private void login() {
        if(username.getText().isEmpty() || password.getText().isEmpty())
            return;
        Auth.anon.onCredentialChanged(username.getText(), password.getText());
        Auth.authAtMastery(Auth.anon);
        reset();
    }

    public void reset() {
        username.setText("");
        password.setText("");
        loginSelection.setVisible(true);
        anonLoginForm.setVisible(false);
    }

    @UiHandler("register")
    void registerClicked(ClickEvent event) {
        History.newItem("register", true);
        MasteryEvents.dispatch(new CloseLoginMenuE());
    }

    @UiHandler("username")
    void usernameKeyUp(KeyUpEvent event) {
        if(event.getNativeKeyCode() == 13)
            login();
    }

    @UiHandler("password")
    void passwordKeyUp(KeyUpEvent event) {
        if(event.getNativeKeyCode() == 13)
            login();
    }

}
