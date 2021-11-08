package util.encoder;

import com.hivemq.codec.encoder.EncoderFactory;
import com.hivemq.codec.encoder.MqttEncoder;
import com.hivemq.configuration.service.SecurityConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnector;
import com.hivemq.mqtt.message.Message;
import com.hivemq.mqtt.message.PINGREQ;
import com.hivemq.mqtt.message.connect.CONNECT;
import com.hivemq.mqtt.message.dropping.MessageDroppedService;
import com.hivemq.mqtt.message.subscribe.SUBSCRIBE;
import com.hivemq.mqtt.message.unsubscribe.UNSUBSCRIBE;
import io.netty.channel.ChannelHandlerContext;

/**
 * @author Abdullah Imal
 */
public class TestEncoderFactory extends EncoderFactory {

    private final @NotNull Mqtt3ConnectEncoder connectEncoder;
    private final @NotNull Mqtt3SubscribeEncoder subscribeEncoder;
    private final @NotNull Mqtt3UnsubscribeEncoder unsubscribeEncoder;
    private final @NotNull PingreqEncoder pingreqEncoder;

    public TestEncoderFactory(
            final @NotNull MessageDroppedService messageDroppedService,
            final @NotNull SecurityConfigurationService securityConfigurationService,
            final @NotNull MqttServerDisconnector mqttServerDisconnector,
            final @NotNull Mqtt3ConnectEncoder connectEncoder,
            final @NotNull Mqtt3SubscribeEncoder subscribeEncoder,
            final @NotNull Mqtt3UnsubscribeEncoder unsubscribeEncoder,
            final @NotNull PingreqEncoder pingreqEncoder) {

        super(messageDroppedService, securityConfigurationService, mqttServerDisconnector);

        this.connectEncoder = connectEncoder;
        this.subscribeEncoder = subscribeEncoder;
        this.unsubscribeEncoder = unsubscribeEncoder;
        this.pingreqEncoder = pingreqEncoder;
    }

    @Override
    protected @Nullable MqttEncoder getEncoder(final @NotNull Message msg, final @NotNull ChannelHandlerContext ctx) {

        if (msg instanceof CONNECT) {
            return connectEncoder;
        }
        if (msg instanceof SUBSCRIBE) {
            return subscribeEncoder;
        }
        if (msg instanceof UNSUBSCRIBE) {
            return unsubscribeEncoder;
        }
        if (msg instanceof PINGREQ) {
            return pingreqEncoder;
        }
        return super.getEncoder(msg, ctx);
    }
}
