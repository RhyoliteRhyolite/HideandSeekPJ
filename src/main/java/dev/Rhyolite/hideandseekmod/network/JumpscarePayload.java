package dev.Rhyolite.hideandseekmod.network;

import dev.Rhyolite.hideandseekmod.HideandSeekMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 서버에서 클라이언트로 "점프스캐어 띄워!"라고 보내는 데이터 묶음입니다.
 */
public record JumpscarePayload(String imageName) implements CustomPacketPayload {

    // 패킷의 고유 ID 설정
    public static final Type<JumpscarePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(HideandSeekMod.MODID, "asset.hideandseekmod.textures.gui.jumpscare.png"));

    // 데이터를 0과 1로 변환(직렬화)하는 코덱
    public static final StreamCodec<ByteBuf, JumpscarePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            JumpscarePayload::imageName,
            JumpscarePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}