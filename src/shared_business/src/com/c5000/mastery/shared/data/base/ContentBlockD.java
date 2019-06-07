package com.c5000.mastery.shared.data.base;

import com.google.gwt.user.client.rpc.IsSerializable;

public class ContentBlockD implements IsSerializable {

    public static final int TYPE_TEXT = 0;
    public static final int TYPE_IMAGE = 1;
    public static final int TYPE_VIDEO = 2;

    public int typ;
    public TokenizedResourceD value;
    public boolean ready;
    public String error;
    public boolean authentic;
}
