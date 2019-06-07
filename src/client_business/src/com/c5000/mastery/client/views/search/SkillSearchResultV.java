package com.c5000.mastery.client.views.search;

import com.c5000.mastery.shared.ImageHelper;
import com.c5000.mastery.client.components.picture.PictureV;
import com.c5000.mastery.client.events.MasteryEvents;
import com.c5000.mastery.client.events.SkillChangedE;
import com.c5000.mastery.shared.data.base.SkillD;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.*;

public class SkillSearchResultV extends Composite implements MasteryEvents.Listener {
    interface ThisUiBinder extends UiBinder<Widget, SkillSearchResultV> {}
    private static ThisUiBinder uiBinder = GWT.create(ThisUiBinder.class);

    @UiField Anchor anchor;
    @UiField HTMLPanel content;
    @UiField HTMLPanel mouseOverEffect;
    @UiField Label title;
    @UiField Label description;
    @UiField PictureV picture;

    private SkillD skill;

    public SkillSearchResultV(SkillD skill) {
        this.skill = skill;
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
        anchor.setHref("#category=" + skill.id);

        title.setText(skill.title);
        picture.set(skill.picture, ImageHelper.Size.SMALL);
        description.setText(skill.description.resource);
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
        if (event instanceof SkillChangedE) {
            SkillD skill = ((SkillChangedE) event).skill;
            if (skill.id.equals(this.skill.id)) {
                this.skill = skill;
                bind();
            }
        }
    }

}