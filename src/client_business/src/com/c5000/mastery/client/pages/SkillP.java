package com.c5000.mastery.client.pages;

import com.c5000.mastery.client.SimpleAsyncCallback;
import com.c5000.mastery.client.events.MasteryEvents;
import com.c5000.mastery.client.views.LazyListV;
import com.c5000.mastery.client.views.assignment.AssignmentOverviewV;
import com.c5000.mastery.client.views.skill.SkillV;
import com.c5000.mastery.shared.data.base.SkillD;
import com.c5000.mastery.shared.data.combined.AssignmentVD;
import com.c5000.mastery.shared.services.IMasteryS;
import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Widget;

import java.util.ArrayList;

public class SkillP extends Composite implements MasteryEvents.Listener {
    interface ThisUiBinder extends UiBinder<Widget, SkillP> {}
    private static ThisUiBinder uiBinder = GWT.create(ThisUiBinder.class);

    @UiField HTMLPanel found;
    @UiField HTMLPanel notFound;

    @UiField HTMLPanel skillPanel;
    @UiField LazyListV assignments;

    private String skillId;
    private SkillD skillData;

    public SkillP(String skillId) {
        this.skillId = skillId;
        initWidget(uiBinder.createAndBindUi(this));
        assignments.setLoader(new LazyListV.Loader<AssignmentVD>() {
            @Override
            public void load(int offset, final SimpleAsyncCallback<ArrayList<AssignmentVD>> callback) {
                IMasteryS.Instance.get().getAssignmentsBySkill(skillData.id, offset, callback);
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
    }

    protected void onAttach() {
        super.onAttach();
        MasteryEvents.subscribeWidget(this);
        if (skillId != null) {
            IMasteryS.Instance.get().getSkill(skillId, new SimpleAsyncCallback<SkillD>() {
                @Override
                public void onSuccess(SkillD result) {
                    skillData = result;
                    bind();
                }
            });
        }
    }

    private void bind() {
        skillPanel.clear();

        if (skillData != null) {
            skillId = skillData.id;

            SkillV view = new SkillV(skillData);
            skillPanel.add(view);

            assignments.setVisible(true);
            assignments.reset();

            found.setVisible(true);
            notFound.setVisible(false);

        } else {
            skillId = null;
            assignments.setVisible(false);
            found.setVisible(false);
            notFound.setVisible(true);
        }
    }

    @Override
    public void onEvent(MasteryEvents.MasteryEvent event) {
    }

}
