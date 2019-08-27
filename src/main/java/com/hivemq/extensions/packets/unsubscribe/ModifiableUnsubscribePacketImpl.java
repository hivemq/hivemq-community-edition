package com.hivemq.extensions.packets.unsubscribe;

import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.packets.general.ModifiableUserProperties;
import com.hivemq.extension.sdk.api.packets.unsubscribe.ModifiableUnsubscribePacket;
import com.hivemq.extensions.packets.general.ModifiableUserPropertiesImpl;
import com.hivemq.mqtt.message.unsubscribe.UNSUBSCRIBE;

import java.util.List;

/**
 * @author Robin Atherton
 */
public class ModifiableUnsubscribePacketImpl implements ModifiableUnsubscribePacket {

    private final @NotNull ModifiableUserPropertiesImpl userProperties;
    private final @NotNull List<String> topics;

    private final boolean modified = false;

    public ModifiableUnsubscribePacketImpl(
            final @NotNull FullConfigurationService fullConfigurationService, final @NotNull
            UNSUBSCRIBE unsubscribe) {

        this.topics = unsubscribe.getTopics();

        this.userProperties = new ModifiableUserPropertiesImpl(
                unsubscribe.getUserProperties().getPluginUserProperties(),
                fullConfigurationService.securityConfiguration().validateUTF8());
    }

    @Override
    public List<String> getTopics() {
        return this.topics;
    }

    @Override
    public ModifiableUserProperties getUserProperties() {
        return this.userProperties;
    }

    public synchronized boolean isModified() {
        //TODO
        return true;
    }

}
