package com.c5000.mastery.client.framework;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class Bottom extends Composite {
    interface ThisUiBinder extends UiBinder<Widget, Bottom> {}
    private static ThisUiBinder uiBinder = GWT.create(ThisUiBinder.class);

    public static final int HEIGHT_PX = 50;
    public static final String HEIGHT_CSS = HEIGHT_PX + "px";
    public static final int BORDER_PX = 1;
    public static final String BORDER_CSS = BORDER_PX + "px";
    public static final int HEIGHT_TOTAL_PX = HEIGHT_PX + BORDER_PX;

    public Bottom() {
        initWidget(uiBinder.createAndBindUi(this));
    }

    protected void onAttach() {
        super.onAttach();
    }

}