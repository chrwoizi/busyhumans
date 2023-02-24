package com.c5000.mastery.client.views.skill;

import com.c5000.mastery.client.ElementHelper;
import com.c5000.mastery.client.views.LazyListV;
import com.c5000.mastery.shared.ImageHelper;
import com.c5000.mastery.client.components.picture.PictureV;
import com.c5000.mastery.client.events.ActivityCreatedE;
import com.c5000.mastery.client.events.ActivityDeletedE;
import com.c5000.mastery.client.events.MasteryEvents;
import com.c5000.mastery.client.views.activity.ActivityV;
import com.c5000.mastery.shared.data.base.AchievementActivityD;
import com.c5000.mastery.shared.data.base.ActivityD;
import com.c5000.mastery.shared.data.base.LicenseTypes;
import com.c5000.mastery.shared.data.combined.AchievementVD;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.ParagraphElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.*;

public class AchievementV extends Composite implements MasteryEvents.Listener, LazyListV.HasSeparator {
    interface ThisUiBinder extends UiBinder<Widget, AchievementV> {}
    private static ThisUiBinder uiBinder = GWT.create(ThisUiBinder.class);

    @UiField PictureV picture;
    @UiField Hyperlink title;
    @UiField ParagraphElement description;
    @UiField HTMLPanel descriptionLicense;
    @UiField Anchor descriptionLicenseAnchor;
    @UiField HTMLPanel activities;
    @UiField Button expandButton;
    @UiField HTMLPanel expanded;
    @UiField SimplePanel separator;

    private AchievementVD achievement;

    public AchievementV(AchievementVD achievement) {
        this.achievement = achievement;
        initWidget(uiBinder.createAndBindUi(this));
    }

    @Override
    public void setSeparatorVisibility(boolean visible) {
        separator.setVisible(visible);
    }

    protected void onAttach() {
        super.onAttach();
        MasteryEvents.subscribeWidget(this);
        picture.setSize(120, 120, true, true);
        bind();
    }

    private void bind() {
        picture.set(achievement.skill.picture, ImageHelper.Size.MEDIUM);
        title.setText(achievement.skill.title);
        title.setTargetHistoryToken("category=" + achievement.skill.id);
        description.setInnerHTML(ElementHelper.convertToAnchorsHtml(achievement.skill.description.resource, "target=\"_blank\" style=\"font-size: 10pt;\""));
        if(achievement.skill.description.license != LicenseTypes.INTERNAL_TEXT) {
            descriptionLicenseAnchor.setText(achievement.skill.description.authorName);
            descriptionLicenseAnchor.setHref(achievement.skill.description.authorUrl);
            descriptionLicense.setVisible(true);
        }
        else {
            descriptionLicense.setVisible(false);
        }
        expandButtonClicked(null);
    }

    @UiHandler("expandButton")
    void expandButtonClicked(ClickEvent event) {
        if(expanded.isVisible()) {
            expanded.setVisible(false);
            expandButton.setText("v");

            activities.clear();
        }
        else {
            expanded.setVisible(true);
            expandButton.setText("^");

            activities.clear();
            for (AchievementActivityD activity : achievement.activities) {
                activities.add(new ActivityV(achievement.persons.get(activity.activity.authorId), activity));
            }
        }
    }

    @Override
    public void onEvent(MasteryEvents.MasteryEvent event) {
        if(event instanceof ActivityDeletedE) {
            ActivityD activity = ((ActivityDeletedE) event).activity;
            if(activity.achievementId.equals(achievement.id)) {
                for (AchievementActivityD a : achievement.activities) {
                    if(a.activity.id.equals(activity.id)) {
                        achievement.activities.remove(a);
                        bind();
                        break;
                    }
                }
            }
        }
    }

}