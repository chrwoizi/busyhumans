package com.c5000.mastery.client.events;

import com.c5000.mastery.client.events.MasteryEvents.MasteryEvent;
import com.c5000.mastery.shared.data.base.TokenizedResourceD;

/**
 * Invoked when a picture has been selected in the skill creation process
 */
public class PictureSelectedE implements MasteryEvent {
    public TokenizedResourceD picture;

    public PictureSelectedE(TokenizedResourceD picture) {
        this.picture = picture;
    }
}