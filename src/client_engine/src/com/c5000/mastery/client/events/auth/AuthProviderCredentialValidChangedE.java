package com.c5000.mastery.client.events.auth;

import com.c5000.mastery.client.auth.AuthProvider;
import com.c5000.mastery.client.events.MasteryEvents.MasteryEvent;

public class AuthProviderCredentialValidChangedE implements MasteryEvent {

    public AuthProvider provider;
    public boolean shouldReset;

    public AuthProviderCredentialValidChangedE(AuthProvider provider, boolean shouldReset) {
        this.provider = provider;
        this.shouldReset = shouldReset;
    }

}