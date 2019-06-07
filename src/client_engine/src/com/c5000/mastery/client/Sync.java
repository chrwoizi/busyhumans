package com.c5000.mastery.client;


import com.c5000.mastery.client.events.NewAnnouncementE;
import com.c5000.mastery.client.events.MasteryEvents;
import com.c5000.mastery.shared.data.base.AnnouncementD;
import com.c5000.mastery.shared.data.base.ClockSyncResultD;
import com.c5000.mastery.shared.data.base.SyncResultD;
import com.c5000.mastery.shared.services.IMasteryS;
import com.google.gwt.user.client.Timer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class Sync {

    private static long serverClockOffset;

    private static final int FAST_SYNC_SAMPLES = 5;
    private static final int KEEP_SYNC_SAMPLES = 10;
    private static final int FAST_SYNC_DELAY = 3000;
    private static final int SLOW_SYNC_DELAY = 600000;

    private static long[] networkDelays = new long[KEEP_SYNC_SAMPLES];
    private static int networkDelaysSize = 0;
    private static int networkDelaysCursor = 0;
    private static long networkDelayMedian = 0;
    private static Timer syncTimer;
    private static boolean isFastSync = true;

    private static List<String> knownAnnouncements = new ArrayList<String>();

    public static Date clientTime() {
        return new Date();
    }

    public static Date serverTime() {
        return new Date(new Date().getTime() + serverClockOffset);
    }

    public static void sync(SyncResultD result) {
        syncClocks(result);

        // receive announcements
        if(result.announcements != null && !result.announcements.isEmpty()) {
            for(AnnouncementD announcement : result.announcements) {
                if(!knownAnnouncements.contains(announcement.id)) {
                    knownAnnouncements.add(announcement.id);
                    MasteryEvents.dispatch(new NewAnnouncementE(announcement));
                }
            }
        }
    }

    public static void syncClocks(ClockSyncResultD result) {

        // update clock offset
        long networkDelay = clientTime().getTime() - result.clientTime - result.serverThinkingDuration;
        addNetworkDelaySample(networkDelay);
        serverClockOffset = (long) (result.serverTime - clientTime().getTime() + networkDelayMedian * 0.5);

        scheduleNext();
    }

    public static void scheduleNext() {// schedule next sync
        if(isFastSync && networkDelaysSize >= FAST_SYNC_SAMPLES) {
            isFastSync = false;
            syncTimer.cancel();
            syncTimer = null;
        }
        scheduleSync();
    }

    private static void addNetworkDelaySample(long networkDelay) {

        // add sample
        networkDelays[networkDelaysCursor] = networkDelay;
        networkDelaysCursor = (networkDelaysCursor + 1) % KEEP_SYNC_SAMPLES;
        networkDelaysSize = Math.min(networkDelaysSize + 1, KEEP_SYNC_SAMPLES);

        // get median
        long[] sorted = new long[networkDelaysSize];
        for (int i = 0; i < networkDelaysSize; ++i)
            sorted[i] = networkDelays[i];
        Arrays.sort(sorted);
        networkDelayMedian = sorted[networkDelaysSize / 2];
    }

    private static void scheduleSync() {
        if(syncTimer == null) {
            syncTimer = new Timer() {
                @Override
                public void run() {
                    IMasteryS.Instance.get().sync(clientTime().getTime(), new SimpleAsyncCallback<SyncResultD>() {
                        @Override
                        public void onSuccess(SyncResultD result) {
                            sync(result);
                            scheduleNext();
                        }
                    });
                }
            };
            syncTimer.scheduleRepeating(isFastSync ? FAST_SYNC_DELAY : SLOW_SYNC_DELAY);
        }
    }

    public static void force() {
        IMasteryS.Instance.get().sync(clientTime().getTime(), new SimpleAsyncCallback<SyncResultD>() {
            @Override
            public void onSuccess(SyncResultD result) {
                sync(result);
            }
        });
    }
}
