package com.hft.matching.infrastructure.adapter.nio.server;

import com.hft.matching.infrastructure.adapter.nio.codec.BinaryProtocolDecoder;
import com.hft.matching.infrastructure.adapter.nio.session.ClientSession;
import com.hft.matching.infrastructure.adapter.nio.session.SessionPool;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class NioReactor implements Runnable {

    private final Selector selector;
    private final ServerSocketChannel channel;

    private final SessionPool sessionPool;
    private final BinaryProtocolDecoder decoder;
    private volatile boolean running = true;

    public NioReactor(Selector selector,
                      ServerSocketChannel channel,
                      SessionPool sessionPool,
                      BinaryProtocolDecoder decoder) {
        this.selector = selector;
        this.channel = channel;
        this.sessionPool = sessionPool;
        this.decoder = decoder;
    }

    @Override
    public void run() {
        while (running) {
            try {
                int readyChannels = selector.select();
                if (readyChannels == 0) continue;
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                Iterator<SelectionKey> keyIterator = selectionKeys.iterator();
                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();
                    keyIterator.remove();
                    if (!key.isValid()) continue;
                    if (key.isAcceptable()) {
                        SocketChannel sessionChannel = channel.accept();
                        if (sessionChannel == null) continue;
                        ClientSession session = sessionPool.acquire();
                        if (session == null) {
                            sessionChannel.close();
                            continue;
                        }
                        sessionChannel.configureBlocking(false);
                        session.attach(sessionChannel);
                        sessionChannel.register(selector, SelectionKey.OP_READ, session);
                    } else if (key.isReadable()) {
                        ClientSession session = (ClientSession) key.attachment();
                        SocketChannel sessionChannel = session.getChannel();
                        ByteBuffer sessionBuffer = session.getReadBuffer();
                        try {
                            int bytesRead = sessionChannel.read(sessionBuffer);
                            if (bytesRead == -1) {
                                key.cancel();
                                sessionPool.release(session.getIndex());
                            }
                            if (bytesRead > 0) {
                                sessionBuffer.flip();
                                decoder.decode(sessionBuffer);
                                sessionBuffer.compact();
                            }
                        } catch (IOException e) {
                            key.cancel();
                            sessionPool.release(session.getIndex());
                        }
                    } else if (key.isWritable()) {
                        ClientSession session = (ClientSession) key.attachment();
                        SocketChannel sessionChannel = session.getChannel();
                        ByteBuffer sessionBuffer = session.getWriteBuffer();
                        try {
                            sessionChannel.write(sessionBuffer);
                            if (!sessionBuffer.hasRemaining()) {
                                key.interestOps(SelectionKey.OP_READ);
                                sessionBuffer.clear();
                            }
                        } catch (IOException e) {
                            key.cancel();
                            sessionPool.release(session.getIndex());
                        }
                    }
                }
             } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void shutdown() {
        running = false;
        selector.wakeup();
    }
}
