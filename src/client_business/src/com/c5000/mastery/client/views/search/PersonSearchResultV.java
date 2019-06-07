package com.c5000.mastery.client.views.search;

import com.c5000.mastery.shared.ImageHelper;
import com.c5000.mastery.client.components.picture.PictureV;
import com.c5000.mastery.client.events.MasteryEvents;
import com.c5000.mastery.client.events.PersonChangedE;
import com.c5000.mastery.shared.data.base.PersonD;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.*;

public class PersonSearchResultV extends Composite implements MasteryEvents.Listener {
    interface ThisUiBinder extends UiBinder<Widget, PersonSearchResultV> {}
    private static ThisUiBinder uiBinder = GWT.create(ThisUiBinder.class);

    @UiField Anchor anchor;
    @UiField HTMLPanel content;
    @UiField HTMLPanel mouseOverEffect;
    @UiField Label name;
    @UiField PictureV picture;
    @UiField Label xp;
    @UiField Label level;

    private PersonD personData;

    public PersonSearchResultV(PersonD personData) {
        this.personData = personData;
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
        anchor.setHref("#person=" + personData.id);

        name.setText(personData.name);
        picture.set(personData.picture, ImageHelper.Size.SMALL);
        xp.setText("XP: " + personData.xp);

        level.setText("Level: " + personData.level);
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
        if (event instanceof PersonChangedE) {
            PersonD person = ((PersonChangedE) event).person;
            if (person.id.equals(this.personData.id)) {
                this.personData = person;
                bind();
            }
        }
    }

}
