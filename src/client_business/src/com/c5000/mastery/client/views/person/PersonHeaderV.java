package com.c5000.mastery.client.views.person;

import com.c5000.mastery.client.DateConverter;
import com.c5000.mastery.shared.ImageHelper;
import com.c5000.mastery.client.components.picture.PictureV;
import com.c5000.mastery.client.events.MasteryEvents;
import com.c5000.mastery.client.events.PersonChangedE;
import com.c5000.mastery.shared.data.base.PersonD;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.*;

public class PersonHeaderV extends Composite implements MasteryEvents.Listener {
    interface ThisUiBinder extends UiBinder<Widget, PersonHeaderV> {}
    private static ThisUiBinder uiBinder = GWT.create(ThisUiBinder.class);

    @UiField Label name;
    @UiField PictureV picture;
    @UiField Label xp;
    @UiField Label level;
    @UiField Label levelXp;
    @UiField Label nextLevelXp;
    @UiField SimplePanel levelProgress;
    @UiField SimplePanel levelAntiProgress;
    @UiField Label joined;
    @UiField Label created;
    @UiField Label completed;
    @UiField HTMLPanel founder;

    private PersonD personData;

    public PersonHeaderV(PersonD personData) {
        this.personData = personData;
        initWidget(uiBinder.createAndBindUi(this));
    }

    protected void onAttach() {
        super.onAttach();
        MasteryEvents.subscribeWidget(this);
        picture.setSize(150, 150, true, true);
        bind();
    }

    private void bind() {
        name.setText(personData.name);
        picture.set(personData.picture, ImageHelper.Size.LARGE);

        level.setText("" + personData.level);

        xp.setText("" + personData.xp + " XP");
        levelXp.setText("" + personData.levelXp);
        nextLevelXp.setText("" + personData.nextLevelXp);

        int pct = (int) (personData.levelProgress * 100);
        levelProgress.getElement().getStyle().setWidth(pct, Style.Unit.PCT);
        levelAntiProgress.getElement().getStyle().setWidth(100 - pct, Style.Unit.PCT);

        joined.setText("" + DateConverter.toNaturalFormatPast(personData.joinDate)  + " ago");
        created.setText("" + personData.createdAssignments);
        completed.setText("" + personData.completedAssignments);

        founder.setVisible(personData.founder);
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
