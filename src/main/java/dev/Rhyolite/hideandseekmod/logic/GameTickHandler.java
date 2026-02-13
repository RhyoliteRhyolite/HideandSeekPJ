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
        // 게임 중이 아니면 실행 안 함
        if (!GameManager.isGameActive) return;

        GameManager.updateBossBar();

        ServerLevel level = event.getServer().overworld(); // 오버월드 기준

        // 1. 술래 대기 타이머 (15초)
        if (GameManager.seekerWaitTimer > 0) {
            GameManager.seekerWaitTimer--;

            // 5초 남았을 때 (10초 경과) - 아이템 털기 경고
            if (GameManager.seekerWaitTimer == 100) {
                event.getServer().getPlayerList().broadcastSystemMessage(Component.literal("§e[!] 술래 해방 5초 전! 도망갈 준비 하세요!"), false);
            }

            // 0초 되었을 때 - 술래 해방
            if (GameManager.seekerWaitTimer == 0) {
                event.getServer().getPlayerList().broadcastSystemMessage(Component.literal("§c§l[!] 술래가 풀려났습니다! 모두 도망치세요!"), false);
                event.getServer().getPlayerList().broadcastSystemMessage(Component.literal("§7(술래는 실명과 구속이 해제되었습니다)"), false);
                for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
                    if (player.getTags().contains("seeker")) {
                        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 7200, 1, false, false));
                        player.sendSystemMessage(Component.literal("§c§l[!] 도망자를 모두 죽이십시오!"));
                    }
                    // 효과는 시간이 다 되면 자동으로 사라지므로 별도 제거 코드 불필요
                }
            }

            // 2. 메인 게임 타이머 (6분 = 7200틱)
            if (GameManager.gameTimer > 0) {
                GameManager.gameTimer--;

                // [핵심] 5분 경과 (남은 시간 1분 = 1200틱) -> 뿅망치 지급
                if (GameManager.gameTimer == 1200) {
                    GameManager.giveHammers(level);
                    event.getServer().getPlayerList().broadcastSystemMessage(Component.literal("§b[!] 남은 시간 1분! 도망자들의 반격이 시작됩니다!"), false);
                }

                // 1분 남았을 때 (위와 겹치지만 메시지용으로 따로 쓸 수 있음)
                // if (GameManager.gameTimer == 1200) { ... }

                // 10초 남았을 때 카운트다운
                if (GameManager.gameTimer <= 200 && GameManager.gameTimer % 20 == 0 && GameManager.gameTimer > 0) {
                    event.getServer().getPlayerList().broadcastSystemMessage(Component.literal("§c남은 시간: " + (GameManager.gameTimer / 20) + "초"), true);
                }

            } else {
                // 3. 게임 종료 (시간 0됨)
                GameManager.stopGame(level, "§b§l[!] 제한시간 종료! 도망자 승리!");
            }
        }
    }
    @SubscribeEvent
    public static void onClientTick (ClientTickEvent.Post event){
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