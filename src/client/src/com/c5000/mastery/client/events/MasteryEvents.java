package com.c5000.mastery.client.events;

import com.c5000.mastery.client.Loggers;
import com.c5000.mastery.shared.Config;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.event.logical.shared.AttachEvent.Handler;
import com.google.gwt.user.client.ui.Widget;

import java.util.ArrayList;
import java.util.List;

public class MasteryEvents {

    public static interface MasteryEvent {}

    public static interface Listener {
        public void onEvent(MasteryEvent event);
    }

    private static List<Listener> listeners = new ArrayList<Listener>();

    /**
     * Subscribes to an event.
     * The listener will automatically be unsubscribed when it (the widget) is detached.
     */
    public static <T extends Widget & Listener> void subscribeWidget(T listener) {
        listener.addAttachHandler(new Handler() {
            @Override
            public void onAttachOrDetach(AttachEvent event) {
                if (!event.isAttached()) {
                    unsubscribe((Listener) event.getSource());
                }
            }
        });
        listeners.add(listener);
    }

    public static void unsubscribe(Listener listener) {
        listeners.remove(listener);
    }

    /**
     * Subscribes to an event.
     * The listener will NOT be unsubscribed when it is destroyed (no weak-reference).
     */
    public static <T extends Listener> void subscribeManually(T listener) {
        assert !(listener instanceof Widget);
        listeners.add(listener);
    }

    public static void dispatch(MasteryEvent event) {
        Listener[] copy = listeners.toArray(new Listener[listeners.size()]);
        for (Listener listener : copy) {
            if(Config.IS_LIVE) {
                // redirect possible exceptions to browser console
                try {
                    listener.onEvent(event);
                }
                catch (Exception ex) {
                    Loggers.root.error("", ex);
                }
            }
            else {
                // show possible exceptions in IDE
                listener.onEvent(event);
            }
        }
    }
}
