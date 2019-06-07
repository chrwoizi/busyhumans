package com.c5000.mastery.client.pages;

import com.c5000.mastery.client.SimpleAsyncCallback;
import com.c5000.mastery.client.auth.Auth;
import com.c5000.mastery.client.auth.CredentialHelper;
import com.c5000.mastery.client.events.MasteryEvents;
import com.c5000.mastery.client.events.auth.MasteryAuthStatusChangedE;
import com.c5000.mastery.shared.Config;
import com.c5000.mastery.shared.data.auth.AuthProviderType;
import com.c5000.mastery.shared.data.auth.AuthStatus;
import com.c5000.mastery.shared.data.auth.EmailSetResult;
import com.c5000.mastery.shared.data.base.PreferencesD;
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

public class PreferencesP extends Composite implements MasteryEvents.Listener {
    interface ThisUiBinder extends UiBinder<Widget, PreferencesP> {}
    private static ThisUiBinder uiBinder = GWT.create(ThisUiBinder.class);

    @UiField HTMLPanel denied;
    @UiField HTMLPanel granted;
    @UiField CheckBox subscribe;
    @UiField CheckBox activity;
    @UiField CheckBox activityReward;
    @UiField CheckBox otherActivityReward;
    @UiField Button save;
    @UiField CheckBox changeEmail;
    @UiField TextBox email;
    @UiField TextBox email2;
    @UiField TextBox password;
    @UiField TableRowElement passwordOuter;
    @UiField HTMLPanel usernameHint;

    private PreferencesD preferences;

    public PreferencesP() {
        initWidget(uiBinder.createAndBindUi(this));
        setEnabled(false);
    }

    @Override
    protected void onAttach() {
        super.onAttach();
        MasteryEvents.subscribeWidget(this);
        refresh();
    }

    private void refresh() {
        if (Auth.status == AuthStatus.AUTHORIZED) {
            denied.setVisible(false);
            granted.setVisible(true);
            IMasteryS.Instance.get().getPreferences(new SimpleAsyncCallback<PreferencesD>() {
                @Override
                public void onSuccess(PreferencesD result) {
                    preferences = result;
                    bind();
                    setEnabled(true);
                }
            });
        } else {
            granted.setVisible(false);
            denied.setVisible(true);
        }
    }

    private void bind() {
        subscribe.setValue(preferences.subscribeOwnAssignments);
        activity.setValue(preferences.notifyOtherActivity);
        activityReward.setValue(preferences.notifyActivityReward);
        otherActivityReward.setValue(preferences.notifyOtherActivityReward);

        usernameHint.setVisible(Auth.currentProvider == AuthProviderType.ANON && Auth.user.emailAddress == null);
        passwordOuter.getStyle().setProperty("display", Auth.currentProvider == AuthProviderType.ANON ? "table-row" : "none");
        if (Auth.user != null && Auth.user.emailAddress != null) {
            email.setText(Auth.user.emailAddress);
            email2.setText(Auth.user.emailAddress);
        } else {
            email.setText("");
            email2.setText("");
        }
    }

    @UiHandler("changeEmail")
    void changeEmailClicked(ClickEvent event) {
        email.setEnabled(changeEmail.getValue());
        email2.setEnabled(changeEmail.getValue());
        password.setEnabled(changeEmail.getValue());
    }

    @UiHandler("email")
    void emailKeyDown(KeyDownEvent event) {
        if (event.getNativeKeyCode() == 13)
            save();
    }

    @UiHandler("email2")
    void email2KeyDown(KeyDownEvent event) {
        if (event.getNativeKeyCode() == 13)
            save();
    }

    @UiHandler("password")
    void passwordKeyDown(KeyDownEvent event) {
        if (event.getNativeKeyCode() == 13)
            save();
    }

    @UiHandler("save")
    void saveClicked(ClickEvent event) {
        save();
    }

    private void save() {
        preferences.subscribeOwnAssignments = subscribe.getValue();
        preferences.notifyOtherActivity = activity.getValue();
        preferences.notifyActivityReward = activityReward.getValue();
        preferences.notifyOtherActivityReward = otherActivityReward.getValue();

        setEnabled(false);
        IMasteryS.Instance.get().setPreferences(preferences, new SimpleAsyncCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                setEnabled(true);
            }
        });

        if (changeEmail.getValue()) {
            final String emailVal = email.getText();
            String email2Val = email2.getText();
            if (emailVal.equals(email2Val)) {
                if (emailVal.matches(Config.EMAIL_REGEX)) {
                    String passwordVal = password.getText();
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
                                        if(Auth.currentProvider == AuthProviderType.ANON) {
                                            if (Auth.user.emailAddress == null) {
                                                Window.alert("Your previous username has been replaced with the given email address. You can no longer login using the old username. Use your email address instead.");
                                            }
                                            Auth.user.username = emailVal;
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
            } else {
                Window.alert("Please enter the same email address in both fields.");
            }
        }
    }

    private void setEnabled(boolean value) {
        subscribe.setEnabled(value);
        activity.setEnabled(value);
        activityReward.setEnabled(value);
        otherActivityReward.setEnabled(value);
        save.setEnabled(value);
    }

    @Override
    public void onEvent(MasteryEvents.MasteryEvent event) {
        if (event instanceof MasteryAuthStatusChangedE) {
            refresh();
        }
    }

}