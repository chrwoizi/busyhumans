package com.c5000.mastery.client.views.activity;

import com.c5000.mastery.client.DateConverter;
import com.c5000.mastery.client.SimpleAsyncCallback;
import com.c5000.mastery.client.Sync;
import com.c5000.mastery.client.auth.Auth;
import com.c5000.mastery.client.components.WidgetCustomButton;
import com.c5000.mastery.client.components.picture.PictureV;
import com.c5000.mastery.client.events.*;
import com.c5000.mastery.client.events.auth.MasteryAuthStatusChangedE;
import com.c5000.mastery.shared.ImageHelper;
import com.c5000.mastery.shared.data.base.*;
import com.c5000.mastery.shared.data.combined.ActivityDeletedVD;
import com.c5000.mastery.shared.data.combined.AssignmentVD;
import com.c5000.mastery.shared.services.IMasteryS;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;

public class ActivityV extends Composite implements MasteryEvents.Listener {
    interface ThisUiBinder extends UiBinder<Widget, ActivityV> {}
    private static ThisUiBinder uiBinder = GWT.create(ThisUiBinder.class);

    @UiField Button delete;
    @UiField ToggleButton abuse;
    @UiField WidgetCustomButton clearAbuse;
    @UiField Label abuseReportCount;
    @UiField HTMLPanel contentPanel;
    @UiField SimplePanel likes;
    @UiField SimplePanel dislikes;
    @UiField ToggleButton toggleLike;
    @UiField ToggleButton toggleDislike;
    @UiField Hyperlink authorName;
    @UiField PictureV authorPicture;
    @UiField Label reward;
    @UiField Label netRating;

    private PersonD author;
    private ActivityD activity;
    private AssignmentD assignment;

    public ActivityV(PersonD author, ActivityD activity) {
        this.author = author;
        this.activity = activity;
        initWidget(uiBinder.createAndBindUi(this));
        authorPicture.setSize(50, 50, true, true);
    }

    public ActivityV(PersonD author, AchievementActivityD activity) {
        this(author, activity.activity);
        this.assignment = activity.assignment;
    }

    protected void onAttach() {
        super.onAttach();
        MasteryEvents.subscribeWidget(this);
        bind();
    }

    private void bind() {
        delete.setVisible(activity.canDelete || (Auth.user != null && Auth.user.isAdmin));
        abuse.setVisible(Auth.user == null || (!activity.canDelete && !author.id.equals(Auth.user.personId)));
        abuse.setDown(activity.hasReportedAbuse);
        clearAbuse.setVisible(Auth.user != null && Auth.user.isAdmin && activity.abuseReports > 0);
        abuseReportCount.setText("" + activity.abuseReports);
        if (assignment != null) {
            authorPicture.setLink("#assignment=" + assignment.id, null);
            authorPicture.set(assignment.picture, ImageHelper.Size.SMALL);
            authorName.setText(assignment.title);
            authorName.setTargetHistoryToken("assignment=" + assignment.id);
        } else {
            authorPicture.setLink("#person=" + author.id, null);
            authorPicture.set(author.picture, ImageHelper.Size.SMALL);
            authorName.setText(author.name);
            authorName.setTargetHistoryToken("person=" + author.id);
        }
        contentPanel.clear();
        for (ContentBlockD block : activity.contentBlocks) {
            switch (block.typ) {
                case ContentBlockD.TYPE_TEXT:
                    contentPanel.add(new TextContentBlockV(block, false));
                    break;
                case ContentBlockD.TYPE_IMAGE:
                    contentPanel.add(new ImageContentBlockV(block, false));
                    break;
                case ContentBlockD.TYPE_VIDEO:
                    contentPanel.add(new VideoContentBlockV(block, false));
                    break;
            }
        }

        float ratings = activity.likes + activity.dislikes;
        if (ratings > 0) {
            float rating = activity.likes / ratings;
            int likesPercent = Math.round(100 * rating);
            int dislikesPercent = 100 - likesPercent;
            likes.setWidth(likesPercent + "%");
            dislikes.setWidth(dislikesPercent + "%");
        } else {
            likes.setWidth("50%");
            dislikes.setWidth("50%");
        }

        netRating.setText("" + (activity.likes - activity.dislikes));
        netRating.setTitle(activity.likes + " likes and " + activity.dislikes + " dislikes");

        toggleLike.setDown(activity.myRating == 1);
        toggleDislike.setDown(activity.myRating == -1);

        if (activity.rewarded != null) {
            if (activity.rewarded >= 0) {
                reward.setText("Gained " + activity.rewarded + " XP for this activity.");
            } else if (activity.rewarded < 0) {
                reward.setText("Lost " + (-activity.rewarded) + " XP for this activity.");
            }
        } else if (activity.rewardDueDate.after(Sync.serverTime())) {
            reward.setText("Rewards will be issued in " + DateConverter.toNaturalFormatFuture(activity.rewardDueDate));
        }
    }

