/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.common.messaging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.Test;

import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.io.impl.SerializationRegistry;
import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.IConnectionProvider;
import com.exametrika.common.messaging.IFeed;
import com.exametrika.common.messaging.ILiveNodeProvider;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.IReceiver;
import com.exametrika.common.messaging.ISender;
import com.exametrika.common.messaging.ISink;
import com.exametrika.common.messaging.impl.MessageFlags;
import com.exametrika.common.messaging.impl.message.MessageFactory;
import com.exametrika.common.messaging.impl.protocols.AbstractProtocol;
import com.exametrika.common.messaging.impl.protocols.composite.MessageRouter;
import com.exametrika.common.messaging.impl.protocols.composite.ProtocolStack;
import com.exametrika.common.messaging.impl.protocols.composite.ProtocolSubStack;
import com.exametrika.common.messaging.impl.protocols.error.UnhandledMessageProtocol;
import com.exametrika.common.messaging.impl.protocols.failuredetection.ChannelObserver;
import com.exametrika.common.messaging.impl.protocols.failuredetection.ICleanupManager;
import com.exametrika.common.messaging.impl.protocols.failuredetection.IFailureObserver;
import com.exametrika.common.messaging.impl.protocols.failuredetection.LiveNodeManager;
import com.exametrika.common.messaging.impl.transports.ConnectionManager;
import com.exametrika.common.tests.Tests;
import com.exametrika.common.time.impl.SystemTimeService;


/**
 * The {@link CompositeProtocolTests} are tests for composite protocol implementations.
 *
 * @author Medvedev-A
 * @see ProtocolSubStack
 * @see MessageRouter
 */
public class CompositeProtocolTests {
    @Test
    public void testProtocol() throws Exception {
        IAddress member2 = new TestAddress(UUID.randomUUID(), "member2");

        ChannelObserver channelObserver = new ChannelObserver("test");
        LiveNodeManager liveNodeManager = new LiveNodeManager("member1", Arrays.<IFailureObserver>asList(channelObserver), channelObserver);
        IAddress member1 = liveNodeManager.setLocalNode(0, "", "");
        liveNodeManager.onNodesConnected(Collections.singleton(member2));

        SerializationRegistry registry = new SerializationRegistry();

        IMessageFactory messageFactory = new MessageFactory(registry, liveNodeManager);

        TestLeafProtocol leaf1 = new TestLeafProtocol("test", messageFactory);
        TestProtocol parent1 = new TestProtocol("test", messageFactory);
        TestLeafProtocol leaf2 = new TestLeafProtocol("test", messageFactory);
        TestProtocol parent2 = new TestProtocol("test", messageFactory);
        TestRootProtocol root = new TestRootProtocol("test", messageFactory);
        UnhandledMessageProtocol error = new UnhandledMessageProtocol("test", messageFactory);

        TestProtocol[] protocols = new TestProtocol[]{leaf1, leaf2, parent1, parent2, root};

        ProtocolSubStack subStack1 = new ProtocolSubStack("test", messageFactory, Arrays.asList(leaf1, parent1));
        ProtocolSubStack subStack2 = new ProtocolSubStack("test", messageFactory, Arrays.asList(leaf2, parent2));

        MessageRouter router = new TestMessageRouter("test", messageFactory, subStack1, subStack2);

        ProtocolStack stack = new ProtocolStack("test", Arrays.asList(error, router, root), liveNodeManager, 100, 1000);
        stack.setTimeService(new SystemTimeService());
        ConnectionManager connectionManager = new ConnectionManager(0, Collections.<String, IConnectionProvider>emptyMap());
        stack.setConnectionProvider(connectionManager);

        stack.register(registry);
        stack.unregister(registry);

        stack.start();

        stack.onTimer(100);

        root.receive(messageFactory.create(member1, MessageFlags.HIGH_PRIORITY));
        root.receive(messageFactory.create(member1, MessageFlags.LOW_PRIORITY));
        root.receive(messageFactory.create(member1, MessageFlags.PARALLEL));
        router.send(messageFactory.create(member1, MessageFlags.HIGH_PRIORITY));
        router.send(messageFactory.create(member1, MessageFlags.LOW_PRIORITY));
        router.send(messageFactory.create(member1, MessageFlags.PARALLEL));

        stack.stop();

        for (TestProtocol protocol : protocols) {
            assertTrue(protocol.started);
            assertTrue(protocol.stopped);
            assertTrue(protocol.timered);
            assertTrue(protocol.cleanedup);
            assertTrue(protocol.getTimeService() != null);
            assertTrue(Tests.get(protocol, "connectionProvider") != null);
        }

        assertTrue(leaf1.registered);
        assertTrue(parent1.registered);
        assertTrue(!leaf2.registered);
        assertTrue(!parent2.registered);
        assertTrue(root.registered);

        assertTrue(leaf1.receiveMessages.size() == 1);
        assertEquals(leaf1.receiveMessages.get(0).getDestination(), member1);
        assertEquals(leaf1.receiveMessages.get(0).getFlags(), MessageFlags.HIGH_PRIORITY);

        assertTrue(leaf2.receiveMessages.size() == 1);
        assertEquals(leaf2.receiveMessages.get(0).getDestination(), member1);
        assertEquals(leaf2.receiveMessages.get(0).getFlags(), MessageFlags.LOW_PRIORITY);

        assertTrue(root.sendMessages.size() == 3);
        assertEquals(root.sendMessages.get(0).getDestination(), member1);
        assertEquals(root.sendMessages.get(0).getFlags(), MessageFlags.HIGH_PRIORITY);
        assertEquals(root.sendMessages.get(1).getDestination(), member1);
        assertEquals(root.sendMessages.get(1).getFlags(), MessageFlags.LOW_PRIORITY);
        assertEquals(root.sendMessages.get(2).getDestination(), member1);
        assertEquals(root.sendMessages.get(2).getFlags(), MessageFlags.PARALLEL);
    }

