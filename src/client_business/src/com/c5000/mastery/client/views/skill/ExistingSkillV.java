package com.c5000.mastery.client.views.skill;

import com.c5000.mastery.shared.ImageHelper;
import com.c5000.mastery.client.events.MasteryEvents;
import com.c5000.mastery.client.events.SkillSelectedE;
import com.c5000.mastery.shared.data.base.LicenseTypes;
import com.c5000.mastery.shared.data.base.SkillD;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.*;

public class ExistingSkillV extends Composite {
    interface ThisUiBinder extends UiBinder<Widget, ExistingSkillV> {}
    private static ThisUiBinder uiBinder = GWT.create(ThisUiBinder.class);

    @UiField FocusPanel focusPanel;
    @UiField HTMLPanel mouseOverEffect;
    @UiField Image picture;
    @UiField Label title;
    @UiField Label description;
    @UiField HTMLPanel descriptionLicense;
    @UiField Anchor descriptionLicenseAnchor;

    private SkillD skillData;
    private boolean selectable;

    public ExistingSkillV(SkillD skillData, boolean selectable) {
        this.skillData = skillData;
        this.selectable = selectable;
        initWidget(uiBinder.createAndBindUi(this));
    }

    protected void onAttach() {
        super.onAttach();
        bind();
    }

    private void bind() {
        focusPanel.setVisible(selectable);
        picture.setUrl(ImageHelper.getUrl(skillData.picture, ImageHelper.Size.SMALL));
        title.setText(skillData.title);
        description.setText(skillData.description.resource);
        if(skillData.description.license != LicenseTypes.INTERNAL_TEXT) {
            descriptionLicenseAnchor.setText(skillData.description.authorName);
            descriptionLicenseAnchor.setHref(skillData.description.authorUrl);
            descriptionLicense.setVisible(true);
        }
        else {
            descriptionLicense.setVisible(false);
        }
    }

    @UiHandler("focusPanel")
    void focusPanelMouseEnter(MouseOverEvent event) {
        mouseOverEffect.setVisible(true);
    }

    @UiHandler("focusPanel")
    void focusPanelMouseOut(MouseOutEvent event) {
        mouseOverEffect.setVisible(false);
    }

    @UiHandler("focusPanel")
    void focusPanelClicked(ClickEvent event) {
        MasteryEvents.dispatch(new SkillSelectedE(skillData));
    }

}