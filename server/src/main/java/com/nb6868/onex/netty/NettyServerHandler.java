package com.nb6868.onex.netty;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 自定义一个Handler，需要继承Netty规定好的某个HandlerAdapter
 * 自定义一个Handler，才能称为一个Handler
 *
 * 若有需要，可用NettyChannelMap管理channel
 */
@Slf4j
@Component
@ChannelHandler.Sharable
public class NettyServerHandler extends SimpleChannelInboundHandler<Object> {

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
        // 解决16进制编解码问题
        String message = "";
        byte[] bytebuf = ((String) msg).getBytes();
        // 此解码为解决 16进制在超过79的情况乱码问题
        for (int i = 0, len = bytebuf.length; i < len; i++) {
            String hex = Integer.toHexString(bytebuf[i] & 0xff).toUpperCase();
            message += hex.length() == 1 ? "0" + hex : hex;
        }
        log.info("channelRead0 --> 客户端【{}】发送来的消息 【{}】", ctx.channel().remoteAddress(), message);
        // 回复消息
        //byte[] returnByte = new byte[]{1, 2, 3,4, 5};
        ctx.channel().writeAndFlush("123456789");
        ctx.channel().eventLoop().execute(() -> {
            try {
                Thread.sleep(10*1000);
                log.info(">>>>>>>>>休眠十秒");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        log.info("handlerAdded --> 通道注册成功 {}", ctx.channel().remoteAddress());
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
