package util;

import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.bootstrap.ClientState;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.handler.publish.PublishFlushHandler;
import io.netty.channel.Channel;

public class DummyClientConnection extends ClientConnection {

    public DummyClientConnection(
            final @NotNull Channel channel,
            final @NotNull PublishFlushHandler publishFlushHandler) {

        super(channel, publishFlushHandler, ClientState.CONNECTING, null, null,
                false, null, null, null, null,
                null, null, null, null,
                null, false, false, false,
                false, null, null, false, false,
                null, null, null, null, null,
                null, null, null, null, null, null,
                null, null, null, null,
                null, null, null);
    }
}