    private static class TestProtocol extends AbstractProtocol {
        private boolean started;
        private boolean stopped;
        public boolean registered;
        private boolean timered;
        private boolean cleanedup;

        public TestProtocol(String channelName, IMessageFactory messageFactory) {
            super(channelName, messageFactory);
        }

        @Override
        public void start() {
            super.start();
            started = true;
        }

        @Override
        public void stop() {
            stopped = true;
            super.stop();
        }

        @Override
        public void register(ISerializationRegistry registry) {
            registered = true;
        }

        @Override
        public void unregister(ISerializationRegistry registry) {
        }

        @Override
        public void onTimer(long currentTime) {
            timered = true;
        }

        @Override
        public void cleanup(ICleanupManager cleanupManager, ILiveNodeProvider liveNodeProvider, long currentTime) {
            cleanedup = true;
        }
    }

    private static class TestLeafProtocol extends TestProtocol {
        private List<IMessage> receiveMessages = new ArrayList<IMessage>();
        private List<IMessage> sendMessages = new ArrayList<IMessage>();

        public TestLeafProtocol(String channelName, IMessageFactory messageFactory) {
            super(channelName, messageFactory);
        }

        @Override
        public void start() {
            blankOffReceiver();
            super.start();
        }

        @Override
        protected void doReceive(IReceiver receiver, IMessage message) {
            receiveMessages.add(message);
        }

        @Override
        protected void doSend(ISender sender, IMessage message) {
            sendMessages.add(message);
            super.doSend(sender, message);
        }
    }

    private static class TestRootProtocol extends TestProtocol {
        private List<IMessage> sendMessages = new ArrayList<IMessage>();

        public TestRootProtocol(String channelName, IMessageFactory messageFactory) {
            super(channelName, messageFactory);
        }

        @Override
        public void start() {
            blankOffSender();
            super.start();
        }

        @Override
        protected void doSend(ISender sender, IMessage message) {
            sendMessages.add(message);
        }
    }

    private static class TestMessageRouter extends MessageRouter {
        private final AbstractProtocol protocol1;
        private final AbstractProtocol protocol2;

        public TestMessageRouter(String channelName, IMessageFactory messageFactory, AbstractProtocol protocol1,
                                 AbstractProtocol protocol2) {
            super(channelName, messageFactory);

            this.protocol1 = protocol1;
            this.protocol2 = protocol2;
        }

        @Override
        public void start() {
            super.start();

            addProtocol(protocol1);
            addProtocol(protocol2);
        }

        @Override
        protected boolean doReceiveRoute(IMessage message) {
            if (message.hasFlags(MessageFlags.HIGH_PRIORITY)) {
                protocol1.receive(message);
                return true;
            } else if (message.hasFlags(MessageFlags.LOW_PRIORITY)) {
                protocol2.receive(message);
                return true;
            } else
                return false;
        }

        @Override
        protected boolean doSendRoute(IMessage message) {
            if (message.hasFlags(MessageFlags.HIGH_PRIORITY)) {
                protocol1.send(message);
                return true;
            } else if (message.hasFlags(MessageFlags.LOW_PRIORITY)) {
                protocol2.send(message);
                return true;
            } else
                return false;
        }

        @Override
        protected ISink doRegisterRoute(IAddress destination, IFeed feed) {
            return null;
        }

        @Override
        protected boolean doUnregisterRoute(ISink sink) {
            return false;
        }
    }
}
