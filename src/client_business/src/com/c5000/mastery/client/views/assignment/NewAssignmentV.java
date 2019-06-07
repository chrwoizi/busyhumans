package com.c5000.mastery.client.views.assignment;

import com.c5000.mastery.client.SimpleAsyncCallback;
import com.c5000.mastery.client.components.picture.PictureV;
import com.c5000.mastery.client.events.AssignmentCreatedE;
import com.c5000.mastery.client.events.MasteryEvents;
import com.c5000.mastery.client.events.PersonChangedE;
import com.c5000.mastery.client.events.SelectedSkillChangedE;
import com.c5000.mastery.client.views.skill.SkillSelectionV;
import com.c5000.mastery.shared.ImageHelper;
import com.c5000.mastery.shared.Sanitizer;
import com.c5000.mastery.shared.data.base.AssignmentCreationParamsD;
import com.c5000.mastery.shared.data.combined.AssignmentVD;
import com.c5000.mastery.shared.services.IMasteryS;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;

public class NewAssignmentV extends Composite implements MasteryEvents.Listener {
    interface ThisUiBinder extends UiBinder<Widget, NewAssignmentV> {}
    private static ThisUiBinder uiBinder = GWT.create(ThisUiBinder.class);

    @UiField SkillSelectionV skillSelection;

    @UiField HTMLPanel granted;
    @UiField HTMLPanel denied;
    @UiField TextBox title;
    @UiField TextArea description;
    @UiField Label reward;
    @UiField PictureV skillPicture;
    @UiField Button create;
    @UiField Label titleValidation;
    @UiField Label descriptionValidation;
    @UiField Label skillValidation;

    private boolean exists;

    public NewAssignmentV() {
        initWidget(uiBinder.createAndBindUi(this));
    }

    protected void onAttach() {
        super.onAttach();
        MasteryEvents.subscribeWidget(this);

        granted.setVisible(true);
        denied.setVisible(false);

        skillPicture.setSize(120, 120, true, true);

        reward.setText("...");
        IMasteryS.Instance.get().getNewAssignmentReward(new SimpleAsyncCallback<Integer>() {
            @Override
            public void onSuccess(Integer result) {
                reward.setText("" + result);
            }
        });

        title.setMaxLength(Sanitizer.MAX_ASSIGNMENT_TITLE_LENGTH);

        titleChanged(null);
        descriptionKeyUp(null);
        setSkillValid(false);
    }

    private void updateCreateButton() {
        boolean allValid = !(exists ||
                titleValidation.isVisible() ||
                descriptionValidation.isVisible() ||
                skillValidation.isVisible());
        create.setEnabled(allValid);
    }

    @UiHandler("title")
    void titleChanged(ChangeEvent event) {
        String capitalized = Sanitizer.capitalize(title.getText());
        if (!title.getText().equals(capitalized)) {
            title.setText(capitalized);
        }
        onTitleChanged();
    }

    @UiHandler("title")
    void titleKeyUp(KeyUpEvent event) {
        onTitleChanged();
    }

    private void onTitleChanged() {
        exists = false;
        String clean = Sanitizer.assignmentTitle(title.getText());
        if (clean == null) {
            titleValidation.setVisible(true);
            titleValidation.setText("Please enter a short title for the assignment.");
        } else {
            titleValidation.setVisible(false);
            checkExists(clean);
        }
        updateCreateButton();
    }

    private void checkExists(String title) {
        IMasteryS.Instance.get().assignmentExists(title, new SimpleAsyncCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                exists = result;
                if (exists) {
                    titleValidation.setVisible(true);
                    titleValidation.setText("An assigment with this title already exists.");
                } else {
                    titleValidation.setVisible(false);
                }
                updateCreateButton();
            }
        });
    }

    @UiHandler("description")
    void descriptionChanged(ChangeEvent event) {
        String capitalized = Sanitizer.capitalize(description.getText());
        if (!description.getText().equals(capitalized)) {
            description.setText(capitalized);
        }
        validateDescription();
    }

    @UiHandler("description")
    void descriptionKeyDown(KeyDownEvent event) {
        if (event != null && event.getNativeKeyCode() == 13) {
            event.preventDefault();
        }
    }

    @UiHandler("description")
    void descriptionKeyUp(KeyUpEvent event) {
        String text = description.getText();
        if (text.length() > Sanitizer.MAX_ASSIGNMENT_DESCRIPTION_LENGTH) {
            description.setText(text.substring(0, Sanitizer.MAX_ASSIGNMENT_DESCRIPTION_LENGTH));
        }
        validateDescription();
    }

    private void validateDescription() {
        description.setText(description.getText().replace("\r", "").replace("\n", " "));
        String clean = Sanitizer.assignmentDescription(description.getText());
        if (clean == null) {
            descriptionValidation.setVisible(true);
            descriptionValidation.setText("Please describe the assignment with up to " + Sanitizer.MAX_ASSIGNMENT_DESCRIPTION_LENGTH + " characters.");
        } else {
            descriptionValidation.setVisible(false);
        }
        updateCreateButton();
    }

    @UiHandler("create")
    void createClicked(ClickEvent event) {

        AssignmentCreationParamsD params = new AssignmentCreationParamsD();
        params.title = title.getText();
        params.description = description.getText();

        skillSelection.setAssignmentParams(params);

        IMasteryS.Instance.get().createAssignment(params, new SimpleAsyncCallback<AssignmentVD>() {
            @Override
            public void onSuccess(AssignmentVD result) {
                if (result != null) {
                    MasteryEvents.dispatch(new AssignmentCreatedE(result.persons.get(result.assignment.authorId), result, true));
                    MasteryEvents.dispatch(new PersonChangedE(result.persons.get(result.assignment.authorId)));
                } else {
                    Window.alert("An error occured. Please try again later.");
                }
            }
        });

        create.setEnabled(false);
        title.setEnabled(false);
        description.setEnabled(false);
        skillSelection.setEnabled(false);
        create.setText("Please wait...");
    }

    @Override
    public void onEvent(MasteryEvents.MasteryEvent event) {
        if (event instanceof SelectedSkillChangedE) {
            SelectedSkillChangedE e = (SelectedSkillChangedE) event;
            setSkillValid(e.isValid);
        }
    }

    private void setSkillValid(boolean valid) {
        if (valid) {
            if (skillSelection.getSelectedSkill() != null)
                skillPicture.set(skillSelection.getSelectedSkill().picture, ImageHelper.Size.MEDIUM);
            else
                skillPicture.set(skillSelection.getNewSkill().getPicture(), ImageHelper.Size.MEDIUM);
        } else {
            skillPicture.setDefault();
            skillValidation.setText("Please select or create a category.");
        }
        skillValidation.setVisible(!valid);
        updateCreateButton();
    }

}