package com.hivemq.mqtt.topic;

import com.hivemq.util.Bytes;

public enum SubscriptionFlagX {

    SHARED_SUBSCRIPTION(1),

    RETAIN_AS_PUBLISHED(2),

    NO_LOCAL(3);

    public final int flagIndex;

    SubscriptionFlagX(int flagIndex) {
        this.flagIndex = flagIndex;
    }

    public static byte getDefaultFlags(final boolean isSharedSubscription, final boolean retainAsPublished, final boolean noLocal) {
        byte flags = Bytes.setBit((byte) 0, SHARED_SUBSCRIPTION.flagIndex, isSharedSubscription);
        flags = Bytes.setBit(flags, RETAIN_AS_PUBLISHED.flagIndex, retainAsPublished);
        return Bytes.setBit(flags, NO_LOCAL.flagIndex, noLocal);
    }

}
