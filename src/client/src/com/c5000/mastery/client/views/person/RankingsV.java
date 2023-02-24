package com.c5000.mastery.client.views.person;

import com.c5000.mastery.client.SimpleAsyncCallback;
import com.c5000.mastery.client.events.MasteryEvents;
import com.c5000.mastery.client.events.auth.MasteryAuthStatusChangedE;
import com.c5000.mastery.shared.data.combined.RankingD;
import com.c5000.mastery.shared.data.combined.RankingsD;
import com.c5000.mastery.shared.services.IMasteryS;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.*;

public class RankingsV extends Composite implements MasteryEvents.Listener {
    interface TopbarUiBinder extends UiBinder<Widget, RankingsV> {}
    private static TopbarUiBinder uiBinder = GWT.create(TopbarUiBinder.class);

    @UiField HTMLPanel persons;
    @UiField HTMLPanel myself;
    @UiField HTMLPanel myselfSpacer;
    @UiField Label empty;

    public RankingsV() {
        initWidget(uiBinder.createAndBindUi(this));
    }

    protected void onAttach() {
        super.onAttach();
        MasteryEvents.subscribeWidget(this);
        update();
    }

    private void update() {
        persons.clear();
        myself.clear();
        myselfSpacer.setVisible(false);
        IMasteryS.Instance.get().getRankings(new SimpleAsyncCallback<RankingsD>() {
            @Override
            public void onSuccess(RankingsD response) {
                persons.clear();
                myself.clear();
                myselfSpacer.setVisible(false);
                empty.setVisible(response.rankings.isEmpty());
                for (RankingD result : response.rankings) {
                    RankingV view = new RankingV(result);
                    view.getElement().getStyle().setMarginBottom(10, Style.Unit.PX);
                    if(result.rank > 5) {
                        myselfSpacer.setVisible(true);
                        myself.add(view);
                    }
                    else {
                        persons.add(view);
                    }
                }
            }
        });
    }

    @Override
    public void onEvent(MasteryEvents.MasteryEvent event) {
        if(event instanceof MasteryAuthStatusChangedE) {
            update();
        }
    }

}
