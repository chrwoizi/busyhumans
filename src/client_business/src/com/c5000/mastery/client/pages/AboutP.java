package com.c5000.mastery.client.pages;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class AboutP extends Composite {
    interface ThisUiBinder extends UiBinder<Widget, AboutP> {}
    private static ThisUiBinder uiBinder = GWT.create(ThisUiBinder.class);

    public AboutP() {
        initWidget(uiBinder.createAndBindUi(this));
    }

}