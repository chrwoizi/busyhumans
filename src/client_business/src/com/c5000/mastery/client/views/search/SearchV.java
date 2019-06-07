package com.c5000.mastery.client.views.search;

import com.c5000.mastery.client.SimpleAsyncCallback;
import com.c5000.mastery.shared.data.combined.SearchResultD;
import com.c5000.mastery.shared.data.combined.SearchResultsD;
import com.c5000.mastery.shared.services.IMasteryS;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.*;

public class SearchV extends Composite {
    interface TopbarUiBinder extends UiBinder<Widget, SearchV> {}
    private static TopbarUiBinder uiBinder = GWT.create(TopbarUiBinder.class);

    @UiField TextBox search;
    @UiField HTMLPanel results;
    @UiField Label hasMore;

    private Timer searchChangedTimer;

    public SearchV() {
        initWidget(uiBinder.createAndBindUi(this));
    }

    protected void onAttach() {
        super.onAttach();
    }

    @UiHandler("search")
    void searchKeyUp(KeyUpEvent event) {

        if (searchChangedTimer != null) {
            searchChangedTimer.cancel();
            searchChangedTimer = null;
        }
        searchChangedTimer = new Timer() {
            @Override
            public void run() {
                searchChangedTimer = null;
                onSearchChanged();
            }
        };
        searchChangedTimer.schedule(250);
    }

    private String getCleanSearch() {
        return search.getText().replace("\"", "").replace("\'", "").trim();
    }

    private void onSearchChanged() {
        results.clear();
        hasMore.setVisible(false);
        String search = getCleanSearch();
        if (!search.isEmpty()) {
            IMasteryS.Instance.get().search(search, new SimpleAsyncCallback<SearchResultsD>() {
                @Override
                public void onSuccess(SearchResultsD response) {
                    results.clear();
                    for (SearchResultD result : response.results) {
                        Widget view = null;
                        if (result.person != null) {
                            view = new PersonSearchResultV(result.person);
                        } else if (result.assignment != null) {
                            view = new AssignmentSearchResultV(result.assignment);
                        } else if (result.skill != null) {
                            view = new SkillSearchResultV(result.skill);
                        }
                        if(view != null) {
                            view.getElement().getStyle().setMarginBottom(10, Style.Unit.PX);
                            results.add(view);
                        }
                    }
                    hasMore.setVisible(response.hasMore);
                }
            });
        }
    }

}
