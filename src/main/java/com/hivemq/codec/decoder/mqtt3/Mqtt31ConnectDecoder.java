/*
 * Copyright 2019 dc-square GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hivemq.codec.decoder.mqtt3;

import com.hivemq.bootstrap.ioc.lazysingleton.LazySingleton;
import com.hivemq.codec.decoder.AbstractMqttConnectDecoder;
import com.hivemq.configuration.HivemqId;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.logging.EventLog;
import com.hivemq.mqtt.handler.connack.MqttConnacker;
import com.hivemq.mqtt.handler.disconnect.Mqtt3ServerDisconnector;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.connack.CONNACK;
import com.hivemq.mqtt.message.connack.Mqtt3ConnAckReturnCode;
import com.hivemq.mqtt.message.connect.CONNECT;
import com.hivemq.mqtt.message.connect.MqttWillPublish;
import com.hivemq.util.Bytes;
import com.hivemq.util.ChannelAttributes;
import com.hivemq.util.Strings;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.hivemq.util.Bytes.isBitSet;
import static com.hivemq.util.ChannelUtils.getChannelIP;

/**
 * This is the MQTT CONNECT decoder for MQTT 3.1.1 messages. The general strategy of the decoder is to disconnect an
 * invalid client as fast as possible. That means, the actual object creation of the CONNECT message is deferred until
 * all protocol checks have passed successfully in order to be as efficient as possibles
 *
 * @author Dominik Obermaier
 */
@LazySingleton
public class Mqtt31ConnectDecoder extends AbstractMqttConnectDecoder {

    private static final Logger log = LoggerFactory.getLogger(Mqtt31ConnectDecoder.class);

    public static final String PROTOCOL_NAME = "MQIsdp";

    public final EventLog eventLog;
    private final HivemqId hiveMQId;

    public Mqtt31ConnectDecoder(final MqttConnacker connacker, final Mqtt3ServerDisconnector disconnector,
                                final EventLog eventLog, final FullConfigurationService fullConfigurationService,
                                final HivemqId hiveMQId) {
        super(connacker, disconnector, fullConfigurationService);
        this.eventLog = eventLog;
        this.hiveMQId = hiveMQId;
    }

