package com.c5000.mastery.client.views.assignment;

import com.c5000.mastery.client.ElementHelper;
import com.c5000.mastery.client.SimpleAsyncCallback;
import com.c5000.mastery.client.auth.Auth;
import com.c5000.mastery.client.components.AddThisV;
import com.c5000.mastery.client.components.WidgetCustomButton;
import com.c5000.mastery.client.components.WidgetToggleButton;
import com.c5000.mastery.client.components.picture.PictureV;
import com.c5000.mastery.client.events.*;
import com.c5000.mastery.shared.Config;
import com.c5000.mastery.shared.ImageHelper;
import com.c5000.mastery.shared.data.auth.AuthStatus;
import com.c5000.mastery.shared.data.base.ActivityD;
import com.c5000.mastery.shared.data.base.PersonD;
import com.c5000.mastery.shared.data.combined.AssignmentVD;
import com.c5000.mastery.shared.services.IMasteryS;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.ParagraphElement;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;

public class AssignmentHeadV extends Composite implements MasteryEvents.Listener {
    interface ThisUiBinder extends UiBinder<Widget, AssignmentHeadV> {}
    private static ThisUiBinder uiBinder = GWT.create(ThisUiBinder.class);

    @UiField Button delete;
    @UiField ToggleButton abuse;
    @UiField WidgetCustomButton clearAbuse;
    @UiField Label abuseReportCount;
    @UiField Button slowdown;
    @UiField Button speedup;
    @UiField Label title;
    @UiField PictureV skillPicture;
    @UiField ParagraphElement description;
    @UiField Label reward;
    @UiField Hyperlink authorLink;
    @UiField Label authorLabel;
    @UiField Hyperlink category;
    @UiField ToggleButton toggleRewardUp;
    @UiField ToggleButton toggleRewardDown;
    @UiField HTMLPanel socialOuter;

    private AssignmentVD assignment;
    private boolean isFounder;

    public AssignmentHeadV(AssignmentVD assignment) {
        this.assignment = assignment;
        initWidget(uiBinder.createAndBindUi(this));
    }

    protected void onAttach() {
        super.onAttach();
        MasteryEvents.subscribeWidget(this);
        skillPicture.setSize(120, 120, true, true);
        bind();
    }

    private void bind() {
        delete.setVisible(Auth.user != null && (Auth.user.isAdmin || (assignment.assignment.authorId.equals(Auth.user.personId)) && assignment.activities.isEmpty()));
        abuse.setVisible(Auth.user == null || (!delete.isVisible() && !assignment.assignment.authorId.equals(Auth.user.personId)));
        abuse.setDown(assignment.assignment.hasReportedAbuse);
        clearAbuse.setVisible(Auth.user != null && Auth.user.isAdmin && assignment.assignment.abuseReports > 0);
        abuseReportCount.setText("" + assignment.assignment.abuseReports);
        slowdown.setVisible(Auth.user != null && Auth.user.isAdmin);
        speedup.setVisible(Auth.user != null && Auth.user.isAdmin);

        skillPicture.set(assignment.assignment.picture, ImageHelper.Size.MEDIUM);
        title.setText(assignment.assignment.title);
        title.setVisible(true);

        description.setInnerHTML(ElementHelper.convertToAnchorsHtml(assignment.assignment.description, "target=\"_blank\" style=\"font-size: 10pt;\""));
        reward.setText(assignment.assignment.reward + " XP");
        category.setTargetHistoryToken("category=" + assignment.assignment.skillId);
        category.setText(assignment.skill.title);

        isFounder = assignment.assignment.id.equals(Config.FOUNDER_ASSIGNMENT_ID);

        boolean canUp = true;
        boolean canDown = true;
        boolean hasUp = false;
        boolean hasDown = false;

        if (Auth.user != null && assignment.userPerson != null) {
            canUp = !isFounder;
            canDown = !isFounder;
        }

        toggleRewardUp.setVisible(canUp);
        toggleRewardDown.setVisible(canDown);

        if (assignment.assignment.boosted != null && assignment.userPerson != null) {
            hasUp = assignment.assignment.boosted > 0;
            hasDown = assignment.assignment.boosted < 0;
        }

        if(canUp) {
            toggleRewardUp.setDown(hasUp);
            if(hasUp) {
                toggleRewardUp.setTitle("You have increased the reward by " + assignment.assignment.boosted + " XP.");
            } else {
                String by = assignment.userPerson != null ? " by " + assignment.userPerson.newAssignmentReward + " XP" : "";
                toggleRewardUp.setTitle("Click this button to increase the reward of this assignment" + by + "." +
                        " This will make it more likely for someone to complete the assignment." +
                        " Therefore you should activate this button if you like the assignment and want to see other people's performances.");
            }
        }

        if(canDown) {
            toggleRewardDown.setDown(hasDown);
            if(hasDown) {
                toggleRewardDown.setTitle("You have decreased the reward by " + (-assignment.assignment.boosted) + " XP.");
            } else {
                String by = assignment.userPerson != null ? " by " + assignment.userPerson.newAssignmentReward + " XP" : "";
                toggleRewardDown.setTitle("Click this button to decrease the reward of this assignment" + by + "." +
                        " This will make it less likely for someone to complete the assignment." +
                        " Therefore you should activate this button only if you think that the reward is too high.");
            }
        }

        PersonD authorPerson = assignment.persons.get(assignment.assignment.authorId);
        if (authorPerson.id.equals(Config.SYS_OBJ_ID)) {
            authorLabel.setText(authorPerson.name);
            authorLabel.setVisible(true);
            authorLink.setVisible(false);
        } else {
            authorLink.setText(authorPerson.name);
            authorLink.setTargetHistoryToken("person=" + authorPerson.id);
            authorLabel.setVisible(false);
            authorLink.setVisible(true);
        }

        if(Config.ENABLE_SOCIAL) {
            socialOuter.clear();
            //socialOuter.add(new FacebookLikeV("assignment=" + assignment.assignment.id));
            socialOuter.add(new AddThisV("assignment=" + assignment.assignment.id, assignment.assignment.title, assignment.assignment.description, false));
        }
    }

