package com.c5000.mastery.client.pages;

import com.c5000.mastery.client.SimpleAsyncCallback;
import com.c5000.mastery.client.views.LazyListV;
import com.c5000.mastery.client.views.assignment.AssignmentOverviewV;
import com.c5000.mastery.shared.data.base.SortBy;
import com.c5000.mastery.shared.data.combined.AssignmentVD;
import com.c5000.mastery.shared.services.IMasteryS;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.Widget;

import java.util.ArrayList;

public class DiscoverP extends Composite {
    interface ThisUiBinder extends UiBinder<Widget, DiscoverP> {}
    private static ThisUiBinder uiBinder = GWT.create(ThisUiBinder.class);

    @UiField LazyListV assignments;
    @UiField RadioButton sortByLatest;
    @UiField RadioButton sortByActivity;
    @UiField RadioButton sortByReward;

    private static SortBy sortBy = SortBy.ACTIVITY;

    public DiscoverP() {
        initWidget(uiBinder.createAndBindUi(this));
        assignments.loadOnAttach = false;
        assignments.setLoader(new LazyListV.Loader<AssignmentVD>() {
            @Override
            public void load(int offset, SimpleAsyncCallback<ArrayList<AssignmentVD>> callback) {
                IMasteryS.Instance.get().getActiveAssignments(offset, sortBy, callback);
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
        switch(sortBy) {
            case NEWEST:
                sortByLatest.setValue(true, false);
                updateSortBy();
                break;
            case ACTIVITY:
                sortByActivity.setValue(true, false);
                updateSortBy();
                break;
            case REWARD:
                sortByReward.setValue(true, false);
                updateSortBy();
                break;
        }
    }

    @UiHandler("sortByLatest")
    void sortByLatestClick(ClickEvent event) {
        updateSortBy();
    }


    @UiHandler("sortByActivity")
    void sortByActivityClick(ClickEvent event) {
        updateSortBy();
    }

    @UiHandler("sortByReward")
    void sortByRewardClick(ClickEvent event) {
        updateSortBy();
    }

    private void updateSortBy() {
        if(sortByActivity.getValue()) {
            sortBy = SortBy.ACTIVITY;
        }
        else if(sortByLatest.getValue()) {
            sortBy = SortBy.NEWEST;
        }
        else if(sortByReward.getValue()) {
            sortBy = SortBy.REWARD;
        }
        assignments.reset();
    }

}