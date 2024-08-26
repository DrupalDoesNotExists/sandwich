package ru.dldnex.bundle.network.handler.proxy;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.MessageToByteEncoderUtils;

public class ProxyEncoderByteBuf extends MessageToByteEncoder<ByteBuf> {
    private final MessageToByteEncoder<ByteBuf> encoder;

    public ProxyEncoderByteBuf(MessageToByteEncoder<ByteBuf> encoder) {
        this.encoder = encoder;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) throws Exception {
        MessageToByteEncoderUtils.encode(this.encoder, ctx, msg, out);
    }
}
