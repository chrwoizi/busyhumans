package com.c5000.mastery.client.views.person;

import com.c5000.mastery.client.DateConverter;
import com.c5000.mastery.shared.data.base.PersonAdminInfoD;
import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

public class PersonAdminInfoV extends Composite {
    interface ThisUiBinder extends UiBinder<Widget, PersonAdminInfoV> {}
    private static ThisUiBinder uiBinder = GWT.create(ThisUiBinder.class);

    @UiField Hyperlink name;
    @UiField Label auth;
    @UiField Label google;
    @UiField Label email;
    @UiField Label joined;
    @UiField Label login;
    @UiField Label score;

    private PersonAdminInfoD data;

    public PersonAdminInfoV(PersonAdminInfoD data) {
        this.data = data;
        initWidget(uiBinder.createAndBindUi(this));
    }

    protected void onAttach() {
        super.onAttach();
        bind();
    }

    private void bind() {
        name.setText(data.person.name);
        name.setTargetHistoryToken("person=" + data.person.id);
        auth.setText(data.loginMethod);
        google.setText(data.hasGoogleAuth ? "GOOGLE" : "no google");
        email.setText(data.hasEmail ? "EMAIL" : "no email");
        joined.setText("Joined " + DateConverter.toNaturalFormatPast(data.person.joinDate) + " ago");
        login.setText("Last login was " + DateConverter.toNaturalFormatPast(data.lastLogin) + " ago");
        score.setText(data.person.xp + "XP Level" + data.person.level);
    }

}
