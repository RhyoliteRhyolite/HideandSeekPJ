package dev.Rhyolite.hideandseekmod.item; // 본인의 패키지 경로로 꼭 확인하세요!

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent; // TickEvent 대신 이거 사용

@EventBusSubscriber(modid = "hideandseekmod") // modid를 본인 것으로 수정
public class TrapEventHandler {
    private static final ResourceLocation SPEED_ID = ResourceLocation.fromNamespaceAndPath("hideandseek", "trap_speed");
    private static final ResourceLocation JUMP_ID = ResourceLocation.fromNamespaceAndPath("hideandseek", "trap_jump");

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) { // 1.21은 Post 또는 Pre를 명시해야 함
        Player player = event.getEntity(); // event.player 대신 event.getEntity() 사용

        if (player.level().isClientSide) return;

        // 덫에 걸린 상태("is_trapped" 태그 보유)이고 웅크리고 있다면
        if (player.getTags().contains("is_trapped") && player.isCrouching()) {

            // 속성(Attribute) 원상복구
            var speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
            var jump = player.getAttribute(Attributes.JUMP_STRENGTH);

            if (speed != null) speed.removeModifier(SPEED_ID);
            if (jump != null) jump.removeModifier(JUMP_ID);

            // 태그 제거 및 알림
            player.removeTag("is_trapped");
            player.displayClientMessage(Component.literal("§e§l웅크려서 덫을 풀었습니다!"), true);
            player.level().playSound(null, player.blockPosition(), SoundEvents.CHAIN_BREAK, SoundSource.PLAYERS, 1.0F, 1.2F);
        }
    }
}