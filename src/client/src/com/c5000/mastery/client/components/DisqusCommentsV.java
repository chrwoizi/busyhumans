package com.c5000.mastery.client.components;

import com.c5000.mastery.shared.Config;
import com.c5000.mastery.shared.PublicDisqusConfig;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.http.client.URL;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Widget;

public class DisqusCommentsV extends Composite {
    interface ThisUiBinder extends UiBinder<Widget, DisqusCommentsV> {}
    private static ThisUiBinder uiBinder = GWT.create(ThisUiBinder.class);

    public DisqusCommentsV(String historyToken, String identifier, String title) {
        initWidget(uiBinder.createAndBindUi(this));
        String url = Config.META_URL + "?token=" + URL.encodePathSegment(historyToken);
        setParams(Config.IS_LIVE ? 0 : 1, PublicDisqusConfig.SHORT_NAME, url, identifier, title);
    }

    protected void onAttach() {
        super.onAttach();

        // DISQUS.reset is not yet supported as of 2012
        //reload(url, identifier, title);

        shutdown();
        new Timer() {
            @Override
            public void run() {
                init();
            }
        }.schedule(1);
    }

    protected void onDetach() {
        shutdown();
        super.onDetach();
    }

    private void init() {
        Element head = Document.get().getElementsByTagName("head").getItem(0);
        Element script = DOM.createElement("script");
        script.setAttribute("type", "text/javascript");
        script.setAttribute("src", "http://busyhumans.disqus.com/embed.js");
        head.appendChild(script);
        on();
    }

    private void shutdown() {
        Element head = Document.get().getElementsByTagName("head").getItem(0);
        NodeList<Element> scripts = head.getElementsByTagName("script");
        for (int i = 0; i < scripts.getLength(); ++i) {
            Element oldScript = scripts.getItem(i);
            if (oldScript.getAttribute("src").equals("http://busyhumans.disqus.com/embed.js")) {
                oldScript.removeFromParent();
                break;
            }
        }
        off();
    }

    private static native void on() /*-{
        if ($wnd.DISQUS) {
            $wnd.DISQUS.on();
        }
    }-*/;

    private static native void off() /*-{
        if ($wnd.DISQUS) {
            $wnd.DISQUS.off();
        }
    }-*/;

    private static native void setParams(int developer, String shortname, String url, String identifier, String title) /*-{
        $wnd.disqus_developer = developer;
        $wnd.disqus_shortname = shortname;
        $wnd.disqus_identifier = identifier;
        $wnd.disqus_url = url;
        $wnd.disqus_title = title;
    }-*/;

    private static native void reload(String url, String identifier, String title) /*-{
        var DISQUS = $wnd.DISQUS
        DISQUS.reset({
            reload:true,
            config:function () {
                this.page.identifier = identifier;
                this.page.url = url;
                this.page.title = title;
            }
        });
    }-*/;

}
