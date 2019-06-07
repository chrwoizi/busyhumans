package com.c5000.mastery.client.pages;

import com.c5000.mastery.client.SimpleAsyncCallback;
import com.c5000.mastery.client.auth.Auth;
import com.c5000.mastery.client.events.*;
import com.c5000.mastery.client.events.auth.MasteryAuthStatusChangedE;
import com.c5000.mastery.client.views.LazyListV;
import com.c5000.mastery.client.views.assignment.AssignmentOverviewV;
import com.c5000.mastery.client.views.person.PersonHeaderV;
import com.c5000.mastery.client.views.skill.AchievementV;
import com.c5000.mastery.shared.data.base.PersonD;
import com.c5000.mastery.shared.data.combined.AchievementVD;
import com.c5000.mastery.shared.data.combined.AssignmentVD;
import com.c5000.mastery.shared.services.IMasteryS;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.*;

import java.util.ArrayList;

public class PersonP extends Composite implements MasteryEvents.Listener {
    interface ThisUiBinder extends UiBinder<Widget, PersonP> {}
    private static ThisUiBinder uiBinder = GWT.create(ThisUiBinder.class);

    private enum SubPage {
        CREATED,
        PARTICIPATIONS,
        ACHIEVEMENTS
    }

    @UiField HTMLPanel found;
    @UiField HTMLPanel notFound;
    @UiField HTMLPanel headerPanel;

    @UiField ToggleButton createdHeader;
    @UiField ToggleButton participationsHeader;
    @UiField ToggleButton achievementsHeader;

    @UiField HTMLPanel createdContent;
    @UiField HTMLPanel participationsContent;
    @UiField HTMLPanel achievementsContent;

    @UiField LazyListV created;
    @UiField LazyListV participations;
    @UiField LazyListV achievements;

    private String personId;
    private PersonD personData;
    private boolean updateWhenPossible = true;

    private static SubPage activeSubPage = SubPage.PARTICIPATIONS;

    public PersonP(String personId) {
        this.personId = personId;
        initWidget(uiBinder.createAndBindUi(this));

        created.loadOnAttach = false;
        participations.loadOnAttach = false;
        achievements.loadOnAttach = false;

        created.setLoader(new LazyListV.Loader<AssignmentVD>() {
            @Override
            public void load(int offset, SimpleAsyncCallback<ArrayList<AssignmentVD>> callback) {
                IMasteryS.Instance.get().getCreatedAssignments(personData.id, offset, callback);
            }

            @Override
            public Widget visualize(AssignmentVD data) {
                return new AssignmentOverviewV(data);
            }

            @Override
            public String getId(AssignmentVD data) {
                return data.assignment.id;
            }
        });

        participations.setLoader(new LazyListV.Loader<AssignmentVD>() {
            @Override
            public void load(int offset, SimpleAsyncCallback<ArrayList<AssignmentVD>> callback) {
                IMasteryS.Instance.get().getCompletedAssignments(personData.id, offset, callback);
            }

            @Override
            public Widget visualize(AssignmentVD data) {
                return new AssignmentOverviewV(data);
            }

            @Override
            public String getId(AssignmentVD data) {
                return data.assignment.id;
            }
        });

        achievements.setLoader(new LazyListV.Loader<AchievementVD>() {
            @Override
            public void load(int offset, SimpleAsyncCallback<ArrayList<AchievementVD>> callback) {
                IMasteryS.Instance.get().getPersonAchievements(personData.id, offset, callback);
            }

            @Override
            public Widget visualize(AchievementVD data) {
                return new AchievementV(data);
            }

            @Override
            public String getId(AchievementVD data) {
                return data.id;
            }
        });
    }

    public boolean isMe() {
        return personId.equals("me") || (Auth.user != null && personId.equals(Auth.user.personId));
    }

    protected void onAttach() {
        super.onAttach();
        MasteryEvents.subscribeWidget(this);

        if (personId == null || "me".equals(personId)) {
            getMyself();
        } else {
            IMasteryS.Instance.get().getPerson(personId, new SimpleAsyncCallback<PersonD>() {
                @Override
                public void onSuccess(PersonD result) {
                    personData = result;
                    bind();
                }
            });
        }

        clickTabsByActiveSubPage();
    }

    private void getMyself() {
        if (Auth.user != null) {
            IMasteryS.Instance.get().getMyself(new SimpleAsyncCallback<PersonD>() {
                @Override
                public void onSuccess(PersonD result) {
                    personData = result;
                    bind();
                }
            });
        }
    }

    @UiHandler("createdHeader")
    void createdHeaderClicked(ClickEvent event) {
        activeSubPage = SubPage.CREATED;
        createdHeader.setDown(true);
        participationsHeader.setDown(false);
        achievementsHeader.setDown(false);
        createdContent.setVisible(true);
        participationsContent.setVisible(false);
        achievementsContent.setVisible(false);
        if(created.isVisible()) {
            created.reset();
        }
    }

    @UiHandler("participationsHeader")
    void participationsHeaderClicked(ClickEvent event) {
        activeSubPage = SubPage.PARTICIPATIONS;
        createdHeader.setDown(false);
        participationsHeader.setDown(true);
        achievementsHeader.setDown(false);
        createdContent.setVisible(false);
        participationsContent.setVisible(true);
        achievementsContent.setVisible(false);
        if(participations.isVisible()) {
            participations.reset();
        }
    }

    @UiHandler("achievementsHeader")
    void achievementsHeaderClicked(ClickEvent event) {
        activeSubPage = SubPage.ACHIEVEMENTS;
        createdHeader.setDown(false);
        participationsHeader.setDown(false);
        achievementsHeader.setDown(true);
        createdContent.setVisible(false);
        participationsContent.setVisible(false);
        achievementsContent.setVisible(true);
        if(achievements.isVisible()) {
            achievements.reset();
        }
    }

    private void bind() {
        headerPanel.clear();

        if (personData != null) {
            personId = personData.id;
            found.setVisible(true);
            notFound.setVisible(false);

            headerPanel.add(new PersonHeaderV(personData));
            tryShowContents();

        } else {
            personId = null;
            found.setVisible(false);
            notFound.setVisible(true);
        }
    }

    private void tryShowContents() {
        if (personData != null) {
            created.setVisible(true);
            participations.setVisible(true);
            achievements.setVisible(true);
            if(updateWhenPossible) {
                clickTabsByActiveSubPage();
            }
        } else {
            created.setVisible(false);
            participations.setVisible(false);
            achievements.setVisible(false);
            updateWhenPossible = true;
            if (personId == null || "me".equals(personId)) {
                getMyself();
            }
        }
    }

    private void clickTabsByActiveSubPage() {
        switch(activeSubPage) {
            case CREATED:
                createdHeader.setValue(true, false);
                createdHeaderClicked(null);
                break;
            case PARTICIPATIONS:
                participationsHeader.setValue(true, false);
                participationsHeaderClicked(null);
                break;
            case ACHIEVEMENTS:
                achievementsHeader.setValue(true, false);
                achievementsHeaderClicked(null);
                break;
        }
    }

    @Override
    public void onEvent(MasteryEvents.MasteryEvent event) {
        if (event instanceof MasteryAuthStatusChangedE) {
            tryShowContents();
        }
    }

}
