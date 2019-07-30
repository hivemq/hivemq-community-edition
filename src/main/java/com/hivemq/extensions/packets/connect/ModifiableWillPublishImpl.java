package com.hivemq.extensions.packets.connect;

import com.hivemq.annotations.NotNull;
import com.hivemq.codec.encoder.mqtt5.UnsignedDataTypes;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.ThreadSafe;
import com.hivemq.extension.sdk.api.packets.connect.WillPublishPacket;
import com.hivemq.extension.sdk.api.packets.publish.ModifiableWillPublish;
import com.hivemq.extensions.packets.general.InternalUserProperties;
import com.hivemq.extensions.packets.publish.ModifiablePublishPacketImpl;

import static com.google.common.base.Preconditions.checkArgument;
import static com.hivemq.mqtt.message.publish.PUBLISH.MESSAGE_EXPIRY_INTERVAL_NOT_SET;

/**
 * @author Lukas Brandl
 */
@ThreadSafe
public class ModifiableWillPublishImpl extends ModifiablePublishPacketImpl implements ModifiableWillPublish {

    private long willDelay;

    public ModifiableWillPublishImpl(@NotNull final FullConfigurationService configurationService,
                                     @NotNull final WillPublishPacket willPublishPacket) {
        super(configurationService,
                willPublishPacket.getTopic(),
                willPublishPacket.getQos().getQosNumber(),
                willPublishPacket.getRetain(),
                willPublishPacket.getPayloadFormatIndicator().orElse(null),
                willPublishPacket.getMessageExpiryInterval().orElse(MESSAGE_EXPIRY_INTERVAL_NOT_SET),
                willPublishPacket.getResponseTopic().orElse(null),
                willPublishPacket.getCorrelationData().orElse(null),
                willPublishPacket.getSubscriptionIdentifiers(),
                willPublishPacket.getContentType().orElse(null),
                willPublishPacket.getPayload().orElse(null),
                (InternalUserProperties) willPublishPacket.getUserProperties(),
                willPublishPacket.getPacketId(),
                willPublishPacket.getDupFlag());
        this.willDelay = willPublishPacket.getWillDelay();
    }

    @Override
    public synchronized void setWillDelay(final long willDelay) {
        checkArgument(willDelay >= 0, "Will delay must NOT be less than 0");
        checkArgument(willDelay < UnsignedDataTypes.UNSIGNED_INT_MAX_VALUE, "Will delay must be less than 4294967295");

        if (this.willDelay == willDelay) {
            return;
        }
        this.willDelay = willDelay;
        modified = true;
    }

    @Override
    public long getWillDelay() {
        return willDelay;
    }
}