    @Override
    public CONNECT decode(final Channel channel, final ByteBuf buf, final byte header) {

        if (buf.readableBytes() < 12) {
            if (log.isDebugEnabled()) {
                log.debug("A client (IP: {}) sent a Connect message with an incorrect connect header. Disconnecting client.", getChannelIP(channel).or("UNKNOWN"));
            }
            eventLog.clientWasDisconnected(channel, "Invalid CONNECT header");
            channel.close();
            return null;
        }

        final ByteBuf connectHeader = buf.readSlice(12);

        if (!validateProtocolName(connectHeader)) {
            if (log.isDebugEnabled()) {
                log.debug("A client (IP: {}) connected with an invalid protocol name. Disconnecting client.", getChannelIP(channel).or("UNKNOWN"));
            }
            eventLog.clientWasDisconnected(channel, "Invalid CONNECT protocol name");
            channel.close();
            return null;
        }

        //We don't need to validate the protocol version byte since we already know it's valid, otherwise
        //we wouldn't be in this protocol-version dependant decoder
        connectHeader.readByte();

        final byte connectFlagsByte = connectHeader.readByte();


        final boolean isCleanSessionFlag = isBitSet(connectFlagsByte, 1);
        final boolean isWillFlag = isBitSet(connectFlagsByte, 2);
        final boolean isWillRetain = isBitSet(connectFlagsByte, 5);
        final boolean isPasswordFlag = isBitSet(connectFlagsByte, 6);
        final boolean isUsernameFlag = isBitSet(connectFlagsByte, 7);

        final int willQoS = (connectFlagsByte & 0b0001_1000) >> 3;

        if (!validateWill(isWillFlag, isWillRetain, willQoS)) {
            if (log.isDebugEnabled()) {
                log.debug("A client (IP: {}) connected with an invalid willTopic flag combination. Disconnecting client.", getChannelIP(channel).or("UNKNOWN"));
            }
            eventLog.clientWasDisconnected(channel, "Invalid CONNECT will-topic/flag combination");
            channel.close();
            return null;
        }

        if (!validateUsernamePassword(isUsernameFlag, isPasswordFlag)) {
            if (log.isDebugEnabled()) {
                log.debug("A client (IP: {}) connected with an invalid username/password combination. The password flag was set but the username flag was not set. Disconnecting client.", getChannelIP(channel).or("UNKNOWN"));
            }
            eventLog.clientWasDisconnected(channel, "Invalid CONNECT username/password combination");
            channel.close();
            return null;
        }

        final int keepAlive = connectHeader.readUnsignedShort();

        final int utf8StringLength;

        if (buf.readableBytes() < 2 || (buf.readableBytes() < (utf8StringLength = buf.readUnsignedShort()) && utf8StringLength > 0)) {
            if (log.isDebugEnabled()) {
                log.debug("A client (IP: {}) sent a CONNECT message with an incorrect client id length. Disconnecting client.", getChannelIP(channel).or("UNKNOWN"));
            }
            eventLog.clientWasDisconnected(channel, "Incorrect CONNECT client id length");
            channel.close();
            return null;
        }

        if (utf8StringLength == 0) {
            if (log.isDebugEnabled()) {
                log.debug("A client (IP: {}) connected with NO client id. This is not allowed. Disconnecting client.", getChannelIP(channel).or("UNKNOWN"));
            }

            channel.writeAndFlush(new CONNACK(Mqtt3ConnAckReturnCode.REFUSED_IDENTIFIER_REJECTED)).
                    addListener(ChannelFutureListener.CLOSE);
            return null;
        }

        final String clientId;

        if (validateUTF8) {
            clientId = Strings.getValidatedPrefixedString(buf, utf8StringLength, true);
            if (clientId == null) {
                if (log.isDebugEnabled()) {
                    log.debug("The client id of the client (IP: {}) is not well formed. This is not allowed. Disconnecting client.", getChannelIP(channel).or("UNKNOWN"));
                }
                eventLog.clientWasDisconnected(channel, "Sent CONNECT with bad UTF-8 character");
                channel.close();
                buf.clear();
                return null;
            }
        } else {
            clientId = Strings.getPrefixedString(buf, utf8StringLength);
        }

        final MqttWillPublish willPublish;

        if (isWillFlag) {
            willPublish = readMqtt3WillPublish(channel, buf, willQoS, isWillRetain, eventLog, hiveMQId);
            if (willPublish == null) {
                return null;
            }
        } else {
            willPublish = null;
        }

        final String userName;
        if (isUsernameFlag) {
            userName = Strings.getPrefixedString(buf);
            if (userName == null) {
                if (log.isDebugEnabled()) {
                    log.debug("A client (IP: {}) sent a Connect message with an incorrect username length. Disconnecting client.", getChannelIP(channel).or("UNKNOWN"));
                }
                eventLog.clientWasDisconnected(channel, "Incorrect CONNECT clientId length");
                channel.close();
                buf.clear();
                return null;
            }
            channel.attr(ChannelAttributes.AUTH_USERNAME).set(userName);
        } else {
            userName = null;
        }

        final byte[] password;
        if (isPasswordFlag) {
            password = Bytes.getPrefixedBytes(buf);
            channel.attr(ChannelAttributes.AUTH_PASSWORD).set(password);
        } else {
            password = null;
        }

        channel.attr(ChannelAttributes.CLIENT_ID).set(clientId);
        channel.attr(ChannelAttributes.CONNECT_KEEP_ALIVE).set(keepAlive);
        channel.attr(ChannelAttributes.CLEAN_START).set(isCleanSessionFlag);

        return new CONNECT.Mqtt3Builder().withProtocolVersion(ProtocolVersion.MQTTv3_1)
                .withClientIdentifier(clientId)
                .withUsername(userName)
                .withPassword(password)
                .withCleanStart(isCleanSessionFlag)
                .withSessionExpiryInterval(isCleanSessionFlag ? 0 : maxSessionExpiryInterval)
                .withKeepAliveTimer(keepAlive)
                .withPasswordRequired(isPasswordFlag)
                .withUsernameRequired(isUsernameFlag)
                .withWill(isWillFlag)
                .withWillPublish(willPublish).build();
    }

    private boolean validateUsernamePassword(final boolean isUsernameFlag, final boolean isPasswordFlag) {
        //Validates that the username flag is set if the password flag is set
        return !isPasswordFlag || isUsernameFlag;
    }

    private boolean validateWill(final boolean isWillFlag, final boolean isWillRetain, final int willQoS) {
        return (isWillFlag && willQoS < 3) || (!isWillRetain && willQoS == 0);

    }


    private boolean validateProtocolName(final ByteBuf protocolNameBuffer) {

        return PROTOCOL_NAME.equals(Strings.getPrefixedString(protocolNameBuffer));
    }

}
