package com.c5000.mastery.client.views.activity;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

public class PleaseLoginV extends Composite {
    interface ThisUiBinder extends UiBinder<Widget, PleaseLoginV> {}
    private static ThisUiBinder uiBinder = GWT.create(ThisUiBinder.class);

    @UiField Label label;

    public PleaseLoginV(String text) {
        initWidget(uiBinder.createAndBindUi(this));
        label.setText(text);
    }

    protected void onAttach() {
        super.onAttach();
    }

}
