package com.c5000.mastery.client.views.skill;

import com.c5000.mastery.shared.ImageHelper;
import com.c5000.mastery.client.events.MasteryEvents;
import com.c5000.mastery.client.events.PictureSelectedE;
import com.c5000.mastery.shared.data.base.TokenizedResourceD;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.*;

public class SmallPictureV extends Composite {
    interface ThisUiBinder extends UiBinder<Widget, SmallPictureV> {}
    private static ThisUiBinder uiBinder = GWT.create(ThisUiBinder.class);

    @UiField FocusPanel focusPanel;
    @UiField HTMLPanel mouseOverEffect;
    @UiField Image picture;

    private TokenizedResourceD resource;

    public SmallPictureV(TokenizedResourceD resource) {
        this.resource = resource;
        initWidget(uiBinder.createAndBindUi(this));
    }

    protected void onAttach() {
        super.onAttach();
        picture.setUrl(ImageHelper.getUrl(resource.resource, ImageHelper.Size.SMALL));
    }

    @UiHandler("focusPanel")
    void focusPanelMouseEnter(MouseOverEvent event) {
        mouseOverEffect.setVisible(true);
    }

    @UiHandler("focusPanel")
    void focusPanelMouseOut(MouseOutEvent event) {
        mouseOverEffect.setVisible(false);
    }

    @UiHandler("focusPanel")
    void focusPanelClicked(ClickEvent event) {
        MasteryEvents.dispatch(new PictureSelectedE(resource));
    }

}