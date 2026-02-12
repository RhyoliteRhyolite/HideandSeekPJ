package dev.Rhyolite.hideandseekmod.logic;

import dev.Rhyolite.hideandseekmod.item.ITEMS;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import java.util.*;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import java.util.List;

public class GameManager {
        public static boolean isGameActive = false;
        public static int gameTimer = 7200;       // 6분 (6분 * 60초 * 20틱 = 7200)
        public static int seekerWaitTimer = 300;  // 15초 (15 * 20 = 300)

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
                    player.teleportTo(100.5, 64, 100.5);
                    player.sendSystemMessage(Component.literal("§c[!] 당신은 술래입니다! 15초 뒤에 풀려납니다."));
                } else {
                    // [도망자 설정]
                    player.removeTag("seeker");

                    // 랜덤 좌표 생성 (예: X 100~150, Z 100~150 사이)
                    double rx = 100 + (Math.random() * 50);
                    double rz = 100 + (Math.random() * 50);
                    // Y좌표는 해당 위치의 가장 높은 블록 + 1
                    int ry = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, (int)rx, (int)rz) + 1;

                    player.teleportTo(rx, ry, rz);
                    player.sendSystemMessage(Component.literal("§a[!] 당신은 도망자입니다! 빨리 숨으세요!"));
                }
            }

            level.getServer().getPlayerList().broadcastSystemMessage(Component.literal("§e=== 숨바꼭질 게임이 시작되었습니다! (제한시간: 6분) ==="), false);
        }

        // 게임 종료 로직
        public static void stopGame(ServerLevel level, String reason) {
            isGameActive = false;
            level.getServer().getPlayerList().broadcastSystemMessage(Component.literal(reason), false);

            // 통계 출력 (ModEvents나 다른 곳에 있는 함수 호출)
            // ModEvents.announceTauntStats(level);
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
}