package com.c5000.mastery.client.auth;

import com.c5000.mastery.client.events.MasteryEvents;
import com.c5000.mastery.client.events.auth.AuthProviderCredentialValidChangedE;

public abstract class AuthProvider {

    private boolean validated;

    public boolean credentialValidatedByServer() {
        return validated;
    }

    public abstract boolean hasCredential();
    public abstract void authAtMastery();

    public void reset() {
        validated = false;
    }

    public void setValidated(boolean validated) {
        this.validated = validated;
        MasteryEvents.dispatch(new AuthProviderCredentialValidChangedE(this, false));
    }
}
