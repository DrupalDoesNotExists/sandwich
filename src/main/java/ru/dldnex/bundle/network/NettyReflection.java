package ru.dldnex.bundle.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.MessageToMessageEncoder;
import ru.dldnex.bundle.exception.SandwichRuntimeException;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.List;

public class NettyReflection {

    public static final MethodHandle MESSAGE_TO_BYTE_ENCODER_ENCODE;
    public static final MethodHandle MESSAGE_TO_MESSAGE_ENCODER_ENCODE;

    static {
        try {
            MESSAGE_TO_BYTE_ENCODER_ENCODE = MethodHandles.privateLookupIn(MessageToByteEncoder.class, MethodHandles.lookup())
                    .findVirtual(MessageToByteEncoder.class, "encode",
                            MethodType.methodType(void.class, ChannelHandlerContext.class, Object.class, ByteBuf.class));
            MESSAGE_TO_MESSAGE_ENCODER_ENCODE = MethodHandles.privateLookupIn(MessageToMessageEncoder.class, MethodHandles.lookup())
                    .findVirtual(MessageToMessageEncoder.class, "encode",
                            MethodType.methodType(void.class, ChannelHandlerContext.class, Object.class, List.class));
        } catch (Exception e) {
            throw new SandwichRuntimeException("Unable to prepare injection instruments", e);
        }
    }

    public static <T> void encode(MessageToByteEncoder<T> encoder, ChannelHandlerContext ctx, T msg, ByteBuf out) {
        try {
            MESSAGE_TO_BYTE_ENCODER_ENCODE.invoke(encoder, ctx, msg, out);
        } catch (Throwable t) {
            t.printStackTrace();
            throw new RuntimeException("Unable to call MessageToByteEncoder#encode()", t);
        }
    }

    public static <T> void encode(MessageToMessageEncoder<T> encoder, ChannelHandlerContext ctx, T msg, List<Object> out) {
        try {
            MESSAGE_TO_MESSAGE_ENCODER_ENCODE.invoke(encoder, ctx, msg, out);
        } catch (Throwable t) {
            t.printStackTrace();
            throw new RuntimeException("Unable to call MessageToMessageEncoder#encode()", t);
        }
    }
}
