package com.c5000.mastery.client.api.facebook;

import com.google.gwt.user.client.ui.Widget;

/**
 * Class that wraps xfbml methods
 *
 * @author ola the wrapper
 * @see http://developers.facebook.com/docs/reference/javascript/#xfbml-methods
 */
public class FBXfbml {

    /**
     * Wrapper method
     *
     * @see http://developers.facebook.com/docs/reference/javascript/FB.XFBML.parse
     */
    public static native void parse() /*-{
        if($wnd.FB)
            $wnd.FB.XFBML.parse();
    }-*/;

    /**
     * Wrapper method
     *
     * @widget widget to parse
     */
    public static void parse(Widget widget) {
        parse(widget.getElement().getId());
    }

    ;

    /**
     * Wrapper method
     *
     * @see http://developers.facebook.com/docs/reference/javascript/FB.XFBML.parse
     */
    public static native void parse(String domelementid) /*-{
        $wnd.FB.XFBML.parse(document.getElementById('domelementid'));
    }-*/;

}
