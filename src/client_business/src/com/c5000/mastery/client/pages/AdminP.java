package com.c5000.mastery.client.pages;

import com.c5000.mastery.client.SimpleAsyncCallback;
import com.c5000.mastery.client.Sync;
import com.c5000.mastery.client.auth.Auth;
import com.c5000.mastery.client.components.upload.FileUploadV;
import com.c5000.mastery.client.events.FileUploadCompleteE;
import com.c5000.mastery.client.events.MasteryEvents;
import com.c5000.mastery.client.views.activity.ActivityV;
import com.c5000.mastery.client.views.assignment.AssignmentHeadV;
import com.c5000.mastery.client.views.person.PersonAdminInfoV;
import com.c5000.mastery.client.views.skill.SkillV;
import com.c5000.mastery.shared.data.auth.CloakD;
import com.c5000.mastery.shared.data.base.PersonAdminInfoD;
import com.c5000.mastery.shared.data.base.SkillD;
import com.c5000.mastery.shared.data.combined.ActivityVD;
import com.c5000.mastery.shared.data.combined.AssignmentVD;
import com.c5000.mastery.shared.services.IMasteryS;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;

import java.util.ArrayList;

public class AdminP extends Composite implements MasteryEvents.Listener {
    interface ThisUiBinder extends UiBinder<Widget, AdminP> {}
    private static ThisUiBinder uiBinder = GWT.create(ThisUiBinder.class);

    @UiField ListBox cloaks;
    @UiField HTMLPanel innerDenied;
    @UiField HTMLPanel innerGranted;
    @UiField HTMLPanel assignments;
    @UiField HTMLPanel activities;
    @UiField HTMLPanel skills;
    @UiField HTMLPanel users;
    @UiField TextBox testPersonName;
    @UiField FileUploadV testPersonPicture;
    @UiField Button testPersonCreate;
    @UiField TextArea announcementText;
    @UiField TextBox announcementShowTime;
    @UiField TextBox announcementHideTime;
    @UiField CheckBox announcementMaintenance;
    @UiField Button announcementCreate;
    @UiField Label userCount;
    @UiField Button subscribeall;
    @UiField Button notifications;

    private String testPersonPictureId;

    public AdminP() {
        initWidget(uiBinder.createAndBindUi(this));
    }

    protected void onAttach() {
        super.onAttach();
        MasteryEvents.subscribeWidget(this);

        innerDenied.setVisible(Auth.user == null || !Auth.user.isAdmin);
        innerGranted.setVisible(Auth.user != null && Auth.user.isAdmin);

        bindCloaks();

        if (Auth.user != null && Auth.user.isAdmin) {

            IMasteryS.Instance.get().getAbusedAssignments(new SimpleAsyncCallback<ArrayList<AssignmentVD>>() {
                @Override
                public void onSuccess(ArrayList<AssignmentVD> result) {
                    assignments.clear();
                    if (result != null)
                        for (AssignmentVD assignment : result)
                            assignments.add(new AssignmentHeadV(assignment));
                }
            });

            IMasteryS.Instance.get().getAbusedActivities(new SimpleAsyncCallback<ArrayList<ActivityVD>>() {
                @Override
                public void onSuccess(ArrayList<ActivityVD> result) {
                    activities.clear();
                    if (result != null)
                        for (ActivityVD activity : result)
                            activities.add(new ActivityV(activity.author, activity.activity));
                }
            });

            IMasteryS.Instance.get().getAbusedSkills(new SimpleAsyncCallback<ArrayList<SkillD>>() {
                @Override
                public void onSuccess(ArrayList<SkillD> result) {
                    skills.clear();
                    if (result != null)
                        for (SkillD skill : result)
                            skills.add(new SkillV(skill));
                }
            });

            IMasteryS.Instance.get().getPersonAdminInfos(new SimpleAsyncCallback<ArrayList<PersonAdminInfoD>>() {
                @Override
                public void onSuccess(ArrayList<PersonAdminInfoD> result) {
                    users.clear();
                    if (result != null) {
                        userCount.setText("" + result.size());
                        for (PersonAdminInfoD user : result)
                            users.add(new PersonAdminInfoV(user));
                    } else {
                        userCount.setText("?");
                    }
                }
            });
        }
    }

