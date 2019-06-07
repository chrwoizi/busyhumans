package com.c5000.mastery.client.views.activity;

import com.c5000.mastery.client.ElementHelper;
import com.c5000.mastery.client.events.MasteryEvents;
import com.c5000.mastery.client.events.NewContentBlockDeletedE;
import com.c5000.mastery.shared.data.base.ContentBlockD;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.ParagraphElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.*;

public class TextContentBlockV extends ContentBlockBaseV {
    interface ThisUiBinder extends UiBinder<Widget, TextContentBlockV> {}
    private static ThisUiBinder uiBinder = GWT.create(ThisUiBinder.class);

    @UiField HTMLPanel outer;
    @UiField ParagraphElement text;
    @UiField Button deleteButton;

    public TextContentBlockV(ContentBlockD block, boolean canDelete) {
        super(block, canDelete);
        initWidget(uiBinder.createAndBindUi(this));
    }

    protected void onAttach() {
        super.onAttach();
        bind();
    }

    private void bind() {
        text.setInnerHTML(ElementHelper.convertToAnchorsHtml(block.value.resource.resource, "target=\"_blank\""));
        deleteButton.setVisible(canDelete);
    }

    @UiHandler("deleteButton")
    void deleteButtonClicked(ClickEvent event) {
        MasteryEvents.dispatch(new NewContentBlockDeletedE(this));
    }

}