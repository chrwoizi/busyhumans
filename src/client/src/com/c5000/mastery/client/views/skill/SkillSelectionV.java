package com.c5000.mastery.client.views.skill;

import com.c5000.mastery.client.SimpleAsyncCallback;
import com.c5000.mastery.client.events.MasteryEvents;
import com.c5000.mastery.client.events.SelectedSkillChangedE;
import com.c5000.mastery.client.events.SkillSelectedE;
import com.c5000.mastery.shared.Sanitizer;
import com.c5000.mastery.shared.data.base.AssignmentCreationParamsD;
import com.c5000.mastery.shared.data.base.SkillD;
import com.c5000.mastery.shared.services.IMasteryS;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.*;

import java.util.ArrayList;

public class SkillSelectionV extends Composite implements MasteryEvents.Listener {

    interface ThisUiBinder extends UiBinder<Widget, SkillSelectionV> {}
    private static ThisUiBinder uiBinder = GWT.create(ThisUiBinder.class);

    @UiField HTMLPanel search;
    @UiField HTMLPanel selected;
    @UiField TextBox skill;
    @UiField HTMLPanel existingSkills;
    @UiField Button newSkill;
    @UiField HTMLPanel selectedSkillPanel;
    @UiField Label enterTerm;
    @UiField HTMLPanel noSkillsFound;
    @UiField Label loading;
    @UiField Button changeButton;

    private Timer skillChangedTimer;

    private SkillD selectedSkill;
    private NewSkillV newSkillV;

    public SkillSelectionV() {
        initWidget(uiBinder.createAndBindUi(this));
    }

    protected void onAttach() {
        super.onAttach();
        MasteryEvents.subscribeWidget(this);
        skill.setMaxLength(Sanitizer.MAX_SKILL_TITLE_LENGTH);
        onSkillTitleChanged();
    }

    public SkillD getSelectedSkill() {
        return selectedSkill;
    }

    public NewSkillV getNewSkill() {
        return newSkillV;
    }

    @UiHandler("newSkill")
    void newSkillClicked(ClickEvent event) {
        if (!Sanitizer.skillTitle(skill.getText()).isEmpty())
            createNewSkill();
    }

    @UiHandler("changeButton")
    void changeButtonClicked(ClickEvent event) {
        selected.setVisible(false);
        search.setVisible(true);
        MasteryEvents.dispatch(new SelectedSkillChangedE(false));
    }

    @UiHandler("skill")
    void onSkillKeyUp(KeyUpEvent event) {

        if (skillChangedTimer != null) {
            skillChangedTimer.cancel();
            skillChangedTimer = null;
        }
        skillChangedTimer = new Timer() {
            @Override
            public void run() {
                skillChangedTimer = null;
                onSkillTitleChanged();
            }
        };
        skillChangedTimer.schedule(250);
    }

    private void onSkillTitleChanged() {
        existingSkills.clear();
        String clean = Sanitizer.skillTitle(skill.getText());
        if (clean != null) {
            enterTerm.setVisible(false);
            noSkillsFound.setVisible(false);
            loading.setVisible(true);
            IMasteryS.Instance.get().suggestExistingSkills(clean, new SimpleAsyncCallback<ArrayList<SkillD>>() {

                @Override
                public void onSuccess(ArrayList<SkillD> result) {
                    existingSkills.clear();
                    loading.setVisible(false);
                    if (result.size() > 0) {
                        boolean exactMatch = false;
                        for (SkillD skill : result) {
                            existingSkills.add(new ExistingSkillV(skill, true));
                            if (skill.title.toLowerCase().equals(Sanitizer.skillTitle(SkillSelectionV.this.skill.getText()).toLowerCase())) {
                                exactMatch = true;
                            }
                        }
                        noSkillsFound.setVisible(!exactMatch);
                    } else {
                        noSkillsFound.setVisible(true);
                    }
                }
            });
        } else {
            loading.setVisible(false);
            noSkillsFound.setVisible(false);
            enterTerm.setVisible(true);
            clearSelectedSkill();
        }
    }

    public void setAssignmentParams(AssignmentCreationParamsD params) {
        if (selectedSkill != null) {
            params.skillId = selectedSkill.id;
        } else {
            params.skillId = null;
            params.newSkillParams = newSkillV.getParams();
        }
    }

    @Override
    public void onEvent(MasteryEvents.MasteryEvent event) {
        if (event instanceof SkillSelectedE) {
            useExistingSkill(((SkillSelectedE) event).skill);
        }
    }

    private void clearSelectedSkill() {
        newSkillV = null;
        selectedSkill = null;
        selectedSkillPanel.clear();
        MasteryEvents.dispatch(new SelectedSkillChangedE(false));
    }

    private void createNewSkill() {
        clearSelectedSkill();
        newSkillV = new NewSkillV();
        newSkillV.setTitle(Sanitizer.skillTitle(skill.getText()), true);
        selectedSkillPanel.add(newSkillV);
        search.setVisible(false);
        selected.setVisible(true);
        MasteryEvents.dispatch(new SelectedSkillChangedE(false));
    }

    private void useExistingSkill(SkillD skill) {
        clearSelectedSkill();
        selectedSkill = skill;
        selectedSkillPanel.add(new ExistingSkillV(selectedSkill, false));
        search.setVisible(false);
        selected.setVisible(true);
        MasteryEvents.dispatch(new SelectedSkillChangedE(true));
    }

    public void setEnabled(boolean enabled) {
        skill.setEnabled(enabled);
        newSkill.setEnabled(enabled);
        changeButton.setEnabled(enabled);
    }

}