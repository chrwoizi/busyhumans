package com.c5000.mastery.shared.data.base;

import com.google.gwt.user.client.rpc.IsSerializable;

public class ClockSyncResultD implements IsSerializable {
    public long serverThinkingDuration;
    public long clientTime;
    public long serverTime;
}
