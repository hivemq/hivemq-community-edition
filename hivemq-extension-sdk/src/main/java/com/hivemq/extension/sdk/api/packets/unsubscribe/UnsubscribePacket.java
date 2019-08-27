package com.hivemq.extension.sdk.api.packets.unsubscribe;

import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.packets.general.UserProperties;

import java.util.List;

/**
 * Represents an UNSUBSCRIBE packet.
 * <p>
 * Contains all values of an MQTT 5 UNSUBSCRIBE, but will also be used to represent an MQTT 3 SUBSCRIBE.
 *
 * @author Robin Atherton
 */
public interface UnsubscribePacket {

    /**
     * Gets the list of topics to be unsubscribed from.
     *
     * @return the list of topics to be unsubscribed from.
     */
    @Immutable
    @NotNull
    List<String> getTopics();

    /**
     * Get the unmodifiable {@link UserProperties} of the UNSUBSCRIBE packet.
     *
     * @return user properties.
     */
    UserProperties getUserProperties();

    /**
     * @return the packet identifier.
     */
    int getPacketIdentifier();
}
