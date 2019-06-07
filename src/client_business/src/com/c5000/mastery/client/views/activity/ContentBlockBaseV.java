package com.c5000.mastery.client.views.activity;

import com.c5000.mastery.client.events.MasteryEvents;
import com.c5000.mastery.client.events.NewContentBlockDeletedE;
import com.c5000.mastery.shared.data.base.ContentBlockD;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.*;

public class ContentBlockBaseV extends Composite {

    protected boolean canDelete;
    public ContentBlockD block;

    public ContentBlockBaseV(ContentBlockD block, boolean canDelete) {
        this.canDelete = canDelete;
        this.block = block;
    }

}