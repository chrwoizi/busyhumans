package com.c5000.mastery.client;

import com.c5000.mastery.client.api.facebook.FacebookApi;
import com.c5000.mastery.client.auth.Auth;
import com.c5000.mastery.client.api.facebook.FBXfbml;
import com.c5000.mastery.client.events.MasteryEvents;
import com.c5000.mastery.client.events.NavigationE;
import com.c5000.mastery.client.events.auth.MasteryAuthStatusChangedE;
import com.c5000.mastery.client.framework.IMainV;
import com.c5000.mastery.client.framework.LoadingP;
import com.c5000.mastery.shared.data.base.InitResultD;
import com.c5000.mastery.shared.services.IMasteryS;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Entry point for the mastery module.
 */
public class MasteryEP implements EntryPoint, MasteryEvents.Listener {

    private LoadingP loading;

    public void onModuleLoad() {
        MasteryEvents.subscribeManually(this);

        History.addValueChangeHandler(new ValueChangeHandler<String>() {
            public void onValueChange(ValueChangeEvent<String> event) {
                navigate(event.getValue());
            }
        });

        loading = new LoadingP();
        RootPanel.get().add(loading);

        IMasteryS.Instance.get().init(Sync.clientTime().getTime(), new SimpleAsyncCallback<InitResultD>() {
            @Override
            public void onSuccess(InitResultD result) {
                Sync.syncClocks(result.clockSync);
                Sync.scheduleNext();
                RootPanel.get().remove(loading);
                RootPanel.get().add((Widget)GWT.create(IMainV.class));
                navigate(History.getToken());
            }
        });

        initApis();
    }

    private void navigate(String token) {
        MasteryEvents.dispatch(new NavigationE(token));
    }

    @Override
    public void onEvent(MasteryEvents.MasteryEvent event) {
        if (event instanceof MasteryAuthStatusChangedE) {
            FBXfbml.parse();
        }
    }

    public static void initApis() {
        FacebookApi.init(new SimpleAsyncCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Auth.init();
            }
        });
    }

}
