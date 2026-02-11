package dev.Rhyolite.hideandseekmod.event;

import dev.Rhyolite.hideandseekmod.HideandSeekMod;
import dev.Rhyolite.hideandseekmod.logic.GameManager;
import dev.Rhyolite.hideandseekmod.logic.ItemPicker;
import dev.Rhyolite.hideandseekmod.network.JumpscarePayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;

@EventBusSubscriber(modid = HideandSeekMod.MODID, bus = EventBusSubscriber.Bus.GAME)
public class GameEvents {

    @SubscribeEvent
    public static void onPlayerAttack(AttackEntityEvent event) {
        // 서버에서만 작동
        if (event.getEntity().level().isClientSide) return;


        if (event.getEntity() instanceof ServerPlayer attacker && event.getTarget() instanceof ServerPlayer victim) {
            // 술래가 도망자를 공격했는지 확인
            if (GameManager.get().isKiller(attacker) && GameManager.get().isRunner(victim)) {
                // 도망자에게 점프스캐어 패킷 전송
                PacketDistributor.sendToPlayer(victim, new JumpscarePayload("jumpscare"));
            }
        }
    }
    // 1. 플레이어별 쿨타임 (어디서든 아이템을 뽑은 후 대기시간)
    private static final Map<UUID, Long> playerCooldowns = new HashMap<>();
    // 2. 이미 사용된 블록의 좌표 저장 (서버가 켜져있는 동안 유지)
    private static final Set<BlockPos> usedSupplyBlocks = new HashSet<>();

    private static final long COOLDOWN_TICKS = 1200L; // 1분

    @SubscribeEvent
    public static void onBlockClick(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        Player player = event.getEntity();
        BlockPos pos = event.getPos();

        if (level.isClientSide) return;

        // 철 블록(보급소)인지 확인
        if (level.getBlockState(pos).is(Blocks.IRON_BLOCK)) {
            UUID uuid = player.getUUID();
            long currentTime = level.getGameTime();

            // [추가된 로직] 1. 블록 중복 사용 체크
            if (usedSupplyBlocks.contains(pos)) {
                player.displayClientMessage(Component.literal("§c안에 아무것도 없습니다. 다른 곳을 찾아보세요!"), true);
                return;
            }

            // 2. 플레이어 쿨타임 체크
            if (playerCooldowns.containsKey(uuid)) {
                long nextTime = playerCooldowns.get(uuid);
                if (currentTime < nextTime) {
                    long remainingSeconds = (nextTime - currentTime) / 20;
                    player.displayClientMessage(Component.literal("§6아직 새로운 아이템을 받을 준비가 되지 않았습니다. (남은 시간: " + remainingSeconds + "초)"), true);
                    return;
                }
            }

            // 3. 아이템 지급 및 정보 저장
            ItemPicker.giveRandomItem(player);

            // 이 블록은 사용됨으로 표시
            usedSupplyBlocks.add(pos);
            // 플레이어에게 쿨타임 적용
            playerCooldowns.put(uuid, currentTime + COOLDOWN_TICKS);

            level.playSound(null, pos, net.minecraft.sounds.SoundEvents.CHEST_OPEN, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);


        }

    }
    public static void resetGame() {
        // 사용된 블록 기록 삭제
        usedSupplyBlocks.clear();
        // 플레이어 쿨타임 기록 삭제
        playerCooldowns.clear();

        // 콘솔이나 서버 로그에 표시 (선택 사항)
        System.out.println("[HideAndSeek] 게임 데이터가 초기화되었습니다.");
    }
}