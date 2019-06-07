package com.c5000.mastery.client.components;

import com.c5000.mastery.shared.Config;
import com.c5000.mastery.shared.PublicFacebookConfig;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.*;
import com.google.gwt.http.client.URL;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class FacebookLikeV extends Composite {
    interface ThisUiBinder extends UiBinder<Widget, FacebookLikeV> {}
    private static ThisUiBinder uiBinder = GWT.create(ThisUiBinder.class);

    private static final boolean SEND = false;
    private static final String LAYOUT = "button_count";
    private static final int WIDTH = 450;
    private static final boolean SHOW_FACES = false;
    private static final String ACTION = "like";
    private static final String COLOR_SCHEME = "light";
    private static final String FONT = "arial";
    private static final int HEIGHT = 21;

    @UiField IFrameElement iframe;

    public FacebookLikeV(String historyToken) {
        initWidget(uiBinder.createAndBindUi(this));
        String metaUrl = Config.META_URL + "?token=" + URL.encodePathSegment(historyToken);
        iframe.setSrc("//www.facebook.com/plugins/like.php"
                + "?href=" + URL.encodePathSegment(metaUrl)
                + "&send=" + SEND
                + "&layout=" + LAYOUT
                + "&width=" + WIDTH
                + "&show_faces=" + SHOW_FACES
                + "&action=" + ACTION
                + "&colorscheme=" + COLOR_SCHEME
                + "&font=" + FONT
                + "&height=" + HEIGHT
                + "&appId=" + PublicFacebookConfig.FACEBOOK_APP_ID);
    }

}
