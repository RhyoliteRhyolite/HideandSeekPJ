package dev.Rhyolite.hideandseekmod.event;

import dev.Rhyolite.hideandseekmod.HideandSeekMod;
import dev.Rhyolite.hideandseekmod.item.ITEMS;
import dev.Rhyolite.hideandseekmod.logic.GameManager;
import dev.Rhyolite.hideandseekmod.logic.ItemPicker;
import dev.Rhyolite.hideandseekmod.network.JumpscarePayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;

@EventBusSubscriber(modid = HideandSeekMod.MODID, bus = EventBusSubscriber.Bus.GAME)
public class GameEvents {

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!GameManager.isGameActive) return;

        // 1. 죽은 사람이 플레이어(도망자)인지 확인
        if (event.getEntity() instanceof ServerPlayer victim && !victim.getTags().contains("seeker")) {

            // 2. 죽인 원인(Source)이 플레이어(술래)인지 확인
            if (event.getSource().getEntity() instanceof ServerPlayer attacker && attacker.getTags().contains("seeker")) {

                // [핵심] 도망자에게 점프스케어 패킷 전송
                // (네트워크 채널 설정이 되어 있다고 가정합니다)
                PacketDistributor.sendToPlayer(victim, new JumpscarePayload("jumpscare"));

                // 추가 연출: 도망자를 관전 모드로 변경하거나 메시지 출력
                victim.setGameMode(GameType.SPECTATOR);
                attacker.sendSystemMessage(Component.literal("§c[!] 도망자를 잡았습니다!"));
                victim.sendSystemMessage(Component.literal("§4[!] 술래에게 잡혔습니다!"));

                // 킬 로그 숨기기 등을 원하면 event.setCanceled(true)를 고려할 수 있으나,
                // LivingDeathEvent는 취소 불가능할 수 있으므로 메시지만 띄우는 게 안전합니다.
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

        var targetBlock = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("yuushya", "recycle_bin_0"));

        if (level.isClientSide) return;

        if (level.getBlockState(pos).is(targetBlock)) {
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

    private static final int TAUNT_SLOT = 8; // 9번째 슬롯 (인덱스 8)

    // 1. 아이템 버리기(Q키) 방지
    @SubscribeEvent
    public static void onItemDrop(ItemTossEvent event) {
        if (event.getEntity().getItem().is(ITEMS.TAUNT_ITEM.get())) {
            // 로그가 콘솔에 뜨는지 확인해보세요
            // System.out.println("도발 아이템 버리기 감지됨 - 취소");

            event.setCanceled(true); // 이벤트 취소 (버리기 막음)

            // 아이템 엔티티가 이미 생성되었다면 제거 (확실한 처리)
            if (event.getEntity() != null) {
                event.getEntity().discard();
            }

            // 플레이어 인벤토리에 복구 (서버와 클라이언트 동기화 문제 방지)
            if (event.getPlayer() != null) {
                event.getPlayer().getInventory().setItem(TAUNT_SLOT, new ItemStack(ITEMS.TAUNT_ITEM.get()));
                event.getPlayer().displayClientMessage(Component.literal("§c도발 아이템은 버릴 수 없습니다!"), true);
            }
        }
    }

    // 2. 아이템 고정 및 이동 방지 (매 틱 실행)
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();

        // 서버에서만 검사해도 되지만, 화면 떨림 방지를 위해 클라이언트도 검사
        if (player.isSpectator()) return;

        // 술래가 아닌 경우에만 (도망자)
        if (!player.getTags().contains("seeker")) {
            boolean changed = false;

            // [A] 마우스 커서에 들고 있는 경우 -> 즉시 삭제
            if (player.containerMenu.getCarried().is(ITEMS.TAUNT_ITEM.get())) {
                player.containerMenu.setCarried(ItemStack.EMPTY);
                changed = true;
            }

            // [B] 8번 슬롯이 비었거나 다른 아이템인 경우 -> 재생성
            ItemStack stackInSlot = player.getInventory().getItem(TAUNT_SLOT);
            if (!stackInSlot.is(ITEMS.TAUNT_ITEM.get())) {
                player.getInventory().setItem(TAUNT_SLOT, new ItemStack(ITEMS.TAUNT_ITEM.get()));
                changed = true;
            }

            // [C] 8번 슬롯 외의 다른 곳에 있는 경우 -> 삭제
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                if (i != TAUNT_SLOT) {
                    if (player.getInventory().getItem(i).is(ITEMS.TAUNT_ITEM.get())) {
                        player.getInventory().setItem(i, ItemStack.EMPTY);
                        changed = true;
                    }
                }
            }

            // 인벤토리가 변경되었다면 강제 업데이트 (화면 갱신)
            if (changed) {
                player.inventoryMenu.broadcastChanges();
            }
        }
    }
}