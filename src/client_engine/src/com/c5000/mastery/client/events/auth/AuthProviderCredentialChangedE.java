package com.c5000.mastery.client.events.auth;

import com.c5000.mastery.client.auth.AuthProvider;
import com.c5000.mastery.client.events.MasteryEvents.MasteryEvent;

public class AuthProviderCredentialChangedE implements MasteryEvent {

    public AuthProvider provider;

    public AuthProviderCredentialChangedE(AuthProvider provider) {
        this.provider = provider;
    }

}