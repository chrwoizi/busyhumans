package com.c5000.mastery.client.views;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.*;

public class TermsOfServiceV extends Composite {
    interface ThisUiBinder extends UiBinder<Widget, TermsOfServiceV> {}
    private static ThisUiBinder uiBinder = GWT.create(ThisUiBinder.class);

    @UiField HTMLPanel english;
    @UiField HTMLPanel german;
    @UiField Label hintEnglish;
    @UiField Label hintGerman;

    public TermsOfServiceV() {
        initWidget(uiBinder.createAndBindUi(this));
    }

    protected void onAttach() {
        super.onAttach();
    }

    public void setLanguage(boolean isGerman) {
        english.setVisible(!isGerman);
        german.setVisible(isGerman);
    }

    public void setShowHint(boolean visible) {
        hintEnglish.setVisible(visible);
        hintGerman.setVisible(visible);
    }

}