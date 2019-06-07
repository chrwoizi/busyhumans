package com.c5000.mastery.client.views.skill;

import com.c5000.mastery.client.ElementHelper;
import com.c5000.mastery.shared.ImageHelper;
import com.c5000.mastery.client.SimpleAsyncCallback;
import com.c5000.mastery.client.auth.Auth;
import com.c5000.mastery.client.components.WidgetCustomButton;
import com.c5000.mastery.client.components.picture.PictureV;
import com.c5000.mastery.client.events.*;
import com.c5000.mastery.shared.data.base.*;
import com.c5000.mastery.shared.services.IMasteryS;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.ParagraphElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;

public class SkillV extends Composite implements MasteryEvents.Listener {
    interface ThisUiBinder extends UiBinder<Widget, SkillV> {}
    private static ThisUiBinder uiBinder = GWT.create(ThisUiBinder.class);

    @UiField Button delete;
    @UiField ToggleButton abuse;
    @UiField WidgetCustomButton clearAbuse;
    @UiField Label abuseReportCount;
    @UiField Label title;
    @UiField PictureV picture;
    @UiField ParagraphElement description;

    @UiField HTMLPanel descriptionLicense;
    @UiField Anchor descriptionLicenseAnchor;

    private SkillD skillData;

    public SkillV(SkillD skillData) {
        this.skillData = skillData;
        initWidget(uiBinder.createAndBindUi(this));
    }

    protected void onAttach() {
        super.onAttach();
        MasteryEvents.subscribeWidget(this);
        picture.setSize(120, 120, true, true);
        bind();
    }

    private void bind() {
        delete.setVisible(Auth.user != null && Auth.user.isAdmin);
        abuse.setVisible(Auth.user == null || !Auth.user.isAdmin);
        abuse.setDown(skillData.hasReportedAbuse);
        clearAbuse.setVisible(Auth.user != null && Auth.user.isAdmin && skillData.abuseReports > 0);
        abuseReportCount.setText("" + skillData.abuseReports);

        title.setText(skillData.title);
        picture.set(skillData.picture, ImageHelper.Size.MEDIUM);

        description.setInnerHTML(ElementHelper.convertToAnchorsHtml(skillData.description.resource, "target=\"_blank\" style=\"font-size: 10pt;\""));
        if (skillData.description.license != LicenseTypes.INTERNAL_TEXT) {
            descriptionLicenseAnchor.setText(skillData.description.authorName);
            descriptionLicenseAnchor.setHref(skillData.description.authorUrl);
            descriptionLicense.setVisible(true);
        } else {
            descriptionLicense.setVisible(false);
        }
    }

    @UiHandler("delete")
    void deleteClicked(ClickEvent event) {
        if (Window.confirm("Delete category '" + skillData.title + "'? All Skills, Activities and Assignments that use this category will be lost! This will be applied to all persons!")) {
            IMasteryS.Instance.get().deleteSkill(skillData.id, new SimpleAsyncCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean result) {
                    if (result)
                        MasteryEvents.dispatch(new SkillDeletedE(skillData.id));
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

        IMasteryS.Instance.get().setSkillAbuseReport(skillData.id, !skillData.hasReportedAbuse, new SimpleAsyncCallback<SkillD>() {
            @Override
            public void onSuccess(SkillD result) {
                if (result != null) {
                    MasteryEvents.dispatch(new SkillChangedE(result));
                }
            }
        });
    }

    @UiHandler("clearAbuse")
    void clearAbuseClicked(ClickEvent event) {
        IMasteryS.Instance.get().clearSkillAbuseReports(skillData.id, new SimpleAsyncCallback<SkillD>() {
            @Override
            public void onSuccess(SkillD result) {
                if (result != null) {
                    MasteryEvents.dispatch(new SkillChangedE(result));
                }
            }
        });
    }

    @Override
    public void onEvent(MasteryEvents.MasteryEvent event) {
        if (event instanceof SkillChangedE) {
            SkillD skill = ((SkillChangedE) event).skill;
            if (skill.id.equals(this.skillData.id)) {
                this.skillData = skill;
                bind();
            }
        }
    }

}