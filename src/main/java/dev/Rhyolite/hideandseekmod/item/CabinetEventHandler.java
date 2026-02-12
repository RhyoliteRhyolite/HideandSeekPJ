package dev.Rhyolite.hideandseekmod.item;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = "hideandseekmod")
public class CabinetEventHandler {

    private static final int MAX_OXYGEN = 300; // 15초

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide || player.isSpectator()) return;

        // 캐비닛 탑승 중인지 확인
        if (player.getVehicle() instanceof ArmorStand seat && seat.getTags().contains("cabinet_seat")) {

            if (!player.hasEffect(MobEffects.INVISIBILITY)) {
                player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 0, false, false));
            } else {
                // 효과 지속시간 갱신 (캐비닛 안에 있는 동안 계속 유지)
                player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 5, 0, false, false));
            }

            // 1. 하차 로직 (Shift 누를 시)
            if (player.isCrouching()) {
                player.stopRiding();
                seat.discard();
                player.removeEffect(MobEffects.INVISIBILITY);
                return;
            }

            // 2. 산소 계산 로직
            int oxygen = player.getPersistentData().getInt("cabinet_oxygen");

            if (oxygen > 0) {
                oxygen--;
                player.getPersistentData().putInt("cabinet_oxygen", oxygen);
            }

            // 3. 액션바에 산소 UI 표시 (방울 아이콘 사용)
            displayOxygenBar(player, oxygen);

            // 4. 산소 고갈 시 대미지
            if (oxygen <= 0) {
                // 1초(20틱)마다 하트 2칸(4.0F)씩 대미지
                if (player.tickCount % 20 == 0) {
                    player.hurt(player.damageSources().drown(), 4.0F);
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.PLAYER_HURT_DROWN, SoundSource.PLAYERS, 1.0F, 1.0F);
                }
            }

        } else {
            // 캐비닛 밖에서는 산소 회복
            int oxygen = player.getPersistentData().getInt("cabinet_oxygen");
            if (oxygen < MAX_OXYGEN) {
                player.getPersistentData().putInt("cabinet_oxygen", oxygen + 5);
            }
        }
    }

    // 액션바 UI 생성 메서드
    private static void displayOxygenBar(Player player, int oxygen) {
        int bubbleCount = (int) Math.ceil(oxygen / 30.0); // 10단계 방울
        String bubbles = "🫧".repeat(Math.max(0, bubbleCount));
        String empty = "  ".repeat(Math.max(0, 10 - bubbleCount)); // 칸 맞춤용

        String color = (oxygen < 60) ? "§c" : "§b"; // 3초 남으면 빨간색으로 변경

        player.displayClientMessage(
                Component.literal(color + "남은 산소: [" + bubbles + empty + "§7] " + String.format("%.1f초", oxygen / 20.0)),
                true
        );
    }
}