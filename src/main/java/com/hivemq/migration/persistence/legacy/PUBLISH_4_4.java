package com.hivemq.migration.persistence.legacy;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.message.MessageType;
import com.hivemq.mqtt.message.MessageWithID;
import com.hivemq.mqtt.message.publish.PUBLISH;

/**
 * @author Florian Limpöck
 * @author Lukas Brandl
 * @since 4.5.0
 */
public class PUBLISH_4_4 extends MessageWithID {

    private final long payloadID;
    private final @NotNull PUBLISH publish;

    public PUBLISH_4_4(@NotNull final PUBLISH publish, final long payloadID) {
        this.publish = publish;
        this.payloadID = payloadID;
    }

    public long getPayloadID() {
        return payloadID;
    }

    @NotNull
    public PUBLISH getPublish() {
        return publish;
    }

    @Override
    public @NotNull MessageType getType() {
        return MessageType.PUBLISH;
    }

    @Override
    public void setEncodedLength(final int length) {
        publish.setEncodedLength(length);
    }

    @Override
    public int getEncodedLength() {
        return publish.getEncodedLength();
    }

    @Override
    public void setRemainingLength(final int length) {
        publish.setRemainingLength(length);
    }

    @Override
    public int getRemainingLength() {
        return publish.getRemainingLength();
    }

    @Override
    public void setPropertyLength(final int length) {
        publish.setPropertyLength(length);
    }

    @Override
    public int getPropertyLength() {
        return publish.getPropertyLength();
    }

    @Override
    public void setOmittedProperties(final int omittedProperties) {
        publish.setOmittedProperties(omittedProperties);
    }

    @Override
    public int getOmittedProperties() {
        return publish.getOmittedProperties();
    }
}
