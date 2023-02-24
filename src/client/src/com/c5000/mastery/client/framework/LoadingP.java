package com.c5000.mastery.client.framework;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class LoadingP extends Composite {
    interface ThisUiBinder extends UiBinder<Widget, LoadingP> {}
    private static ThisUiBinder uiBinder = GWT.create(ThisUiBinder.class);

    public LoadingP() {
        initWidget(uiBinder.createAndBindUi(this));
    }
}