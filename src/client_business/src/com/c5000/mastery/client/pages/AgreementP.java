package com.c5000.mastery.client.pages;

import com.c5000.mastery.client.SimpleAsyncCallback;
import com.c5000.mastery.client.auth.Auth;
import com.c5000.mastery.client.views.TermsOfServiceV;
import com.c5000.mastery.shared.services.IMasteryS;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.*;

public class AgreementP extends Composite {
    interface ThisUiBinder extends UiBinder<Widget, AgreementP> {}
    private static ThisUiBinder uiBinder = GWT.create(ThisUiBinder.class);

    @UiField ListBox languageSelection;
    @UiField TermsOfServiceV tos;
    @UiField Button agree;

    public AgreementP() {
        initWidget(uiBinder.createAndBindUi(this));
    }

    protected void onAttach() {
        super.onAttach();
        languageSelection.addItem("English");
        languageSelection.addItem("German");
        languageSelectionChanged(null);
        tos.setShowHint(true);
    }

    @UiHandler("languageSelection")
    void languageSelectionChanged(ChangeEvent event) {
        tos.setLanguage(languageSelection.isItemSelected(1));
        agree.setText(languageSelection.isItemSelected(0) ? "Agree" : "Zustimmen");
    }

    @UiHandler("agree")
    void agreeClicked(ClickEvent event) {
        IMasteryS.Instance.get().confirmTos(new SimpleAsyncCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Auth.reAuthAtMastery();
            }
        });
    }

}