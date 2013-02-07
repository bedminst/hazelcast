/*
 * Copyright (c) 2008-2013, Hazelcast, Inc. All Rights Reserved.
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

package com.hazelcast.nio;

import com.hazelcast.ascii.TextCommandService;
import com.hazelcast.config.*;
import com.hazelcast.instance.MemberImpl;
import com.hazelcast.instance.Node;
import com.hazelcast.instance.OutOfMemoryErrorDispatcher;
import com.hazelcast.logging.ILogger;
import com.hazelcast.logging.SystemLogService;
import com.hazelcast.nio.serialization.Data;
import com.hazelcast.nio.serialization.SerializationContext;
import com.hazelcast.nio.serialization.SerializationService;
import com.hazelcast.spi.impl.NodeEngineImpl;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class NodeIOService implements IOService {

    private final Node node;
    private final NodeEngineImpl nodeEngine;

    public NodeIOService(Node node) {
        this.node = node;
        this.nodeEngine = node.nodeEngine;
    }

    public boolean isActive() {
        return node.isActive();
    }

    public ILogger getLogger(String name) {
        return node.getLogger(name);
    }

    public SystemLogService getSystemLogService() {
        return node.getSystemLogService();
    }

    public void onOutOfMemory(OutOfMemoryError oom) {
        OutOfMemoryErrorDispatcher.onOutOfMemory(oom);
    }

    public void handleInterruptedException(Thread thread, RuntimeException e) {
        node.handleInterruptedException(thread, e);
    }

    public void onIOThreadStart() {
//        ThreadContext.get().setCurrentInstance(node.hazelcastInstance);
    }

    public Address getThisAddress() {
        return node.getThisAddress();
    }

    public void onFatalError(Exception e) {
        getSystemLogService().logConnection(e.getClass().getName() + ": " + e.getMessage());
        node.shutdown(false, false);
    }

    public SocketInterceptorConfig getSocketInterceptorConfig() {
        return node.getConfig().getNetworkConfig().getSocketInterceptorConfig();
    }

    public SymmetricEncryptionConfig getSymmetricEncryptionConfig() {
        return node.getConfig().getNetworkConfig().getSymmetricEncryptionConfig();
    }

    public AsymmetricEncryptionConfig getAsymmetricEncryptionConfig() {
        return node.getConfig().getNetworkConfig().getAsymmetricEncryptionConfig();
    }

    public SSLConfig getSSLConfig() {
        return node.getConfig().getNetworkConfig().getSSLConfig();
    }

    public void handleMemberPacket(final Packet packet) {
        final MemberImpl member = node.clusterService.getMember(packet.getConn().getEndPoint());
        if (member != null) {
            member.didRead();
        }
        nodeEngine.handlePacket(packet);
    }

    public void handleClientCommand(Protocol p) {
        //TODO command name is not serviceName. A mapper should be introduced.
        node.clientCommandService.handle(p);
    }

    public TextCommandService getTextCommandService() {
        return node.getTextCommandService();
    }

    public boolean isMemcacheEnabled() {
        return node.groupProperties.MEMCACHE_ENABLED.getBoolean();
    }

    public boolean isRestEnabled() {
        return node.groupProperties.REST_ENABLED.getBoolean();
    }

    public void removeEndpoint(final Address endPoint) {
        nodeEngine.getExecutionService().execute("hz:io", new Runnable() {
            public void run() {
                node.clusterService.removeAddress(endPoint);
            }
        });
    }

    public String getThreadPrefix() {
        return node.getThreadPoolNamePrefix("IO");
    }

    public ThreadGroup getThreadGroup() {
        return node.threadGroup;
    }

    public void onFailedConnection(Address address) {
        if (!node.joined()) {
            node.failedConnection(address);
        }
    }

    public void shouldConnectTo(Address address) {
        if (node.getThisAddress().equals(address)) {
            throw new RuntimeException("Connecting to self! " + address);
        }
    }

    public boolean isReuseSocketAddress() {
        return node.getConfig().getNetworkConfig().isReuseAddress();
    }

    public int getSocketPort() {
        return node.getConfig().getNetworkConfig().getPort();
    }

    public boolean isSocketBindAny() {
        return node.groupProperties.SOCKET_CLIENT_BIND_ANY.getBoolean();
    }

    public boolean isSocketPortAutoIncrement() {
        return node.getConfig().getNetworkConfig().isPortAutoIncrement();
    }

    public int getSocketReceiveBufferSize() {
        return this.node.getGroupProperties().SOCKET_RECEIVE_BUFFER_SIZE.getInteger();
    }

    public int getSocketSendBufferSize() {
        return this.node.getGroupProperties().SOCKET_SEND_BUFFER_SIZE.getInteger();
    }

    public int getSocketLingerSeconds() {
        return this.node.getGroupProperties().SOCKET_LINGER_SECONDS.getInteger();
    }

    public boolean getSocketKeepAlive() {
        return this.node.getGroupProperties().SOCKET_KEEP_ALIVE.getBoolean();
    }

    public boolean getSocketNoDelay() {
        return this.node.getGroupProperties().SOCKET_NO_DELAY.getBoolean();
    }

    public int getSelectorThreadCount() {
        return node.groupProperties.IO_THREAD_COUNT.getInteger();
    }

    public void onDisconnect(final Address endpoint) {
        if (endpoint != null) {
            nodeEngine.getExecutionService().execute("hz:io", new Runnable() {
                public void run() {
                    nodeEngine.onMemberDisconnect(endpoint);
                }
            });
        }
    }

    public boolean isClient() {
        return false;
    }

    public long getConnectionMonitorInterval() {
        return node.groupProperties.CONNECTION_MONITOR_INTERVAL.getLong();
    }

    public int getConnectionMonitorMaxFaults() {
        return node.groupProperties.CONNECTION_MONITOR_MAX_FAULTS.getInteger();
    }

    public void onShutdown() {
//        try {
//            ThreadContext.get().setCurrentInstance(node.hazelcastInstance);
            // wait a little
//            Thread.sleep(100);
//        } catch (Throwable ignored) {
//        }
    }

    public void executeAsync(final Runnable runnable) {
        nodeEngine.getExecutionService().execute("hz:io", runnable);
    }

    public Data toData(Object obj) {
        return nodeEngine.toData(obj);
    }

    public Object toObject(Data data) {
        return nodeEngine.toObject(data);
    }

    public SerializationService getSerializationService() {
        return node.getSerializationService();
    }

    public SerializationContext getSerializationContext() {
        return node.serializationService.getSerializationContext();
    }

    public Collection<Integer> getOutboundPorts() {
        final NetworkConfig networkConfig = node.getConfig().getNetworkConfig();
        final Collection<String> portDefinitions = networkConfig.getOutboundPortDefinitions() == null
                ? Collections.<String>emptySet() : networkConfig.getOutboundPortDefinitions();
        final Set<Integer> ports = networkConfig.getOutboundPorts() == null
                ? new HashSet<Integer>() : new HashSet<Integer>(networkConfig.getOutboundPorts());
        if (portDefinitions.isEmpty() && ports.isEmpty()) {
            return Collections.emptySet(); // means any port
        }
        if (portDefinitions.contains("*") || portDefinitions.contains("0")) {
            return Collections.emptySet(); // means any port
        }
        // not checking port ranges...
        for (String portDef : portDefinitions) {
            String[] portDefs = portDef.split("[,; ]");
            for (String def : portDefs) {
                def = def.trim();
                final int dashPos = def.indexOf('-');
                if (dashPos > 0) {
                    final int start = Integer.parseInt(def.substring(0, dashPos));
                    final int end = Integer.parseInt(def.substring(dashPos + 1));
                    for (int port = start; port <= end; port++) {
                        ports.add(port);
                    }
                } else {
                    ports.add(Integer.parseInt(def));
                }
            }
        }
        if (ports.contains(0)) {
            return Collections.emptySet(); // means any port
        }
        return ports;
    }
}

