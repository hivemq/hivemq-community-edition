package com.hivemq.extensions.packets.unsubscribe;

import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.packets.general.ModifiableUserProperties;
import com.hivemq.extension.sdk.api.packets.unsubscribe.ModifiableUnsubscribePacket;
import com.hivemq.extensions.packets.general.ModifiableUserPropertiesImpl;
import com.hivemq.mqtt.message.unsubscribe.UNSUBSCRIBE;

import java.util.Arrays;
import java.util.List;

/**
 * @author Robin Atherton
 */
public class ModifiableUnsubscribePacketImpl implements ModifiableUnsubscribePacket {

    private final @NotNull ModifiableUserPropertiesImpl userProperties;
    private @NotNull List<String> topics;
    private final int packetIdentifier;

    private boolean modified = false;

    public ModifiableUnsubscribePacketImpl(
            final @NotNull FullConfigurationService fullConfigurationService, final @NotNull
            UNSUBSCRIBE unsubscribe) {

        this.topics = unsubscribe.getTopics();

        this.userProperties = new ModifiableUserPropertiesImpl(
                unsubscribe.getUserProperties().getPluginUserProperties(),
                fullConfigurationService.securityConfiguration().validateUTF8());

        this.packetIdentifier = unsubscribe.getPacketIdentifier();

    }

    @Override
    public List<String> getTopics() {
        return this.topics;
    }

    @Override
    public void setTopics(final List<String> topics) {
        this.topics = topics;
        this.modified = true;
    }

    @Override
    public void addTopics(final String... topics) {
        if (topics.length >= 1) {
            this.topics.add(Arrays.toString(topics));
        } else {
            this.topics.addAll(Arrays.asList(topics));
        }
        this.modified = true;

    }

    @Override
    public void removeTopics(final String... topics) {
        if (topics.length >= 1) {
            this.topics.remove(Arrays.toString(topics));
        } else {
            this.topics.removeAll(Arrays.asList(topics));
        }
        this.modified = true;
    }

    @Override
    public ModifiableUserProperties getUserProperties() {
        return this.userProperties;
    }

    @Override
    public int getPacketIdentifier() {
        return this.packetIdentifier;
    }

    public boolean isModified() {
        return modified;
    }

}
