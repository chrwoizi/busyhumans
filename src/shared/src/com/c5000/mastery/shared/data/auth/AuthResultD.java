package com.c5000.mastery.shared.data.auth;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.ArrayList;

public class AuthResultD implements IsSerializable {
    public AuthProviderType provider = AuthProviderType.NONE;
    public AuthStatus status;
    public boolean isAdmin;
    public String accountId;
    public String personId;
    public ArrayList<CloakD> cloaks;
    public String anonUsername;
    public String emailAddress;
}