    private void bindCloaks() {
        cloaks.clear();
        cloaks.addItem("myself", "");
        if (Auth.user != null && Auth.user.isAdmin) {
            for (int i = 0; i < Auth.user.cloaks.size(); i++) {
                CloakD cloak = Auth.user.cloaks.get(i);
                cloaks.addItem(cloak.name, cloak.personId);
                if (Auth.user.personId.equals(cloak.personId)) {
                    cloaks.setSelectedIndex(i + 1);
                }
            }
        }
    }

    @UiHandler("cloaks")
    void cloaksChanged(ChangeEvent event) {
        String cloak = null;
        if (cloaks.getSelectedIndex() >= 0) {
            cloak = cloaks.getValue(cloaks.getSelectedIndex());
            if (cloak.isEmpty())
                cloak = null;
        }
        final String fcloak = cloak;
        IMasteryS.Instance.get().setCloak(fcloak, new SimpleAsyncCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Window.Location.reload();
            }
        });
    }

    @UiHandler("testPersonCreate")
    void testPersonCreateClicked(ClickEvent event) {
        final String name = testPersonName.getText();
        if (!name.isEmpty() && testPersonPictureId != null) {
            testPersonName.setEnabled(false);
            testPersonPicture.setEnabled(false);
            testPersonCreate.setEnabled(false);
            IMasteryS.Instance.get().createTestPerson(name, testPersonPictureId, new SimpleAsyncCallback<String>() {
                @Override
                public void onSuccess(String personId) {
                    testPersonName.setText("");
                    testPersonPictureId = null;
                    testPersonName.setEnabled(true);
                    testPersonPicture.setEnabled(true);
                    testPersonCreate.setEnabled(true);
                    if (Auth.user != null) {
                        CloakD cloak = new CloakD();
                        cloak.personId = personId;
                        cloak.name = name;
                        Auth.user.cloaks.add(cloak);
                    }
                    bindCloaks();
                }
            });
        } else {
            Window.alert("Missing name or picture.");
        }
    }

    @UiHandler("announcementCreate")
    void announcementCreateClicked(ClickEvent event) {
        try {
            final String text = announcementText.getText();
            final int show = Integer.parseInt(announcementShowTime.getText());
            final Integer hide = announcementHideTime.getText().isEmpty() ? null : Integer.parseInt(announcementHideTime.getText());
            final boolean isMaintenance = announcementMaintenance.getValue();
            if (!text.isEmpty() && show >= 0 && (hide == null || hide > 0)) {
                announcementText.setEnabled(false);
                announcementShowTime.setEnabled(false);
                announcementHideTime.setEnabled(false);
                announcementMaintenance.setEnabled(false);
                announcementCreate.setEnabled(false);
                IMasteryS.Instance.get().createAnnouncement(text, show, hide, isMaintenance, new SimpleAsyncCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        announcementText.setText("");
                        announcementShowTime.setText("0");
                        announcementHideTime.setText("");
                        announcementMaintenance.setValue(false);
                        announcementText.setEnabled(true);
                        announcementShowTime.setEnabled(true);
                        announcementHideTime.setEnabled(true);
                        announcementMaintenance.setEnabled(true);
                        announcementCreate.setEnabled(true);
                        Sync.force();
                    }
                });
            } else {
                Window.alert("Missing text or due time.");
            }
        } catch (NumberFormatException ex) {
            Window.alert("Show/hide time must be a number.");
        }
    }

    @Override
    public void onEvent(MasteryEvents.MasteryEvent event) {
        if (event instanceof FileUploadCompleteE) {
            FileUploadCompleteE e = (FileUploadCompleteE) event;
            if (e.sender == testPersonPicture) {
                if (e.fileId != null) {
                    testPersonPictureId = e.fileId;
                }
            }
        }
    }

    @UiHandler("subscribeall")
    void subscribeallClicked(ClickEvent event) {
        IMasteryS.Instance.get().subscribeToAllAssignments(new SimpleAsyncCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
            }
        });
    }

    @UiHandler("notifications")
    void notificationsClicked(ClickEvent event) {
        IMasteryS.Instance.get().sendNotifications(new SimpleAsyncCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
            }
        });
    }
}