/*
 * Copyright (c) 2008-2016, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.internal.plugin.multicast;

import com.hazelcast.logging.ILogger;
import com.hazelcast.nio.Address;
import com.hazelcast.spi.discovery.DiscoveryNode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.MulticastSocket;

public class MulticastDiscoverySender implements Runnable {

    private static final int SLEEP_DURATION = 2000;
    MulticastSocket multicastSocket;
    MulticastMemberInfo multicastMemberInfo;
    DatagramPacket datagramPacket;
    ILogger logger;
    private boolean stop;

    public MulticastDiscoverySender(DiscoveryNode discoveryNode, MulticastSocket multicastSocket, ILogger logger)
            throws IOException {
        this.multicastSocket = multicastSocket;
        this.logger = logger;
        if (discoveryNode != null) {
            Address address = discoveryNode.getPublicAddress();
            multicastMemberInfo = new MulticastMemberInfo(address.getHost(), address.getPort());
        }
        initDatagramPacket();
    }

    private void initDatagramPacket() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out;
        out = new ObjectOutputStream(bos);
        out.writeObject(multicastMemberInfo);
        byte[] yourBytes = bos.toByteArray();
        datagramPacket = new DatagramPacket(yourBytes, yourBytes.length,
                multicastSocket.getInetAddress(), multicastSocket.getPort());
    }

    @Override
    public void run() {
        while (!stop) {
            try {
                Thread.sleep(SLEEP_DURATION);
            } catch (InterruptedException e) {
                logger.finest("Thread sleeping interrupted. This may due to graceful shutdown.");
            }
            try {
                send();
            } catch (IOException e) {
                logger.finest(e.getMessage());
            }
        }
    }

    void send() throws IOException {
        multicastSocket.send(datagramPacket);
    }

    void stop() {
        stop = true;
    }
}
