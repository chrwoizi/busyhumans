package com.c5000.mastery.client.auth;

import com.c5000.mastery.client.SimpleAsyncCallback;
import com.c5000.mastery.client.events.MasteryEvents;
import com.c5000.mastery.client.events.auth.AuthProviderCredentialChangedE;
import com.c5000.mastery.client.events.auth.AuthProviderCredentialValidChangedE;
import com.c5000.mastery.shared.data.auth.CredentialCheckResultD;
import com.c5000.mastery.shared.services.IMasteryS;

public class GoogleAuth extends AuthProvider {

    private String code;

    @Override
    public boolean hasCredential() {
        return code != null && !code.isEmpty();
    }

    @Override
    public void authAtMastery() {
        // TODO
    }

    @Override
    public void reset() {
        super.reset();
        code = null;
    }

    public void getCredentialForCurrentUser() {
        IMasteryS.Instance.get().checkGoogleCredential(new SimpleAsyncCallback<CredentialCheckResultD>() {
            @Override
            public void onSuccess(CredentialCheckResultD result) {
                setValidated(result.valid);
            }
        });
    }

    public void onCredentialChanged(String code) {
        super.reset();
        this.code = code;
        MasteryEvents.dispatch(new AuthProviderCredentialChangedE(this));
        IMasteryS.Instance.get().setGoogleCredential(code, new SimpleAsyncCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                setValidated(result);
            }
        });
    }
}