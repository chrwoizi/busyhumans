package com.c5000.mastery.backend.google;

import com.google.gdata.data.ExtensionDescription;
import com.google.gdata.data.AbstractExtension;
import com.google.gdata.data.AttributeGenerator;
import com.google.gdata.data.AttributeHelper;
import com.google.gdata.data.youtube.YouTubeNamespace;
import com.google.gdata.util.ParseException;

/**
 * Object representation for the yt:accessControl tag
 */
@ExtensionDescription.Default(
        nsAlias = YouTubeNamespace.PREFIX,
        nsUri = YouTubeNamespace.URI,
        localName = "accessControl")
public class YtAccessControl extends AbstractExtension {

    public String action;
    public String permission;

    public YtAccessControl() {
    }

    public YtAccessControl(String action, String permission) {
        this.action = action;
        this.permission = permission;
    }

    @Override
    protected void putAttributes(AttributeGenerator generator) {
        super.putAttributes(generator);
        if (action != null) {
            generator.put("action", action);
        }
        if (permission != null) {
            generator.put("permission", permission);
        }
    }

    @Override
    protected void consumeAttributes(AttributeHelper helper)
            throws ParseException {
        super.consumeAttributes(helper);
        action = helper.consume("action", true);
        permission = helper.consume("action", true);
    }
}