    @UiHandler("delete")
    void deleteClicked(ClickEvent event) {
        if (Window.confirm("Delete assignment '" + assignment.assignment.title + "'?")) {
            IMasteryS.Instance.get().deleteAssignment(assignment.assignment.id, new SimpleAsyncCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean result) {
                    if (result)
                        MasteryEvents.dispatch(new AssignmentDeletedE(assignment.assignment.id));
                }
            });
        }
    }

    @UiHandler("abuse")
    void abuseClicked(ClickEvent event) {
        if (Auth.user == null) {
            Window.alert("Please login.");
            abuse.setDown(false);
            return;
        }

        if(assignment.userPerson == null || isFounder) {
            toggleRewardUp.setDown(false);
            return;
        }

        IMasteryS.Instance.get().setAssignmentAbuseReport(assignment.assignment.id, !assignment.assignment.hasReportedAbuse, new SimpleAsyncCallback<AssignmentVD>() {
            @Override
            public void onSuccess(AssignmentVD result) {
                if (result != null) {
                    MasteryEvents.dispatch(new AssignmentChangedE(result));
                }
            }
        });
    }

    @UiHandler("clearAbuse")
    void clearAbuseClicked(ClickEvent event) {
        IMasteryS.Instance.get().clearAssignmentAbuseReports(assignment.assignment.id, new SimpleAsyncCallback<AssignmentVD>() {
            @Override
            public void onSuccess(AssignmentVD result) {
                if (result != null) {
                    MasteryEvents.dispatch(new AssignmentChangedE(result));
                }
            }
        });
    }

    @UiHandler("speedup")
    void speedupClicked(ClickEvent event) {
        IMasteryS.Instance.get().speedupAssignment(assignment.assignment.id, 1, new SimpleAsyncCallback<AssignmentVD>() {
            @Override
            public void onSuccess(AssignmentVD result) {
                if (result != null) {
                    MasteryEvents.dispatch(new AssignmentChangedE(result));
                }
            }
        });
    }

    @UiHandler("slowdown")
    void slowdownClicked(ClickEvent event) {
        IMasteryS.Instance.get().speedupAssignment(assignment.assignment.id, -1, new SimpleAsyncCallback<AssignmentVD>() {
            @Override
            public void onSuccess(AssignmentVD result) {
                if (result != null) {
                    MasteryEvents.dispatch(new AssignmentChangedE(result));
                }
            }
        });
    }

    @UiHandler("toggleRewardUp")
    void toggleRewardUpClicked(ClickEvent event) {
        if (Auth.user == null) {
            Window.alert("Please login.");
            toggleRewardUp.setDown(false);
            return;
        }

        if(assignment.userPerson == null || isFounder) {
            toggleRewardUp.setDown(false);
            return;
        }

        if (toggleRewardUp.isDown()) {
            IMasteryS.Instance.get().boostAssignment(assignment.assignment.id, 1, new SimpleAsyncCallback<AssignmentVD>() {
                @Override
                public void onSuccess(AssignmentVD result) {
                    if (result != null)
                        MasteryEvents.dispatch(new AssignmentChangedE(result));
                }
            });
            toggleRewardDown.setDown(false);
        } else {
            IMasteryS.Instance.get().boostAssignment(assignment.assignment.id, 0, new SimpleAsyncCallback<AssignmentVD>() {
                @Override
                public void onSuccess(AssignmentVD result) {
                    if (result != null)
                        MasteryEvents.dispatch(new AssignmentChangedE(result));
                }
            });
        }
    }

    @UiHandler("toggleRewardDown")
    void toggleRewardDownClicked(ClickEvent event) {
        if (Auth.user == null) {
            Window.alert("Please login.");
            toggleRewardUp.setDown(false);
            return;
        }

        if(assignment.userPerson == null || isFounder) {
            toggleRewardUp.setDown(false);
            return;
        }

        if (toggleRewardDown.isDown()) {
            IMasteryS.Instance.get().boostAssignment(assignment.assignment.id, -1, new SimpleAsyncCallback<AssignmentVD>() {
                @Override
                public void onSuccess(AssignmentVD result) {
                    if (result != null)
                        MasteryEvents.dispatch(new AssignmentChangedE(result));
                }
            });
            toggleRewardUp.setDown(false);
        } else {
            IMasteryS.Instance.get().boostAssignment(assignment.assignment.id, 0, new SimpleAsyncCallback<AssignmentVD>() {
                @Override
                public void onSuccess(AssignmentVD result) {
                    if (result != null)
                        MasteryEvents.dispatch(new AssignmentChangedE(result));
                }
            });
        }
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
        } else if (event instanceof AssignmentDeletedE) {
            String assignmentId = ((AssignmentDeletedE) event).assignmentId;
            if (assignmentId.equals(assignment.assignment.id)) {
                History.newItem("discover", true);
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