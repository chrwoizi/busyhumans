package com.c5000.mastery.client.pages;

import com.c5000.mastery.client.views.TermsOfServiceV;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;

public class LegalP extends Composite {
    interface ThisUiBinder extends UiBinder<Widget, LegalP> {}
    private static ThisUiBinder uiBinder = GWT.create(ThisUiBinder.class);

    @UiField ListBox languageSelection;
    @UiField HTMLPanel english;
    @UiField HTMLPanel german;
    @UiField TermsOfServiceV tos;

    public LegalP() {
        initWidget(uiBinder.createAndBindUi(this));
    }

    protected void onAttach() {
        super.onAttach();
        //languageSelection.addItem("English");
        languageSelection.addItem("German");
    }

    @UiHandler("languageSelection")
    void languageSelectionChanged(ChangeEvent event) {
        tos.setLanguage(languageSelection.isItemSelected(1));
        english.setVisible(languageSelection.isItemSelected(0));
        german.setVisible(languageSelection.isItemSelected(1));
    }

}