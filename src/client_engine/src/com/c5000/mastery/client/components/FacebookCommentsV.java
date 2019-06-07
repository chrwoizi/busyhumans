package com.c5000.mastery.client.components;

import com.c5000.mastery.client.api.facebook.FBXfbml;
import com.c5000.mastery.shared.Config;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.http.client.URL;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class FacebookCommentsV extends Composite {
    interface ThisUiBinder extends UiBinder<Widget, FacebookCommentsV> {}
    private static ThisUiBinder uiBinder = GWT.create(ThisUiBinder.class);

    private static final int ROOT_COUNT = 10;
    private static final int WIDTH = 580;

    @UiField DivElement fbdiv;

    public FacebookCommentsV(String historyToken) {
        initWidget(uiBinder.createAndBindUi(this));
        fbdiv.setAttribute("data-href", URL.encodePathSegment(Config.META_URL + "?token=" + URL.encodePathSegment(historyToken)));
        fbdiv.setAttribute("data-num-posts", "" + ROOT_COUNT);
        fbdiv.setAttribute("data-width", "" + WIDTH);
    }

    protected void onAttach() {
        super.onAttach();
        FBXfbml.parse(this);
    }

}
