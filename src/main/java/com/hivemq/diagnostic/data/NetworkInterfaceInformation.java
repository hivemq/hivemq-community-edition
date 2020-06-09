/*
 * Copyright 2019-present HiveMQ GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hivemq.diagnostic.data;

import com.google.common.annotations.VisibleForTesting;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.util.MacAddressFormatter;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;

/**
 * @author Dominik Obermaier
 */
class NetworkInterfaceInformation extends AbstractInformation {

    public String getNetworkInterfaceInformation() {
        try {

            final StringBuilder networkInterfaceBuilder = new StringBuilder();

            final Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            for (final NetworkInterface netint : Collections.list(interfaces)) {

                addInterfaceInformation(networkInterfaceBuilder, netint);
            }
            return networkInterfaceBuilder.toString();
        } catch (final Exception e) {
            return "Could not determine network interfaces. Exception: " + ExceptionUtils.getStackTrace(e);
        }
    }

    private StringBuilder addInterfaceInformation(final StringBuilder stringBuilder, final NetworkInterface networkInterface) throws SocketException {
        stringBuilder.append(String.format("┌[%s]\n", networkInterface.getName()));
        addNetworkInformation(stringBuilder, "Display Name", networkInterface.getDisplayName());
        addNetworkInformation(stringBuilder, "MAC Address", getMacaddress(networkInterface));
        addNetworkInformation(stringBuilder, "MTU", getMTU(networkInterface));
        addNetworkInformation(stringBuilder, "Is Loopback?", getIsLoopback(networkInterface));
        addNetworkInformation(stringBuilder, "Is P2P?", getIsP2P(networkInterface));
        addNetworkInformation(stringBuilder, "Is Up?", getIsUp(networkInterface));
        addNetworkInformation(stringBuilder, "Is Virtual?", getIsVirtual(networkInterface));
        addNetworkInformation(stringBuilder, "Supports Multicast?", getSupportsMulticast(networkInterface));


        stringBuilder.append(String.format("└─[%s]\n", "Inet Addresses"));

        final Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
        for (final InetAddress inetAddress : Collections.list(inetAddresses)) {
            stringBuilder.append(String.format("\t├─[%s]\n", inetAddress));
        }

        stringBuilder.append("\n");

        return stringBuilder;
    }

    @NotNull
    @VisibleForTesting
    String getIsLoopback(final @NotNull NetworkInterface networkInterface) {
        try {

            return String.valueOf(networkInterface.isLoopback());
        } catch (final Exception e) {
            return "Could not determine if interface is loopback interface";
        }
    }

    @NotNull
    @VisibleForTesting
    String getIsP2P(final @NotNull NetworkInterface networkInterface) {
        try {
            return String.valueOf(networkInterface.isPointToPoint());
        } catch (final Exception e) {
            return "Could not determine if interface is P2P interface";
        }
    }

    @NotNull
    @VisibleForTesting
    String getIsUp(final @NotNull NetworkInterface networkInterface) {
        try {
            return String.valueOf(networkInterface.isUp());
        } catch (final Exception e) {
            return "Could not determine if interface is up";
        }
    }

    @NotNull
    @VisibleForTesting
    String getIsVirtual(final @NotNull NetworkInterface networkInterface) {
        try {
            return String.valueOf(networkInterface.isVirtual());
        } catch (final Exception e) {
            return "Could not determine if interface is virtual";
        }
    }

    @NotNull
    @VisibleForTesting
    String getSupportsMulticast(final @NotNull NetworkInterface networkInterface) {
        try {
            return String.valueOf(networkInterface.supportsMulticast());
        } catch (final Exception e) {
            return "Could not determine if interface supports multicast";
        }
    }

    @NotNull
    @VisibleForTesting
    String getMTU(final @NotNull NetworkInterface networkInterface) {
        try {
            return String.valueOf(networkInterface.getMTU());
        } catch (final Exception e) {
            return "Could not determine MTU";
        }
    }

    @NotNull
    @VisibleForTesting
    String getMacaddress(final @NotNull NetworkInterface networkInterface) {
        try {
            final byte[] hardwareAddress = networkInterface.getHardwareAddress();
            if (hardwareAddress != null && hardwareAddress.length == 6) {
                return MacAddressFormatter.format(hardwareAddress);
            } else {
                return "Could not determine MAC Address";
            }
        } catch (final Exception e) {
            return "Could not determine MAC Address";
        }
    }

    private StringBuilder addNetworkInformation(final StringBuilder infoBuilder, final String key, final String value) {
        return infoBuilder.append(String.format("├─[%s] = [%s]\n", key, value));
    }
}
