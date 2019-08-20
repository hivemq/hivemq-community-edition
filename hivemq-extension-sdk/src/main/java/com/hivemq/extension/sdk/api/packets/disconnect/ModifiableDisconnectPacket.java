package com.hivemq.extension.sdk.api.packets.disconnect;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.packets.general.ModifiableUserProperties;
import com.hivemq.extension.sdk.api.packets.general.UserProperties;

public interface ModifiableDisconnectPacket extends DisconnectPacket {

    /**
     * Sets a reasonString for the DISCONNECT packet.
     *
     * @param reasonString the reason to set.
     */
    void setReasonString(String reasonString);

    /**
     * Sets the session expiry interval of the DISCONNECT packet.
     *
     * @param expiryInterval a settable value indicating the interval after which the session will expire.
     */
    void setSessionExpiryInterval(long expiryInterval);

    /**
     * Sets the server reference of the DISCONNECT packet.
     *
     * @param serverReference the server reference to set.
     */
    void setServerReference(String serverReference);

    /**
     * Gets the modifiable {@link UserProperties} of the DISCONNECT packet.
     */
    @Override
    @NotNull ModifiableUserProperties getUserProperties();

}
