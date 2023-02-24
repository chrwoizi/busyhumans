package com.c5000.mastery.client.views.account;

import com.c5000.mastery.client.Loggers;
import com.c5000.mastery.client.SimpleAsyncCallback;
import com.c5000.mastery.client.auth.Auth;
import com.c5000.mastery.client.auth.CredentialHelper;
import com.c5000.mastery.client.events.MasteryEvents;
import com.c5000.mastery.client.events.auth.MasteryAuthStatusChangedE;
import com.c5000.mastery.shared.Config;
import com.c5000.mastery.shared.data.auth.AuthProviderType;
import com.c5000.mastery.shared.data.auth.EmailSetResult;
import com.c5000.mastery.shared.services.IMasteryS;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;

public class EmailV extends Composite implements MasteryEvents.Listener {
    interface ThisUiBinder extends UiBinder<Widget, EmailV> {}
    private static ThisUiBinder uiBinder = GWT.create(ThisUiBinder.class);

    @UiField TextBox email;
    @UiField TextBox email2;
    @UiField PasswordTextBox password;
    @UiField HTMLPanel usernameHint;
    @UiField Button assign;
    @UiField HTMLPanel passwordHint;
    @UiField TableRowElement passwordOuter;

    public EmailV() {
        initWidget(uiBinder.createAndBindUi(this));
    }

    protected void onAttach() {
        super.onAttach();
        MasteryEvents.subscribeWidget(this);
        bind();
    }

    private void bind() {
        if (Auth.user != null && Auth.user.emailAddress == null) {
            passwordHint.setVisible(Auth.currentProvider == AuthProviderType.ANON);
            usernameHint.setVisible(Auth.currentProvider == AuthProviderType.ANON);
            passwordOuter.getStyle().setProperty("display", Auth.currentProvider == AuthProviderType.ANON ? "table-row" : "none");
        }
    }

    @Override
    public void onEvent(MasteryEvents.MasteryEvent event) {
        if (event instanceof MasteryAuthStatusChangedE) {
            bind();
        }
    }

    @UiHandler("assign")
    void assignClicked(ClickEvent event) {
        assign();
    }

    @UiHandler("email")
    void emailKeyDown(KeyDownEvent event) {
        if (event.getNativeKeyCode() == 13)
            assign();
    }

    @UiHandler("email2")
    void email2KeyDown(KeyDownEvent event) {
        if (event.getNativeKeyCode() == 13)
            assign();
    }

    @UiHandler("password")
    void passwordKeyDown(KeyDownEvent event) {
        if (event.getNativeKeyCode() == 13)
            assign();
    }

    private void assign() {
        final String emailVal = email.getText();
        String email2Val = email2.getText();
        if(!emailVal.equals(email2Val)) {
            Window.alert("Please enter the same email address in both fields.");
            return;
        }
        String passwordVal = password.getText();
        if (emailVal.matches(Config.EMAIL_REGEX)) {
            String oldPasswordHash = CredentialHelper.hashAnonPassword(Auth.user.username, passwordVal);
            String newPasswordHash = CredentialHelper.hashAnonPassword(emailVal, passwordVal);
            IMasteryS.Instance.get().setEmailAddress(emailVal, oldPasswordHash, newPasswordHash, new SimpleAsyncCallback<EmailSetResult>() {
                @Override
                public void onSuccess(EmailSetResult result) {
                    switch (result) {
                        case UNKNOWN:
                            Window.alert("An error occured. Please contact info@busyhumans.com");
                            email.setText("");
                            email2.setText("");
                            password.setText("");
                            break;
                        case WRONG_PASSWORD:
                            Window.alert("Wrong password.");
                            password.setText("");
                            break;
                        case OK:
                            email.setText("");
                            email2.setText("");
                            password.setText("");
                            if (Auth.user != null) {
                                if (Auth.currentProvider == AuthProviderType.ANON && Auth.user.emailAddress == null) {
                                    Window.alert("Your previous username has been replaced with the given email address. You can no longer login using the old username. Use your email address instead.");
                                }
                                Auth.user.emailAddress = emailVal;
                                MasteryEvents.dispatch(new MasteryAuthStatusChangedE());
                            }
                            break;
                    }
                }
            });
        } else {
            Window.alert("Please enter a valid email address.");
        }
    }

}
