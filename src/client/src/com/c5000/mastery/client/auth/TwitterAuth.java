package com.c5000.mastery.client.auth;

import com.c5000.mastery.client.SimpleAsyncCallback;
import com.c5000.mastery.client.events.MasteryEvents;
import com.c5000.mastery.client.events.auth.AuthProviderCredentialChangedE;
import com.c5000.mastery.client.events.auth.AuthProviderCredentialValidChangedE;
import com.c5000.mastery.shared.data.auth.AuthResultD;
import com.c5000.mastery.shared.data.auth.CredentialCheckResultD;
import com.c5000.mastery.shared.services.IMasteryS;

public class TwitterAuth extends AuthProvider {

    private String verifier;

    @Override
    public boolean hasCredential() {
        return verifier != null && !verifier.isEmpty();
    }

    @Override
    public void authAtMastery() {
        IMasteryS.Instance.get().authWithTwitter(verifier, new SimpleAsyncCallback<AuthResultD>() {
            @Override
            public void onSuccess(AuthResultD result) {
                Auth.setMasteryAuth(result);
            }
        });
    }

    @Override
    public void reset() {
        super.reset();
        verifier = null;
    }

    public void onCredentialChanged(String verifier) {
        super.reset();
        this.verifier = verifier;
        MasteryEvents.dispatch(new AuthProviderCredentialChangedE(this));
    }
}