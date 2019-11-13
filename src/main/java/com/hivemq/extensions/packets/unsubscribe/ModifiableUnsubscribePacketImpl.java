package com.hivemq.extensions.packets.unsubscribe;

import com.google.common.collect.ImmutableList;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.packets.general.ModifiableUserProperties;
import com.hivemq.extension.sdk.api.packets.unsubscribe.ModifiableUnsubscribePacket;
import com.hivemq.extensions.packets.general.ModifiableUserPropertiesImpl;
import com.hivemq.mqtt.message.unsubscribe.UNSUBSCRIBE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Robin Atherton
 */
public class ModifiableUnsubscribePacketImpl implements ModifiableUnsubscribePacket {

    Logger logger = LoggerFactory.getLogger(ModifiableUnsubscribePacketImpl.class);

    private final @NotNull ModifiableUserPropertiesImpl userProperties;
    private @NotNull ImmutableList<String> topics;
    private final int packetIdentifier;

    private boolean modified = false;

    public ModifiableUnsubscribePacketImpl(
            final @NotNull FullConfigurationService fullConfigurationService, final @NotNull UNSUBSCRIBE unsubscribe) {
        this.topics = ImmutableList.copyOf(unsubscribe.getTopics());
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
    public void setTopics(final @NotNull List<String> topics) {
        this.topics = ImmutableList.copyOf(topics);
        this.modified = true;
    }

    @Override
    public void addTopics(final @NotNull String... topics) {
        boolean modifiedFlag = true;
        final List<String> temp = new ArrayList<>(this.topics);
        for (final String topic : topics) {
            if (!temp.contains(topic)) {
                temp.add(topic);
            } else {
                logger.warn("Cannot unsubscribe from topics twice.");
                if (topics.length == 1) {
                    modifiedFlag = false;
                }
            }
        }
        this.topics = ImmutableList.copyOf(temp);
        if (modifiedFlag) {
            this.modified = true;
        }
    }

    @Override
    public void removeTopics(final @NotNull String... topics) {
        final ArrayList<String> topicList = new ArrayList<>();
        for (final String topic : topics) {
            if (this.topics.contains(topic)) {
                topicList.add(topic);
            } else {
                logger.warn("Removing the same topic from the unsubscribe message's twice is not possible.");
                return;
            }
        }
        final List<String> temp = new ArrayList<>(this.topics);
        final Iterator<String> iterator = temp.iterator();
        while (iterator.hasNext()) {
            for (final String topic : topicList) {
                if (iterator.next().equals(topic)) {
                    iterator.remove();
                }
            }
        }
        this.topics = ImmutableList.copyOf(temp);
        this.modified = true;
    }

    @Override
    public @NotNull ModifiableUserProperties getUserProperties() {
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
