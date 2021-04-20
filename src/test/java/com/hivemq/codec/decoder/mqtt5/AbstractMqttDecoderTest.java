package com.hivemq.codec.decoder.mqtt5;

import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnectorImpl;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.After;
import org.junit.Before;
import org.slf4j.LoggerFactory;
import util.LogbackCapturingAppender;
import util.TestMqttDecoder;

public class AbstractMqttDecoderTest {

    protected @NotNull EmbeddedChannel channel;
    protected @NotNull ClientConnection clientConnection = new ClientConnection();
    protected @NotNull LogbackCapturingAppender logCapture;

    @Before
    public void setUp() {
        logCapture = LogbackCapturingAppender.Factory.weaveInto(LoggerFactory.getLogger(MqttServerDisconnectorImpl.class));
        createChannel();
    }

    @After
    public void tearDown() {
        LogbackCapturingAppender.Factory.cleanUp();
        channel.close();
    }

    protected void createChannel() {
        channel = new EmbeddedChannel(TestMqttDecoder.create());
        channel.attr(ChannelAttributes.CLIENT_CONNECTION).set(clientConnection);
    }
}
