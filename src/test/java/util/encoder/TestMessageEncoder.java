package util.encoder;

import com.codahale.metrics.MetricRegistry;
import com.hivemq.codec.encoder.EncoderFactory;
import com.hivemq.codec.encoder.MQTTMessageEncoder;
import com.hivemq.configuration.service.SecurityConfigurationService;
import com.hivemq.logging.EventLog;
import com.hivemq.metrics.MetricsHolder;
import com.hivemq.metrics.handler.GlobalMQTTMessageCounter;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnectorImpl;
import com.hivemq.mqtt.message.dropping.MessageDroppedService;
import io.netty.channel.ChannelHandler;

/**
 * @author Dominik Obermaier
 * @author Lukas Brandl
 */
@ChannelHandler.Sharable
public class TestMessageEncoder extends MQTTMessageEncoder {

    public TestMessageEncoder(
            final MessageDroppedService messageDroppedService,
            final SecurityConfigurationService securityConfigurationService) {

        super(new EncoderFactory(messageDroppedService, securityConfigurationService,
                        new MqttServerDisconnectorImpl(new EventLog())),
                new GlobalMQTTMessageCounter(new MetricsHolder(new MetricRegistry())));
    }
}
