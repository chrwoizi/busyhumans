package com.c5000.mastery.client.views;

import com.c5000.mastery.client.ElementHelper;
import com.c5000.mastery.client.SimpleAsyncCallback;
import com.c5000.mastery.client.events.AssignmentDeletedE;
import com.c5000.mastery.client.events.MasteryEvents;
import com.c5000.mastery.client.events.SkillDeletedE;
import com.c5000.mastery.shared.Config;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@SuppressWarnings("unchecked")
public class LazyListV extends Composite implements MasteryEvents.Listener {
    interface ThisUiBinder extends UiBinder<Widget, LazyListV> {}
    private static ThisUiBinder uiBinder = GWT.create(ThisUiBinder.class);

    @UiField HTMLPanel container;
    @UiField HTMLPanel loading;
    @UiField Label empty;
    @UiField DivElement bottom;

    private HashMap<String, Widget> shown = new HashMap<String, Widget>();
    private List<String> loaded = new ArrayList<String>();
    private boolean couldHaveMore = true;
    private Loader loader;
    public boolean loadOnAttach = true;

    public static interface Loader<DataType> {
        public void load(int offset, SimpleAsyncCallback<ArrayList<DataType>> callback);

        public Widget visualize(DataType data);

        String getId(DataType data);
    }

    public static interface HasSeparator {
        public void setSeparatorVisibility(boolean visible);
    }

    public LazyListV() {
        initWidget(uiBinder.createAndBindUi(this));
    }

    public void setLoader(Loader loader) {
        this.loader = loader;
    }

    protected void onAttach() {
        super.onAttach();
        MasteryEvents.subscribeWidget(this);
        Window.addWindowScrollHandler(new Window.ScrollHandler() {
            @Override
            public void onWindowScroll(Window.ScrollEvent event) {
                // load more if the view is scrolled to the end
                if (ElementHelper.isScrolledIntoView(bottom)) {
                    load(Config.PAGE_SIZE);
                }
            }
        });
        if(loadOnAttach)
            load(Config.PAGE_SIZE);
    }

    public void reset() {
        container.clear();
        shown.clear();
        loaded.clear();
        couldHaveMore = true;
        load(Config.PAGE_SIZE);
    }

    void load(final int count) {
        if(!isVisible())
            return;
        if (loading.isVisible())
            return;
        if (!couldHaveMore)
            return;
        loading.setVisible(true);
        loader.load(loaded.size(), new SimpleAsyncCallback<ArrayList<Object>>() {
            @Override
            public void onSuccess(ArrayList<Object> result) {
                loading.setVisible(false);
                if (!result.isEmpty()) {
                    int before = shown.size();
                    addItems(result);
                    int after = shown.size();
                    int remaining = count - (after - before);
                    if (remaining > 0) {
                        load(remaining);
                    } else {
                        new Timer() {
                            @Override
                            public void run() {
                                // load more while the view is scrolled to the end
                                if (ElementHelper.isScrolledIntoView(bottom)) {
                                    load(Config.PAGE_SIZE);
                                }
                            }
                        }.schedule(1);
                    }
                } else {
                    couldHaveMore = false;
                }
            }
        });
    }

    private void addItems(ArrayList<Object> items) {
        for (Object item : items) {
            String id = loader.getId(item);
            loaded.add(id);
            if (!shown.containsKey(id)) {
                Widget v = loader.visualize(item);
                container.add(v);
                shown.put(id, v);
            }
        }

        for (int i = 0; i < container.getWidgetCount(); ++i) {
            Widget item = container.getWidget(i);
            if(item instanceof HasSeparator) {
                ((HasSeparator) item).setSeparatorVisibility(i < container.getWidgetCount() - 1);
            }
        }

        if (shown.size() > 0) {
            container.setVisible(true);
            empty.setVisible(false);
        } else {
            container.setVisible(false);
            empty.setVisible(true);
        }
    }

    @Override
    public void onEvent(MasteryEvents.MasteryEvent event) {
        if (event instanceof AssignmentDeletedE) {
            String id = ((AssignmentDeletedE) event).assignmentId;
            remove(id);
        } else if (event instanceof SkillDeletedE) {
            String id = ((SkillDeletedE) event).skillId;
            remove(id);
        }
    }

    public void remove(String id) {
        if (shown.containsKey(id)) {
            Widget existing = shown.get(id);
            container.remove(existing);
            shown.remove(id);
        }
    }
}