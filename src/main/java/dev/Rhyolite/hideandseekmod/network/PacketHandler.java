package dev.Rhyolite.hideandseekmod.network;

import dev.Rhyolite.hideandseekmod.HideandSeekMod;
import dev.Rhyolite.hideandseekmod.client.JumpscareOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class PacketHandler {
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(HideandSeekMod.MODID);

        registrar.playToClient(
                JumpscarePayload.TYPE,
                JumpscarePayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        // 화면 효과 발동
                        JumpscareOverlay.trigger();
                        // 소리 재생
                        if (Minecraft.getInstance().player != null) {
                            Minecraft.getInstance().player.playSound(SoundEvents.ENDERMAN_SCREAM, 1.0f, 0.5f);
                        }
                    });
                }
        );
    }
}