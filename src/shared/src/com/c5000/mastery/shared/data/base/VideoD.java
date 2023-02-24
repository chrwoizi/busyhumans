package com.c5000.mastery.shared.data.base;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.ArrayList;
import java.util.Date;

public class VideoD implements IsSerializable {
    public String id;
    public String title;
    public Date date;
    public boolean embeddable;
    public boolean ready;
    public String error;
    public boolean authenticated;
}
