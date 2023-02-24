package com.c5000.mastery.client.framework;

import com.c5000.mastery.shared.Config;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class MainV extends Composite {
    interface ThisUiBinder extends UiBinder<Widget, MainV> {}
    private static ThisUiBinder uiBinder = GWT.create(ThisUiBinder.class);

    public static final int WIDTH_PX = 900;
    public static final String WIDTH_CSS = WIDTH_PX + "px";

    @UiField Topbar topbar;
    @UiField PageContainer pageContainer;
    @UiField Bottom bottom;

    public MainV() {
        initWidget(uiBinder.createAndBindUi(this));

        // Facebook adds some url parameters in links to the facebook app. clear those but keep gwt codesvr.
        if(Window.Location.getParameterMap().size() > (Config.IS_LIVE ? 0 : 1)) {
            Window.Location.assign(Config.BASE_URL_GWT);
        }
    }

    protected void onAttach() {
        super.onAttach();
        updateHeight();
        Window.addResizeHandler(new ResizeHandler() {
            @Override
            public void onResize(ResizeEvent event) {
                updateHeight();
            }
        });
    }

    private void updateHeight() {
        int minHeight = Window.getClientHeight() - Topbar.HEIGHT_PX - PageContainer.PADDING_TOTAL_PX - Bottom.HEIGHT_TOTAL_PX;
        pageContainer.getElement().getStyle().setProperty("minHeight", minHeight + "px");
    }

}
