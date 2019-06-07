package com.c5000.mastery.client.pages;

import com.c5000.mastery.client.SimpleAsyncCallback;
import com.c5000.mastery.client.auth.Auth;
import com.c5000.mastery.client.components.DisqusCommentsV;
import com.c5000.mastery.client.events.ActivityCreatedE;
import com.c5000.mastery.client.events.ActivityDeletedE;
import com.c5000.mastery.client.events.AssignmentChangedE;
import com.c5000.mastery.client.events.MasteryEvents;
import com.c5000.mastery.client.views.activity.PleaseLoginV;
import com.c5000.mastery.client.views.activity.ActivityV;
import com.c5000.mastery.client.views.activity.NewActivityV;
import com.c5000.mastery.client.views.assignment.AssignmentHeadV;
import com.c5000.mastery.shared.Config;
import com.c5000.mastery.shared.data.base.ActivityD;
import com.c5000.mastery.shared.data.base.PersonD;
import com.c5000.mastery.shared.data.combined.AssignmentVD;
import com.c5000.mastery.shared.services.IMasteryS;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;

public class AssignmentP extends Composite implements MasteryEvents.Listener {
    interface ThisUiBinder extends UiBinder<Widget, AssignmentP> {}
    private static ThisUiBinder uiBinder = GWT.create(ThisUiBinder.class);

    @UiField HTMLPanel found;
    @UiField HTMLPanel notFound;

    @UiField HTMLPanel assignmentPanel;
    @UiField HTMLPanel activitiesPanel;
    @UiField HTMLPanel activities;
    @UiField Label noActivities;
    @UiField ToggleButton subscribe;

    @UiField HTMLPanel commentsOuter;

    private String assignmentId;
    private AssignmentVD assignmentData;

    public AssignmentP(String assignmentId) {
        this.assignmentId = assignmentId;
        initWidget(uiBinder.createAndBindUi(this));
    }

    protected void onAttach() {
        super.onAttach();
        MasteryEvents.subscribeWidget(this);

        if (assignmentId != null) {
            IMasteryS.Instance.get().getAssignment(assignmentId, new SimpleAsyncCallback<AssignmentVD>() {
                @Override
                public void onSuccess(AssignmentVD result) {
                    assignmentData = result;
                    bind();
                }
            });
        }
    }

    private void bind() {
        assignmentPanel.clear();

        if (assignmentData != null) {
            assignmentId = assignmentData.assignment.id;

            assignmentPanel.add(new AssignmentHeadV(assignmentData));

            activitiesPanel.setVisible(!assignmentId.equals(Config.FOUNDER_ASSIGNMENT_ID));
            noActivities.setVisible(assignmentData.activities.isEmpty());
            activities.clear();
            for (ActivityD activity : assignmentData.activities) {
                activities.add(new ActivityV(assignmentData.persons.get(activity.authorId), activity));
            }
            if (Auth.user != null) {
                if (assignmentData.assignment.canComplete && !assignmentData.assignment.hasCompleted) {
                    activities.add(new NewActivityV(assignmentId, assignmentData.userPerson));
                }
            } else {
                activities.add(new PleaseLoginV("Please login to add your activity."));
            }

            setSubscribed(assignmentData.assignment.subscribed);

            found.setVisible(true);
            notFound.setVisible(false);

            commentsOuter.clear();
            //commentsOuter.add(new FacebookCommentsV("assignment=" + assignmentId));
            commentsOuter.add(new DisqusCommentsV("assignment=" + assignmentId, assignmentId, assignmentData.assignment.title));

        } else {
            assignmentId = null;
            found.setVisible(false);
            notFound.setVisible(true);
            commentsOuter.clear();
        }
    }

    private void setSubscribed(boolean value) {
        subscribe.setDown(value);
        if (value)
            subscribe.setTitle("You will be notified via email when a new activity is added.");
        else
            subscribe.setTitle("Click here if you want to be notified via email when a new activity is added.");
    }

    @UiHandler("subscribe")
    void subscribeClicked(ClickEvent event) {
        if (Auth.user == null) {
            Window.alert("Please login.");
            setSubscribed(false);
            return;
        }

        if (Auth.user.emailAddress == null) {
            Window.alert("Please assign an email address to your account. You can do that in the side bar on your right-hand side.");
            setSubscribed(false);
            return;
        }

        if (subscribe.isDown()) {
            setSubscribed(true);
            IMasteryS.Instance.get().subscribe(assignmentData.assignment.id, new SimpleAsyncCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    setSubscribed(true);
                }
            });
        } else {
            setSubscribed(false);
            IMasteryS.Instance.get().unsubscribe(assignmentData.assignment.id, new SimpleAsyncCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    setSubscribed(false);
                }
            });
        }
    }

    @Override
    public void onEvent(MasteryEvents.MasteryEvent event) {
        if (event instanceof AssignmentChangedE) {
            AssignmentVD assignment = ((AssignmentChangedE) event).assignment;
            if (assignment.assignment.id.equals(this.assignmentId)) {
                this.assignmentData = assignment;
                bind();
            }
        } else if (event instanceof ActivityCreatedE) {
            ActivityD activity = ((ActivityCreatedE) event).activity;
            PersonD author = ((ActivityCreatedE) event).author;
            if (activity.assignmentId.equals(this.assignmentId)) {
                if (activity.authorId.equals(assignmentData.userPerson.id))
                    assignmentData.assignment.hasCompleted = true;
                assignmentData.activities.add(activity);
                assignmentData.persons.put(author.id, author);
                bind();
            }
        } else if (event instanceof ActivityDeletedE) {
            ActivityD activity = ((ActivityDeletedE) event).activity;
            if (activity.assignmentId.equals(this.assignmentId)) {
                for (ActivityD item : assignmentData.activities) {
                    if (item.id.equals(activity.id)) {
                        if (activity.authorId.equals(assignmentData.userPerson.id))
                            assignmentData.assignment.hasCompleted = false;
                        assignmentData.activities.remove(activity);
                        break;
                    }
                }
                bind();
            }
        }
    }

}
