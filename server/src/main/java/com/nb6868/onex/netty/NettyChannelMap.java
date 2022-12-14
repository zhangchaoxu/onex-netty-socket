package com.nb6868.onex.netty;

import java.util.Map;

import io.netty.channel.Channel;
import io.netty.channel.socket.SocketChannel;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 存放连接的channel对象
 */
public class NettyChannelMap {

    private static Map<String, SocketChannel> map = new ConcurrentHashMap<>();

    public static void add(String clientId, SocketChannel socketChannel) {
        if (null == get(clientId)) {
            map.put(clientId, socketChannel);
        }
    }

    public static Channel get(String clientId) {
        return map.get(clientId);
    }

    public static void remove(SocketChannel socketChannel) {
        for (Map.Entry entry : map.entrySet()) {
            if (entry.getValue() == socketChannel) {
                map.remove(entry.getKey());
            }
        }
    }

}
