package com.c5000.mastery.client;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Label;

public class ElementHelper {

    public static boolean isScrolledIntoView(Element element) {
        int docViewTop = Window.getScrollTop();
        int docViewBottom = docViewTop + Window.getClientHeight();

        int elemTop = element.getOffsetTop();
        int elemBottom = elemTop + element.getClientHeight();

        return elemBottom <= docViewBottom && elemTop >= docViewTop;
    }

    public static String convertToAnchorsHtml(String text, String anchorAttributes) {
        final String LINK_REGEX = "(\\b(?:(?:https?|ftp|file)://|www\\.|ftp\\.)[-a-zA-Z0-9+&@#/%=~_|$?!:,.]*[a-zA-Z0-9+&@#/%=~_|$])";
        final String LINK_REPLACEMENT = "<a href=\"$1\" " + anchorAttributes + ">$1</a>";

        String sanitized = new Label(text).getElement().getInnerHTML();
        String validUrls = sanitized.replace("&amp;", "&");
        return validUrls.replaceAll(LINK_REGEX, LINK_REPLACEMENT);
    }

}
