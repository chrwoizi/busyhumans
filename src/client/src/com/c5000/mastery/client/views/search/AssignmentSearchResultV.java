package com.c5000.mastery.client.views.search;

import com.c5000.mastery.shared.ImageHelper;
import com.c5000.mastery.client.components.picture.PictureV;
import com.c5000.mastery.client.events.AssignmentChangedE;
import com.c5000.mastery.client.events.MasteryEvents;
import com.c5000.mastery.shared.data.combined.AssignmentVD;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.*;

public class AssignmentSearchResultV extends Composite implements MasteryEvents.Listener {
    interface ThisUiBinder extends UiBinder<Widget, AssignmentSearchResultV> {}
    private static ThisUiBinder uiBinder = GWT.create(ThisUiBinder.class);

    @UiField Anchor anchor;
    @UiField HTMLPanel content;
    @UiField HTMLPanel mouseOverEffect;
    @UiField Label title;
    @UiField PictureV picture;
    @UiField Label reward;

    private AssignmentVD assignment;

    public AssignmentSearchResultV(AssignmentVD assignment) {
        this.assignment = assignment;
        initWidget(uiBinder.createAndBindUi(this));
    }

    protected void onAttach() {
        super.onAttach();
        MasteryEvents.subscribeWidget(this);
        anchor.getElement().appendChild(content.getElement());
        picture.setSize(50, 50, true, true);
        bind();
    }

    private void bind() {
        anchor.setHref("#assignment=" + assignment.assignment.id);

        title.setText(assignment.assignment.title);
        picture.set(assignment.assignment.picture, ImageHelper.Size.SMALL);
        reward.setText(assignment.assignment.reward + " XP");
    }

    @UiHandler("anchor")
    void anchorMouseEnter(MouseOverEvent event) {
        mouseOverEffect.setVisible(true);
    }

    @UiHandler("anchor")
    void anchorMouseOut(MouseOutEvent event) {
        mouseOverEffect.setVisible(false);
    }

    @Override
    public void onEvent(MasteryEvents.MasteryEvent event) {
        if (event instanceof AssignmentChangedE) {
            AssignmentVD assignment = ((AssignmentChangedE) event).assignment;
            if (assignment.assignment.id.equals(this.assignment.assignment.id)) {
                this.assignment = assignment;
                bind();
            }
        }
    }

}