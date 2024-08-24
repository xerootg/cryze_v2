package com.tencentcs.iotvideo.restreamer

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.bytes.ByteArrayEncoder
import io.netty.handler.stream.ChunkedWriteHandler
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicLong

class ByteStreamServer(private val port: Int) : Runnable {

    private val bossGroup = NioEventLoopGroup(1)
    private val workerGroup = NioEventLoopGroup()
    private val channels = CopyOnWriteArrayList<Channel>()
    private var header: ByteArray? = null
    private var serverChannel: Channel? = null
    private val packetsFlushed = AtomicLong(0)

    override fun run() {
        try {
            val b = ServerBootstrap()
            b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel::class.java)
                .childHandler(object : ChannelInitializer<SocketChannel>() {
                    override fun initChannel(ch: SocketChannel) {
                        ch.pipeline().addLast(
                            ChunkedWriteHandler(),
                            ByteArrayEncoder(),
                            ByteStreamHandler(channels, ::header, packetsFlushed)
                        )
                    }
                })
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true)

            val f: ChannelFuture = b.bind(port).sync()
            serverChannel = f.channel()
            serverChannel?.closeFuture()?.sync()
        } finally {
            shutdown()
        }
    }

    fun sendBytes(data: ByteArray) {
        for (channel in channels) {
            channel.writeAndFlush(data).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)
            packetsFlushed.incrementAndGet()
        }
    }

    fun setHeader(header: ByteArray) {
        this.header = header
        for (channel in channels) {
            channel.writeAndFlush(header).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)
            packetsFlushed.incrementAndGet()
        }
    }

    fun shutdown() {
        serverChannel?.close()
        bossGroup.shutdownGracefully()
        workerGroup.shutdownGracefully()
    }

    override fun toString(): String {
        return "ByteStreamServer(port=$port, connectedClients=${channels.size}, packetsFlushed=${packetsFlushed.get()})"
    }
}

class ByteStreamHandler(
    private val channels: CopyOnWriteArrayList<Channel>,
    private val headerProvider: () -> ByteArray?,
    private val packetsFlushed: AtomicLong
) : SimpleChannelInboundHandler<ByteArray>() {

    override fun channelActive(ctx: ChannelHandlerContext) {
        channels.add(ctx.channel())
        headerProvider()?.let {
            ctx.writeAndFlush(it).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)
            packetsFlushed.incrementAndGet()
        }
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        channels.remove(ctx.channel())
    }

    override fun channelRead0(ctx: ChannelHandlerContext, msg: ByteArray) {
        // Handle incoming data if needed
    }
}
