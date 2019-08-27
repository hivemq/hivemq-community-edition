package com.hivemq.extension.sdk.api.packets.unsubscribe;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.packets.general.ModifiableUserProperties;

import java.util.List;

/**
 * A copy of an {@link UnsubscribePacket} that can be modified for onward delivery.
 *
 * @author Robin Atherton
 */
public interface ModifiableUnsubscribePacket extends UnsubscribePacket {

    /**
     * Sets the list of Topics to be unsubscribed from.
     *
     * @param topics the list of Topics to unsubscribe from.
     */
    void setTopics(List<String> topics);

    /**
     * Adds one or more topics to the UNSUBSCRIBE packet.
     *
     * @param topics one or more topics to be added.
     */
    void addTopics(String... topics);

    /**
     * Removes one or more topics from the UNSUBSCRIBE packet.
     *
     * @param topics one or more topics to be removed.
     */
    void removeTopics(String... topics);

    /**
     * Gets the modifiable {@link ModifiableUserProperties} of the UNSUBSCRIBE packet.
     */
    @Override
    @NotNull ModifiableUserProperties getUserProperties();

    /**
     * Used to check if an UNSUBSCRIBE package has been modified.
     *
     * @return true if the UNSUBSCRIBE package has been modified, false if it has not.
     */
    boolean isModified();
}
