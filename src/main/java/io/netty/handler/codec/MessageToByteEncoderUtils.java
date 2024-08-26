package io.netty.handler.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

public class MessageToByteEncoderUtils {
    public static <T> void encode(MessageToByteEncoder<T> encoder, ChannelHandlerContext ctx, T msg, ByteBuf out) throws Exception {
        encoder.encode(ctx, msg, out);
    }
}
