package com.nb6868.onex.netty;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.HexUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.concurrent.GlobalEventExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.Charset;

/**
 * 自定义一个Handler，需要继承Netty规定好的某个HandlerAdapter
 * 自定义一个Handler，才能称为一个Handler
 * <p>
 * 若有需要，可用NettyChannelMap管理channel
 */
@Slf4j
@Component
@ChannelHandler.Sharable
public class NettyServerHandler extends SimpleChannelInboundHandler<Object> {

    /**
     * 定义一个channel组管理所有channel
     * GlobalEventExecutor.INSTANCE 是一个全局事件执行器 是一个单例
     */
    private static ChannelGroup channelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        // 通过判断IdleStateEvent的状态来实现自己的读空闲，写空闲，读写空闲处理逻辑
        if (evt instanceof IdleStateEvent && ((IdleStateEvent) evt).state() == IdleState.READER_IDLE) {
            //读空闲，关闭通道
            log.info("userEventTriggered -->用户事件触发 读空闲，关闭通道 {}", ctx.channel().remoteAddress());
            ctx.close();
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
        // 通道加入map
        String clientId = ctx.channel().remoteAddress().toString();
        NettyChannelMap.add(clientId, (SocketChannel) ctx.channel());
        // 收到的是byte数组
        String message = HexUtil.encodeHexStr((byte[]) msg);
        log.info("channelRead0 --> 客户端【{}】发送来的消息Str 【{}】", ctx.channel().remoteAddress(), message);
        // 回复消息
        byte[] returnString = HexUtil.decodeHex("EF10090908");
        log.info("channelRead0 --> 返回消息Hex");
        ctx.channel().writeAndFlush(returnString);
        ctx.channel().eventLoop().execute(() -> {
            ThreadUtil.sleep(200);
            log.info("发送完成");
        });
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        log.info("handlerAdded --> 通道注册成功 {}", ctx.channel().remoteAddress());
        channelGroup.add(ctx.channel());
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        log.info("handlerRemoved --> 通道移除 --> {}", ctx.channel().remoteAddress());
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        log.info("channelActive --> 通道开启 --> {}", ctx.channel().remoteAddress());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.info("channelInactive --> 通道关闭 --> {}", ctx.channel().remoteAddress());
        NettyChannelMap.remove((SocketChannel) ctx.channel());
    }

    /**
     * 处理异常，一般是需要关闭通道
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("exceptionCaught", cause);
        ctx.close();
    }

}
