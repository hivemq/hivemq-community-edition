package com.hivemq.util;

import com.hivemq.configuration.entity.mqtt.MqttConfigurationDefaults;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.publish.PUBLISH;

/**
 * Various utilities for dealing with Publishes or data from Publishes
 *
 * @author Dominik Obermaier
 * @author Silvio Giebl
 */
public class PublishUtil {

    /**
     * Returns the minimum QoS of both passed QoS
     *
     * @return the minimum of both passed QoS
     */
    public static @NotNull QoS getMinQoS(final @NotNull QoS subscribedQoS, final @NotNull QoS actualQoS) {
        if (subscribedQoS.getQosNumber() < actualQoS.getQosNumber()) {
            return subscribedQoS;
        }
        return actualQoS;
    }

    /**
     * Checks whether the given PUBLISH is expired.
     *
     * @param publish the PUBLISH to check.
     * @return whether the given PUBLISH is expired.
     */
    public static boolean checkExpiry(final @NotNull PUBLISH publish) {
        return checkExpiry(publish.getTimestamp(), publish.getMessageExpiryInterval());
    }

    /**
     * Checks whether the given expiry interval has passed starting at the given timestamp.
     *
     * @param timestampMs           the start timestamp in milliseconds.
     * @param expiryIntervalSeconds the expiry interval in seconds.
     * @return whether the given expiry interval has passed starting at the given timestamp.
     */
    public static boolean checkExpiry(final long timestampMs, final long expiryIntervalSeconds) {
        return remainingExpiry(timestampMs, expiryIntervalSeconds) == 0;
    }

    /**
     * Calculates the remaining expiry interval for the given expiry interval starting at the given timestamp.
     *
     * @param timestampMs           the start timestamp in milliseconds.
     * @param expiryIntervalSeconds the expiry interval in seconds.
     * @return the remaining expiry interval for the given expiry interval starting at the given timestamp,
     * <code>0</code> if the expiry interval has passed.
     */
    private static long remainingExpiry(final long timestampMs, final long expiryIntervalSeconds) {
        if (isExpiryDisabled(expiryIntervalSeconds)) {
            return PUBLISH.MESSAGE_EXPIRY_INTERVAL_NOT_SET;
        }
        final long waitingSeconds = (System.currentTimeMillis() - timestampMs) / 1000;
        return Math.max(0, expiryIntervalSeconds - waitingSeconds);
    }

    /**
     * Checks whether the given expiry interval eventually expires, which means it is not a disabled value.
     *
     * @param expiryIntervalSeconds the expiry interval in seconds.
     * @return whether the given expiry interval eventually expires.
     */
    private static boolean isExpiryDisabled(final long expiryIntervalSeconds) {
        return (expiryIntervalSeconds == MqttConfigurationDefaults.TTL_DISABLED) ||
                (expiryIntervalSeconds == PUBLISH.MESSAGE_EXPIRY_INTERVAL_NOT_SET);
    }
}
