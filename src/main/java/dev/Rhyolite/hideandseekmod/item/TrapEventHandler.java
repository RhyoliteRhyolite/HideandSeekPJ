package dev.Rhyolite.hideandseekmod.item;

import dev.Rhyolite.hideandseekmod.block.TrapBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = "hideandseekmod") // 본인 모드 ID 확인
public class TrapEventHandler {

    // 플레이어별 웅크리기 시간을 저장하는 맵
    private static final Map<UUID, Integer> crouchTimers = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;

        // 덫에 걸린 사람("is_trapped")만 체크
        if (player.getTags().contains("is_trapped")) {

            if (player.isCrouching()) {
                // 웅크리고 있으면 타이머 증가
                UUID uuid = player.getUUID();
                int ticks = crouchTimers.getOrDefault(uuid, 0) + 1;
                crouchTimers.put(uuid, ticks);

                // 진행 상황 알림 (선택 사항, 너무 자주 뜨면 삭제 가능)
                if (ticks % 5 == 0) {
                    player.displayClientMessage(Component.literal("§7해체 중... " + (int)((ticks / 20.0) * 100) + "%"), true);
                }

                // [성공] 1초(20틱) 경과 시
                if (ticks >= 20) {
                    releasePlayer(player);
                    crouchTimers.remove(uuid);
                }
            } else {
                // 웅크리다 말았으면 타이머 초기화 (처음부터 다시)
                crouchTimers.remove(player.getUUID());
            }
        }
    }

    private static void releasePlayer(Player player) {
        // 1. 속성(이동/점프) 원상복구
        var speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        var jump = player.getAttribute(Attributes.JUMP_STRENGTH);
        if (speed != null) speed.removeModifier(TrapBlock.TRAP_SPEED_MOD);
        if (jump != null) jump.removeModifier(TrapBlock.TRAP_JUMP_MOD);

        player.removeTag("is_trapped");

        // 2. 덫 파괴 로직 개선 (주변 탐색)
        BlockPos playerPos = player.blockPosition();
        // 플레이어 발밑과 주변 3x2x3 범위를 검사하여 덫 블록을 찾음
        Iterable<BlockPos> checkRange = BlockPos.betweenClosed(
                playerPos.offset(-1, 0, -1),
                playerPos.offset(1, 1, 1)
        );

        for (BlockPos targetPos : checkRange) {
            if (player.level().getBlockState(targetPos).getBlock() instanceof TrapBlock) {
                player.level().destroyBlock(targetPos, false);
                break; // 덫 하나만 부수고 중단
            }
        }

        // 3. 연출
        player.displayClientMessage(Component.literal("§a§l덫을 해체했습니다!"), true);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.CHAIN_BREAK, SoundSource.PLAYERS, 1.0F, 1.0F);
    }
}