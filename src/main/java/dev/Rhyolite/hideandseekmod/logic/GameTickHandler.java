package dev.Rhyolite.hideandseekmod.logic; // 패키지명 확인

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = "hideandseekmod") // modid 확인
public class GameTickHandler {

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (!GameManager.isGameActive) return;

        ServerLevel level = event.getServer().overworld();

        // [요청 2] 상단 타이머 바 업데이트
        float progress = (float) GameManager.gameTimer / 7200.0f;
        GameManager.GAME_BAR.setProgress(progress);

        int minutes = (GameManager.gameTimer / 20) / 60;
        int seconds = (GameManager.gameTimer / 20) % 60;
        GameManager.GAME_BAR.setName(Component.literal(String.format("§e§l남은 시간: %02d:%02d", minutes, seconds)));

        // 1. 술래 대기 타이머 처리
        if (GameManager.seekerWaitTimer > 0) {
            GameManager.seekerWaitTimer--;
            if (GameManager.seekerWaitTimer == 0) {
                // [요청 1] 준비 시간이 끝나면 술래에게 이동 속도 증가 1 (2단계) 부여
                for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
                    if (player.getTags().contains("seeker")) {
                        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 7200, 1, false, false));
                        player.sendSystemMessage(Component.literal("§c§l[!] 도망자를 모두 죽이십시오!"));
                    }
                }
            }
        }

        // 2. 메인 게임 타이머 처리 (기존 동일)
        if (GameManager.gameTimer > 0) {
            GameManager.gameTimer--;

            // 5분 경과 시 뿅망치 지급 로직 (기존 동일)
            if (GameManager.gameTimer == 1200) {
                GameManager.giveHammers(level);
            }
        } else {
            GameManager.stopGame(level, "§b§l[!] 제한시간 종료! 도망자 승리!");
        }
    }
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();

        // 게임이 실행 중이고 플레이어가 존재할 때만 체크
        if (mc.player != null && mc.level != null) {
            // GameManager의 상태를 체크하거나, 특정 태그가 있는지 확인
            // 여기서는 예시로 모든 상황에서 1인칭을 강제합니다.
            if (mc.options.getCameraType() != CameraType.FIRST_PERSON) {
                mc.options.setCameraType(CameraType.FIRST_PERSON);
            }
        }
    }
}