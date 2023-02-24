package com.c5000.mastery.client.pages;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class ImprintP extends Composite {
    interface ThisUiBinder extends UiBinder<Widget, ImprintP> {}
    private static ThisUiBinder uiBinder = GWT.create(ThisUiBinder.class);

    public ImprintP() {
        initWidget(uiBinder.createAndBindUi(this));
    }

    protected void onAttach() {
        super.onAttach();
    }

}