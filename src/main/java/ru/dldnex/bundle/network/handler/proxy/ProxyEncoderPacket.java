package ru.dldnex.bundle.network.handler.proxy;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.MessageToByteEncoderUtils;
import net.minecraft.server.v1_16_R3.Packet;

public class ProxyEncoderPacket extends MessageToByteEncoder<Packet<?>> {
    private final MessageToByteEncoder<Packet<?>> encoder;

    public ProxyEncoderPacket(MessageToByteEncoder<Packet<?>> encoder) {
        this.encoder = encoder;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Packet<?> msg, ByteBuf out) throws Exception {
        MessageToByteEncoderUtils.encode(this.encoder, ctx, msg, out);
    }
}