    @Override
    public void onEvent(MasteryEvents.MasteryEvent event) {
        if (event instanceof MasteryAuthStatusChangedE) {
            bind();
        } else if (event instanceof ActivityChangedE) {
            ActivityD activity = ((ActivityChangedE) event).activity;
            if (activity.id.equals(this.activity.id)) {
                this.activity = activity;
                bind();
            }
        } else if (event instanceof AssignmentChangedE) {
            for (ActivityD activity : ((AssignmentChangedE) event).assignment.activities) {
                MasteryEvents.dispatch(new ActivityChangedE(activity));
            }
        }
    }

    @UiHandler("delete")
    void deleteClicked(ClickEvent event) {
        if (Window.confirm("Delete activity?")) {
            IMasteryS.Instance.get().deleteActivity(activity.id, new SimpleAsyncCallback<ActivityDeletedVD>() {
                @Override
                public void onSuccess(ActivityDeletedVD result) {
                    if (result != null) {
                        MasteryEvents.dispatch(new ActivityDeletedE(activity));
                        if (result.achievementDeleted) {
                            MasteryEvents.dispatch(new AchievementDeletedE(activity.achievementId));
                        }
                    }
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

        if (activity.canDelete || author.id.equals(Auth.user.personId)) {
            abuse.setDown(false);
            return;
        }

        IMasteryS.Instance.get().setActivityAbuseReport(activity.id, !activity.hasReportedAbuse, new SimpleAsyncCallback<ActivityD>() {
            @Override
            public void onSuccess(ActivityD result) {
                if (result != null) {
                    MasteryEvents.dispatch(new ActivityChangedE(result));
                }
            }
        });
    }

    @UiHandler("clearAbuse")
    void clearAbuseClicked(ClickEvent event) {
        IMasteryS.Instance.get().clearActivityAbuseReports(activity.id, new SimpleAsyncCallback<ActivityD>() {
            @Override
            public void onSuccess(ActivityD result) {
                if (result != null) {
                    MasteryEvents.dispatch(new ActivityChangedE(result));
                }
            }
        });
    }

    @UiHandler("toggleLike")
    void toggleLikeClicked(ClickEvent event) {
        if (Auth.user == null) {
            Window.alert("Please login.");
            toggleLike.setDown(false);
            return;
        }

        if (toggleLike.isDown()) {
            IMasteryS.Instance.get().rateActivity(activity.id, 1, new SimpleAsyncCallback<ActivityD>() {
                @Override
                public void onSuccess(ActivityD result) {
                    if (result != null)
                        MasteryEvents.dispatch(new ActivityChangedE(result));
                }
            });
            toggleDislike.setDown(false);
        } else {
            IMasteryS.Instance.get().rateActivity(activity.id, 0, new SimpleAsyncCallback<ActivityD>() {
                @Override
                public void onSuccess(ActivityD result) {
                    if (result != null)
                        MasteryEvents.dispatch(new ActivityChangedE(result));
                }
            });
        }
    }

    @UiHandler("toggleDislike")
    void toggleDislikeClicked(ClickEvent event) {
        if (Auth.user == null) {
            Window.alert("Please login.");
            toggleDislike.setDown(false);
            return;
        }

        if (toggleDislike.isDown()) {
            IMasteryS.Instance.get().rateActivity(activity.id, -1, new SimpleAsyncCallback<ActivityD>() {
                @Override
                public void onSuccess(ActivityD result) {
                    if (result != null)
                        MasteryEvents.dispatch(new ActivityChangedE(result));
                }
            });
            toggleLike.setDown(false);
        } else {
            IMasteryS.Instance.get().rateActivity(activity.id, 0, new SimpleAsyncCallback<ActivityD>() {
                @Override
                public void onSuccess(ActivityD result) {
                    if (result != null)
                        MasteryEvents.dispatch(new ActivityChangedE(result));
                }
            });
        }
    }

}