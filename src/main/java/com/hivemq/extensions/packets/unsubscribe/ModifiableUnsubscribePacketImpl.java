package com.hivemq.extensions.packets.unsubscribe;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.packets.general.ModifiableUserProperties;
import com.hivemq.extension.sdk.api.packets.unsubscribe.ModifiableUnsubscribePacket;
import com.hivemq.extensions.packets.general.ModifiableUserPropertiesImpl;
import com.hivemq.mqtt.message.unsubscribe.UNSUBSCRIBE;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Robin Atherton
 */
public class ModifiableUnsubscribePacketImpl implements ModifiableUnsubscribePacket {

    private final @NotNull ModifiableUserPropertiesImpl userProperties;
    private @NotNull ImmutableList<String> topics;
    private final int packetIdentifier;

    private boolean modified = false;

    public ModifiableUnsubscribePacketImpl(
            final @NotNull FullConfigurationService fullConfigurationService, final @NotNull UNSUBSCRIBE unsubscribe) {
        this.topics = unsubscribe.getTopics();
        this.userProperties = new ModifiableUserPropertiesImpl(
                unsubscribe.getUserProperties().getPluginUserProperties(),
                fullConfigurationService.securityConfiguration().validateUTF8());
        this.packetIdentifier = unsubscribe.getPacketIdentifier();
    }

    @Override
    public ImmutableList<String> getTopics() {
        return this.topics;
    }

    @Override
    public void setTopics(final @NotNull List<String> topics) {
        this.topics = ImmutableList.copyOf(topics);
        this.modified = true;
    }

    @Override
    public void addTopics(final @NotNull String... topics) {
        final ArrayList<String> topicList = new ArrayList<>();
        for (final String topic : topics) {
            if (!this.topics.contains(topic)) {
                topicList.add(topic);
            }
        }
        final ArrayList<String> demutedTopics =
                Lists.newArrayList(this.topics);
        demutedTopics.addAll(topicList);
        this.topics = ImmutableList.copyOf(demutedTopics);
        this.modified = true;
    }

    @Override
    public void removeTopics(final @NotNull String... topics) {
        final ArrayList<String> topicList = new ArrayList<>();
        for (final String topic : topics) {
            if (this.topics.contains(topic)) {
                topicList.add(topic);
            } else {
                throw new IllegalArgumentException("Cannot remove topics to which the client is not subscribed.");
            }
        }
        final ArrayList<String> demutedTopics =
                Lists.newArrayList(this.topics);
        final Iterator<String> iterator = demutedTopics.iterator();
        while (iterator.hasNext()) {
            for (final String topic : topicList) {
                if (iterator.next().equals(topic)) {
                    demutedTopics.remove(topic);
                }
            }
        }
        this.topics = ImmutableList.copyOf(demutedTopics);
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
