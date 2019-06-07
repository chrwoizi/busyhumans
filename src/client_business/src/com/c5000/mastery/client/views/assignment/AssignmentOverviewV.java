package com.c5000.mastery.client.views.assignment;

import com.c5000.mastery.client.SimpleAsyncCallback;
import com.c5000.mastery.client.Sync;
import com.c5000.mastery.client.auth.Auth;
import com.c5000.mastery.client.components.picture.PictureV;
import com.c5000.mastery.client.events.*;
import com.c5000.mastery.shared.ImageHelper;
import com.c5000.mastery.shared.data.base.ActivityD;
import com.c5000.mastery.shared.data.base.PersonD;
import com.c5000.mastery.shared.data.combined.AssignmentVD;
import com.c5000.mastery.shared.services.IMasteryS;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.*;

public class AssignmentOverviewV extends Composite implements MasteryEvents.Listener {
    interface ThisUiBinder extends UiBinder<Widget, AssignmentOverviewV> {}
    private static ThisUiBinder uiBinder = GWT.create(ThisUiBinder.class);

    @UiField Anchor anchor;
    @UiField HTMLPanel content;
    @UiField HTMLPanel mouseOverEffect;
    @UiField Label title;
    @UiField PictureV skillPicture;
    @UiField Label description;
    @UiField Label reward;
    @UiField Label category;
    @UiField Label newActivities;
    @UiField Label activities;

    private AssignmentVD assignment;

    public AssignmentOverviewV(AssignmentVD assignment) {
        this.assignment = assignment;
        initWidget(uiBinder.createAndBindUi(this));
    }

    protected void onAttach() {
        super.onAttach();
        MasteryEvents.subscribeWidget(this);
        anchor.getElement().appendChild(content.getElement());
        skillPicture.setSize(120, 120, true, true);
        bind();
    }

    private void bind() {

        anchor.setHref("#assignment=" + assignment.assignment.id);

        skillPicture.set(assignment.assignment.picture, ImageHelper.Size.MEDIUM);
        title.setText(assignment.assignment.title);
        title.setVisible(true);

        description.setText(assignment.assignment.description);
        reward.setText(assignment.assignment.reward + " XP");
        category.setText(assignment.skill.title);

        int notRated = 0;
        for (ActivityD activity : assignment.activities) {
            if (Sync.serverTime().before(activity.rewardDueDate)) {
                if (activity.myRating == 0)
                    notRated++;
            }
        }
        int all = assignment.activities.size();
        if (all == 0) {
            newActivities.setVisible(false);
            activities.setVisible(true);
            activities.setText("no activity");
        } else {
            String text;
            String activityStr = " activit" + (all == 1 ? "y" : "ies");
            if (notRated > 0) {
                text = notRated + (notRated != all ? " / " + all : "") + activityStr + " to rate";
                activities.setVisible(false);
                newActivities.setVisible(true);
                newActivities.setText(text);
            }
            else {
                text = all + activityStr;
                newActivities.setVisible(false);
                activities.setVisible(true);
                activities.setText(text);
            }
        }
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
                if (Auth.user != null && getRewardedXp(this.assignment) == null && getRewardedXp(assignment) != null) {
                    IMasteryS.Instance.get().getPerson(Auth.user.personId, new SimpleAsyncCallback<PersonD>() {
                        @Override
                        public void onSuccess(PersonD result) {
                            MasteryEvents.dispatch(new PersonChangedE(result));
                        }
                    });
                }
                this.assignment = assignment;
                bind();
            }
        } else if (event instanceof ActivityCreatedE) {
            ActivityD activity = ((ActivityCreatedE) event).activity;
            if (activity.assignmentId.equals(assignment.assignment.id)) {
                IMasteryS.Instance.get().getAssignment(assignment.assignment.id, new SimpleAsyncCallback<AssignmentVD>() {
                    @Override
                    public void onSuccess(AssignmentVD result) {
                        assignment = result;
                        bind();
                    }
                });
            }
        } else if (event instanceof ActivityDeletedE) {
            ActivityD activity = ((ActivityDeletedE) event).activity;
            if (activity.assignmentId.equals(assignment.assignment.id)) {
                IMasteryS.Instance.get().getAssignment(assignment.assignment.id, new SimpleAsyncCallback<AssignmentVD>() {
                    @Override
                    public void onSuccess(AssignmentVD result) {
                        assignment = result;
                        bind();
                    }
                });
            }
        }
    }

    private Integer getRewardedXp(AssignmentVD assignment) {
        for (ActivityD activity : assignment.activities) {
            if (Auth.user != null && activity.authorId.equals(Auth.user.personId)) {
                return activity.rewarded;
            }
        }
        return null;
    }

}