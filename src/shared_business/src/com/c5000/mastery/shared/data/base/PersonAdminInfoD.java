package com.c5000.mastery.shared.data.base;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.Date;

public class PersonAdminInfoD implements IsSerializable {
    public PersonD person;
    public String loginMethod;
    public boolean hasGoogleAuth;
    public boolean hasEmail;
    public Date lastLogin;
}
