package com.c5000.mastery.client.auth;

import com.c5000.mastery.client.SimpleAsyncCallback;
import com.c5000.mastery.client.events.MasteryEvents;
import com.c5000.mastery.client.events.auth.AuthProviderCredentialChangedE;
import com.c5000.mastery.shared.data.auth.AuthResultD;
import com.c5000.mastery.shared.services.IMasteryS;

public class AnonAuth extends AuthProvider {

    public String email;
    public String password;

    @Override
    public boolean hasCredential() {
        return email != null && !email.isEmpty() && password != null && !password.isEmpty();
    }

    @Override
    public void authAtMastery() {
        String passwordHash = CredentialHelper.hashAnonPassword(email, password);
        IMasteryS.Instance.get().authWithAnon(email, passwordHash, new SimpleAsyncCallback<AuthResultD>() {
            @Override
            public void onSuccess(AuthResultD result) {
                Auth.setMasteryAuth(result);
            }
        });
    }

    @Override
    public void reset() {
        super.reset();
        email = null;
        password = null;
    }

    public void onCredentialChanged(String username, String password) {
        super.reset();
        this.email = username;
        this.password = password;
        MasteryEvents.dispatch(new AuthProviderCredentialChangedE(this));
    }
}