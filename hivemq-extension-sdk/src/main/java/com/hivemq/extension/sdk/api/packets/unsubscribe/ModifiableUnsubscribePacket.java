package com.hivemq.extension.sdk.api.packets.unsubscribe;

import com.hivemq.extension.sdk.api.packets.general.ModifiableUserProperties;
import com.hivemq.extension.sdk.api.packets.general.UserProperties;

import java.util.List;

/**
 * A copy of an {@link UnsubscribePacket} that can be modified for onward delivery.
 *
 * @author Robin Atherton
 */
public interface ModifiableUnsubscribePacket {

    /**
     * @return the list of topics to be unsubscribed from.
     */
    List<String> getTopics();

    /**
     * Get the modifiable {@link UserProperties} of the UNSUBSCRIBE packet.
     *
     * @return the modifiable user properties.
     */
    ModifiableUserProperties getUserProperties();
}
