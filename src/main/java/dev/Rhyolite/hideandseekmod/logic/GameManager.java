package dev.Rhyolite.hideandseekmod.logic;

import dev.Rhyolite.hideandseekmod.item.ITEMS;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import java.util.*;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import java.util.List;

import static dev.Rhyolite.hideandseekmod.block.TrapBlock.TRAP_JUMP_MOD;

public class GameManager {
        public static boolean isGameActive = false;
        public static int gameTimer = 7200;       // 6분 (6분 * 60초 * 20틱 = 7200)
        public static int seekerWaitTimer = 300;  // 15초 (15 * 20 = 300)


        public static final ServerBossEvent GAME_TIMER_BAR = new ServerBossEvent(
                Component.literal("§e남은 시간"),
                BossEvent.BossBarColor.YELLOW,
                BossEvent.BossBarOverlay.PROGRESS
        );

        // 게임 시작 로직
        public static void startGame(ServerLevel level) {
            List<ServerPlayer> players = level.players();
            if (players.size() < 2) {
                // 혼자서 테스트할 때는 이 줄을 주석 처리하세요
                // return;
            }

            isGameActive = true;
            gameTimer = 7200;      // 6분 리셋
            seekerWaitTimer = 300; // 15초 리셋

            // 1. 역할 분배 (랜덤 셔플)
            Collections.shuffle(players);
            ServerPlayer seeker = players.get(0); // 첫 번째 사람이 술래

            for (ServerPlayer player : players) {
                // 상태 초기화 (체력, 효과 등)
                player.removeAllEffects();
                player.heal(20.0F);
                player.getFoodData().setFoodLevel(20);
                player.getInventory().clearContent(); // 인벤토리 초기화
                player.getPersistentData().putInt("taunt_count", 0); // 도발 횟수 초기화

                if (player.equals(seeker)) {
                    // [술래 설정]
                    player.addTag("seeker");
                    // 실명 & 이동 불가 (구속 레벨 255는 아예 못 움직임)
                    player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 300, 0, false, false));
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 300, 255, false, false));
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 300, 255, false, false)); // 대기 중 무적


                    // 술래 대기 좌표 (원하는 좌표로 수정하세요)
                    player.teleportTo(33, 0, 17);
                    player.setDeltaMovement(0, 0, 0);
                    player.sendSystemMessage(Component.literal("§c[!] 당신은 술래입니다! 15초 뒤에 풀려납니다."));
                } else {
                    // [도망자 설정]
                    player.removeTag("seeker");
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 7200, 0, false, false));

                    double minX = 36;
                    double maxX = 55;
                    double minZ = -19;
                    double maxZ = 56.0;
                    double maxY = 27;
                    double minY = 9;

                    double rx = minX + (Math.random() * (maxX - minX));
                    double rz = minZ + (Math.random() * (maxZ - minZ));
                    double ry = minY + (Math.random() * (maxY - minY));

                    player.teleportTo(rx, ry, rz);
                    player.sendSystemMessage(Component.literal("§a[!] 당신은 도망자입니다! 빨리 숨으세요!"));
                }
            }


            level.players().forEach(GAME_TIMER_BAR::addPlayer);
            GAME_TIMER_BAR.setVisible(true);
            level.getServer().getPlayerList().broadcastSystemMessage(Component.literal("§e=== 숨바꼭질 게임이 시작되었습니다! (제한시간: 6분) ==="), false);
        }

        // 게임 종료 로직
        public static void stopGame(ServerLevel level, String reason) {
            isGameActive = false;
            level.getServer().getPlayerList().broadcastSystemMessage(Component.literal(reason), false);
            announceTauntStats(level);
            resetGame(level);

            // 통계 출력 (ModEvents나 다른 곳에 있는 함수 호출)
            // ModEvents.announceTauntStats(level);

            GAME_TIMER_BAR.removeAllPlayers();
            GAME_TIMER_BAR.setVisible(false);
        }
        public static void updateBossBar() {
            if (!isGameActive) return;

            // 진행률 계산 (0.0 ~ 1.0)
            float progress = (float) gameTimer / 7200.0F;
            GAME_TIMER_BAR.setProgress(Math.max(0.0F, progress));

            // 시간 포맷팅 (분:초)
            int seconds = gameTimer / 20;
            int min = seconds / 60;
            int sec = seconds % 60;

            String timeStr = String.format("§e§l남은 시간: %02d:%02d", min, sec);
            GAME_TIMER_BAR.setName(Component.literal(timeStr));

             // 시간에 따른 색상 변화 (선택 사항)
            if (gameTimer <= 1200) { // 1분 남았을 때 빨간색
                GAME_TIMER_BAR.setColor(BossEvent.BossBarColor.RED);
            } else {
                GAME_TIMER_BAR.setColor(BossEvent.BossBarColor.YELLOW);
        }
    }

        public static void giveHammers(ServerLevel level) {
            for (ServerPlayer player : level.players()) {
                // 도망자이면서 관전자가 아닌 사람에게만
                if (!player.getTags().contains("seeker") && !player.isSpectator()) {

                    // [수정됨] 커스텀 아이템(PICO_HAMMER) 지급
                    ItemStack hammer = new ItemStack(ITEMS.PICO_HAMMER.get());

                    // 이름이나 인챈트가 필요하면 추가
                    hammer.set(DataComponents.CUSTOM_NAME, Component.literal("§6§l뿅망치"));

                    if (player.getInventory().add(hammer)) {
                        player.sendSystemMessage(Component.literal("§6[!] 5분이 지났습니다! 뿅망치를 받아 술래를 응징하세요!"));
                    }
                }
            }
        }

    public static void announceTauntStats(Level level) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        // 2. serverLevel.players()는 확실하게 List<ServerPlayer>를 반환합니다.
        List<ServerPlayer> players = serverLevel.players();

        int totalTaunts = 0;
        StringBuilder message = new StringBuilder("§b--- [ 도발 통계 ] ---\n");

        for (ServerPlayer player : players) {
            if (!player.getTags().contains("seeker") && !player.isSpectator()) {
                int count = player.getPersistentData().getInt("taunt_count");
                totalTaunts += count;
                message.append("§f").append(player.getScoreboardName())
                        .append(": §e").append(count).append("회\n");
            }
        }

        message.append("§b-------------------\n");
        message.append("§a모든 도망자의 총 도발 횟수: §l").append(totalTaunts).append("회");

        for (ServerPlayer p : players) {
            p.sendSystemMessage(Component.literal(message.toString()));
        }
    }

    // 모든 게임 상태를 깨끗하게 비우는 메서드
    public static void resetGame(ServerLevel level) {
        GameManager.isGameActive = false;
        GameManager.gameTimer = 7200;       // 6분 리셋
        GameManager.seekerWaitTimer = 300;  // 15초 리셋

        for (ServerPlayer player : level.players()) {
            // 1. 태그 제거
            player.removeTag("seeker");

            // 2. 효과 제거
            player.removeAllEffects();

            // 3. 인벤토리 및 데이터 초기화
            player.getPersistentData().putInt("cabinet_oxygen", 300);
            player.getPersistentData().putInt("taunt_count", 0);

            // 4. 관전자 모드였던 사람들을 다시 모험모드로 (선택 사항)
            if (player.isSpectator()) {
                player.setGameMode(GameType.ADVENTURE);
            }
        }

        // 5. 설치된 함정 정보 초기화 (Map 방식을 쓸 경우)
        // TrapEventHandler.TRAPPED_CANS.clear();

        level.getServer().getPlayerList().broadcastSystemMessage(
                Component.literal("§e[!] 게임 상태가 초기화되었습니다."), false
        );
    }
    public static void seekerWin(ServerLevel level) {
        isGameActive = false;
        GAME_TIMER_BAR.removeAllPlayers(); // 타이머 바 제거

        String winMessage = "\n§c§l[ 게임 종료 ]\n" +
                "§6§l술래가 모든 도망자를 찾아냈습니다!\n" +
                "§e§l술래 승리!!\n";

        level.getServer().getPlayerList().broadcastSystemMessage(Component.literal(winMessage), false);

        // 필요 시 모든 플레이어를 스폰 지점으로 텔레포트시키거나
        // 통계(도발 횟수 등)를 여기서 호출할 수 있습니다.
    }
    public static int getRemainingRunners(ServerLevel level) {
        int count = 0;
        for (ServerPlayer player : level.players()) {
            // 술래가 아니고, 관전 모드도 아닌 사람(아직 살아있는 도망자)
            if (!player.getTags().contains("seeker") && !player.isSpectator()) {
                count++;
            }
        }
        return count;
    }
}