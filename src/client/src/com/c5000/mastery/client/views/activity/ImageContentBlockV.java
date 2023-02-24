package com.c5000.mastery.client.views.activity;

import com.c5000.mastery.shared.ImageHelper;
import com.c5000.mastery.client.components.picture.PictureV;
import com.c5000.mastery.client.events.MasteryEvents;
import com.c5000.mastery.client.events.NewContentBlockDeletedE;
import com.c5000.mastery.shared.data.base.ContentBlockD;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.*;

public class ImageContentBlockV extends ContentBlockBaseV {
    interface ThisUiBinder extends UiBinder<Widget, ImageContentBlockV> {}
    private static ThisUiBinder uiBinder = GWT.create(ThisUiBinder.class);

    @UiField HTMLPanel outer;
    @UiField PictureV image;
    @UiField Button deleteButton;

    public ImageContentBlockV(ContentBlockD block, boolean canDelete) {
        super(block, canDelete);
        initWidget(uiBinder.createAndBindUi(this));
    }

    protected void onAttach() {
        super.onAttach();
        bind();
    }

    private void bind() {
        image.setLink(ImageHelper.getUrl(block.value.resource, ImageHelper.Size.HIRES), "_blank");
        image.set(block.value.resource, ImageHelper.Size.LARGE);
        image.setNativeHeight(400);
        deleteButton.setVisible(canDelete);
    }

    @UiHandler("deleteButton")
    void deleteButtonClicked(ClickEvent event) {
        MasteryEvents.dispatch(new NewContentBlockDeletedE(this));
    }

}