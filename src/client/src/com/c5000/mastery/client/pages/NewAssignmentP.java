package com.c5000.mastery.client.pages;

import com.c5000.mastery.client.auth.Auth;
import com.c5000.mastery.client.events.auth.MasteryAuthStatusChangedE;
import com.c5000.mastery.shared.data.auth.AuthStatus;
import com.c5000.mastery.client.events.AssignmentCreatedE;
import com.c5000.mastery.client.events.MasteryEvents;
import com.c5000.mastery.client.views.assignment.NewAssignmentV;
import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

public class NewAssignmentP extends Composite implements MasteryEvents.Listener {
    interface ThisUiBinder extends UiBinder<Widget, NewAssignmentP> {}
    private static ThisUiBinder uiBinder = GWT.create(ThisUiBinder.class);

    @UiField Label accessDenied;
    @UiField HTMLPanel accessGranted;
    @UiField HTMLPanel newAssignmentPanel;

    public NewAssignmentP() {
        initWidget(uiBinder.createAndBindUi(this));
    }

    protected void onAttach() {
        super.onAttach();
        MasteryEvents.subscribeWidget(this);
        if (Auth.user != null) {
            accessGranted.setVisible(true);
            accessDenied.setVisible(false);
            newAssignmentPanel.add(new NewAssignmentV());
        }
        else {
            accessGranted.setVisible(false);
            accessDenied.setVisible(true);
        }
    }

    @Override
    public void onEvent(MasteryEvents.MasteryEvent event) {
        if (event instanceof AssignmentCreatedE) {
            AssignmentCreatedE e = (AssignmentCreatedE) event;
            if (e.createdByThisUser) {
                History.newItem("assignment=" + e.assignment.assignment.id);
            }
        } else if (event instanceof MasteryAuthStatusChangedE) {
            accessGranted.setVisible(Auth.user != null);
            accessDenied.setVisible(Auth.user == null);
        }
    }

}