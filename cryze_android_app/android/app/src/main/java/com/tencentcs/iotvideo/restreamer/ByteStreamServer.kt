package com.tencentcs.iotvideo.restreamer

import com.tencentcs.iotvideo.utils.LogUtils
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.ChannelPromise
import io.netty.channel.SimpleChannelInboundHandler
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

    // a prefix sent to all new connections
    fun setHeader(header: ByteArray) {
        this.header = header
        for (channel in channels) { // send to all already connected clients
            channel.writeAndFlush(header)
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
    private val packetsFlushed: AtomicLong //
) : SimpleChannelInboundHandler<ByteArray>() {

    override fun channelActive(ctx: ChannelHandlerContext) {
        channels.add(ctx.channel())
        // send the header "prefix" to the new client if there is one
        headerProvider()?.let {
            ctx.writeAndFlush(it)
            packetsFlushed.incrementAndGet()
        }
    }

    // called when a client disconnects for any reason
    override fun channelInactive(ctx: ChannelHandlerContext) {
        channels.remove(ctx.channel())
    }

    // no-op, this is a yeeting style server
    override fun channelRead0(ctx: ChannelHandlerContext, msg: ByteArray) {
    }

    @Deprecated("Reasons.")
    override fun exceptionCaught(ctx: ChannelHandlerContext?, cause: Throwable?) {
        channels.remove(ctx?.channel())
        LogUtils.e("ByteStreamServer", "pruned channel for exception: ${cause?.message}")
    }
}
