package com.c5000.mastery.client.components;

import com.c5000.mastery.shared.Config;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.AnchorElement;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.http.client.URL;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class AddThisV extends Composite {
    interface ThisUiBinder extends UiBinder<Widget, AddThisV> {}
    private static ThisUiBinder uiBinder = GWT.create(ThisUiBinder.class);

    @UiField DivElement toolbox;
    @UiField AnchorElement facebook;
    @UiField AnchorElement twitter;
    @UiField AnchorElement googleplus;
    @UiField AnchorElement share;

    private String historyToken;
    private boolean largeStyle;
    private String title;
    private String description;

    public AddThisV(String historyToken, boolean largeStyle) {
        initWidget(uiBinder.createAndBindUi(this));
        this.historyToken = historyToken;
        this.largeStyle = largeStyle;
    }

    public AddThisV(String historyToken, String title, String description, boolean largeStyle) {
        initWidget(uiBinder.createAndBindUi(this));
        this.historyToken = historyToken;
        this.largeStyle = largeStyle;
        this.title = title;
        this.description = description;
    }

    protected void onAttach() {
        super.onAttach();

        String url = historyToken != null ? Config.META_URL + "?token=" + URL.encodePathSegment(historyToken) : Config.BASE_URL_GWT;

        toolbox.setAttribute("addthis:url", url);
        toolbox.setAttribute("addthis:title", title);
        toolbox.setAttribute("addthis:description", description);

        if (largeStyle) {
            toolbox.addClassName("addthis_32x32_style");

            facebook.setAttribute("fb:like:layout", "box_count");
            facebook.getStyle().setMarginRight(5, Style.Unit.PX);

            twitter.setAttribute("tw:count", "vertical");
            twitter.getStyle().setMarginRight(5, Style.Unit.PX);

            googleplus.setAttribute("g:plusone:size", "tall");
            googleplus.getStyle().setMarginRight(5, Style.Unit.PX);

            share.addClassName("addthis_counter");
            createShareButton(share, url, title, description);
        } else {
            facebook.setAttribute("fb:like:layout", "button_count");
            facebook.getStyle().setProperty("minWidth", 100, Style.Unit.PX);

            googleplus.setAttribute("g:plusone:size", "medium");

            share.addClassName("addthis_counter");
            share.addClassName("addthis_pill_style");
            createShareButton(share, url, title, description);
        }

        initToolbox(toolbox);
    }

    private static native void createShareButton(Element shareAnchor, String url, String title, String description) /*-{
        if (title && title != null) {
            $wnd.addthis.counter(shareAnchor, $wnd.addthis_config, {"url":url, "title":title, "description":description});
        }
        else {
            $wnd.addthis.counter(shareAnchor, $wnd.addthis_config, {"url":url});
        }
    }-*/;

    private static native void initToolbox(Element shareToolbox) /*-{
        $wnd.addthis.toolbox(shareToolbox);
    }-*/;

}
