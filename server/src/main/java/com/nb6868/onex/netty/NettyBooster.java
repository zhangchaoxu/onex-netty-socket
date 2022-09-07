package com.nb6868.onex.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import cn.hutool.core.util.ObjectUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Slf4j
@Component
public class NettyBooster {

    @Value("${netty.server.port}")
    private int nettyPort;

    private final NettyServerHandler nettyServerHandler;

    // bossGroup 只是处理连接请求，真正的和客户端业务处理，会交给 workerGroup 完成
    // 创建BossGroup，这里指定线程数1就够了，bossGroup 就相当于领导，workerGroup 就相当于员工，领导有一个差不多了
    private EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    //创建WorkerGroup
    private EventLoopGroup workerGroup = new NioEventLoopGroup();

    public NettyBooster(NettyServerHandler nettyServerHandler) {
        this.nettyServerHandler = nettyServerHandler;
    }

    @PostConstruct
    public void start() throws InterruptedException {
        //创建服务器端的启动对象，配置参数
        ServerBootstrap serverBootstrap =
                new ServerBootstrap()
                        .group(bossGroup, workerGroup)
                        // 使用NioServerSocketChannel作为服务器的通道实现
                        .channel(NioServerSocketChannel.class)
                        // 设置日志处理器
                        .handler(new LoggingHandler(LogLevel.INFO))
                        // 设置线程队列得到的连接个数
                        .option(ChannelOption.SO_BACKLOG, 128)
                        // 设置保持活动连接的状态
                        .childOption(ChannelOption.SO_KEEPALIVE, true)
                        // 通过NoDelay禁用Nagle,消息立即发出去，不用等待到一定的数据量才发出去
                        .childOption(ChannelOption.TCP_NODELAY, true)
                        // 给我们的workerGroup的EventLoop对应的管道设置处理器
                        .childHandler(new ChannelInitializer<SocketChannel>() {
                            /**
                             * 给pipeline设置处理器
                             */
                            @Override
                            protected void initChannel(SocketChannel socketChannel) {
                                socketChannel.pipeline().addLast(
                                        // 设置一个空闲状态处理程序（心跳机制），读空闲，写空闲，读写空闲
                                        new IdleStateHandler(0, 0, 0)
                                );
                                // 打印分隔符,分包处理
                                // ByteBuf delimiter = Unpooled.copiedBuffer(new byte[]{16,3});
                                // socketChannel.pipeline().addLast("frameDecoder", new DelimiterBasedFrameDecoder(1024,false, delimiter));
                                // 属于ChannelOutboundHandler，依照逆序执行
                                socketChannel.pipeline().addLast("encoder", new StringEncoder());
                                // 属于ChannelInboundHandler，依照顺序执行
                                socketChannel.pipeline().addLast("decoder", new StringDecoder());
                                socketChannel.pipeline().addLast(nettyServerHandler);
                            }
                        });
        ChannelFuture cf = serverBootstrap.bind(nettyPort).sync();
        if (cf.isSuccess()) {
            log.info("NettyBooster.start --> Netty 启动成功");
        } else {
            log.error("NettyBooster.start --> Netty 启动失败");
        }
        //对关闭通道进行监听
        cf.channel().closeFuture().sync();
    }

    /**
     * Springboot关闭时关闭Netty
     */
    @PreDestroy
    private void destroy() throws InterruptedException {
        log.info("NettyBooster.destroy --> : 关闭 Netty...");
        if (ObjectUtil.isNotNull(bossGroup)) {
            bossGroup.shutdownGracefully().sync();
        }
        if (ObjectUtil.isNotNull(workerGroup)) {
            workerGroup.shutdownGracefully().sync();
        }
        log.info("NettyBooster.destroy --> : Netty 线程已全部关闭...");
    }

}
