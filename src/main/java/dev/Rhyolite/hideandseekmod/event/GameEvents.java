package dev.Rhyolite.hideandseekmod.event;

import dev.Rhyolite.hideandseekmod.block.TrashCanBlockEntity;
import dev.Rhyolite.hideandseekmod.item.ITEMS;
import dev.Rhyolite.hideandseekmod.logic.GameManager;
import dev.Rhyolite.hideandseekmod.logic.ItemPicker;
import dev.Rhyolite.hideandseekmod.network.JumpscarePayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;

@EventBusSubscriber(modid = "hideandseekmod")
public class GameEvents {

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!GameManager.isGameActive) return;

        if (event.getEntity() instanceof ServerPlayer victim && !victim.getTags().contains("seeker")) {
            if (event.getSource().getEntity() instanceof ServerPlayer attacker && attacker.getTags().contains("seeker")) {

                // 1. 점프스케어 패킷 전송
                PacketDistributor.sendToPlayer(victim, new JumpscarePayload("jumpscare"));

                // 2. 즉시 관전 모드 전환 (남은 인원 체크를 위해 중요)
                victim.setGameMode(GameType.SPECTATOR);

                attacker.sendSystemMessage(Component.literal("§c[!] 도망자 " + victim.getScoreboardName() + "님을 잡았습니다!"));

                // 3. [추가] 남은 도망자 확인
                ServerLevel level = (ServerLevel) victim.level();
                int remaining = GameManager.getRemainingRunners(level);

                if (remaining > 0) {
                    level.getServer().getPlayerList().broadcastSystemMessage(
                            Component.literal("§e[!] 남은 도망자 수: §l" + remaining + "명"), false);
                } else {
                    // 도망자가 0명이면 술래 승리!
                    GameManager.seekerWin(level);
                }
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

    // 외부 모드(yuushya)의 쓰레기통 블록 참조
    public static final Block TRASH_CAN_BLOCK = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("yuushya", "recycle_bin_0"));

    // [1] 관전자 차단 및 함정 설치
    @SubscribeEvent
    public static void onSpectatorInteract(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide) return;

        ServerPlayer player = (ServerPlayer) event.getEntity();
        BlockState state = event.getLevel().getBlockState(event.getPos());

        // 타겟 블록인지 확인
        if (state.is(TRASH_CAN_BLOCK)) {

            // A. 관전자인 경우
            if (player.isSpectator()) {
                // 함정 아이템을 들고 있다면 설치 시도
                if (player.getMainHandItem().is(ITEMS.SPECTATOR_TRAP.get())) {
                    BlockEntity be = event.getLevel().getBlockEntity(event.getPos());

                    if (be instanceof TrashCanBlockEntity trashCan) {
                        if (trashCan.isTrapped()) {
                            player.displayClientMessage(Component.literal("§c이미 함정이 설치된 쓰레기통입니다."), true);
                        } else {
                            trashCan.setTrapped(true);
                            player.level().playSound(null, event.getPos(), SoundEvents.SCULK_BLOCK_CHARGE, SoundSource.PLAYERS, 1.0F, 1.0F);
                            player.displayClientMessage(Component.literal("§5[!] 쓰레기통에 점프스케어 함정을 설치했습니다!"), true);
                        }
                    }
                } else {
                    // 함정 아이템이 아닌 일반 클릭은 아예 막음 (아이템 획득 방지)
                    player.displayClientMessage(Component.literal("§c관전자는 쓰레기을 이용할 수 없습니다."), true);
                }

                // 관전자의 모든 상호작용은 여기서 종료 (설치든 털기 시도든 취소)
                event.setCancellationResult(InteractionResult.SUCCESS);
                event.setCanceled(true);
                return;
            }

            // B. 도망자인 경우 (함정 발동)
            if (!player.getTags().contains("seeker")) {
                BlockEntity be = event.getLevel().getBlockEntity(event.getPos());

                if (be instanceof TrashCanBlockEntity trashCan && trashCan.isTrapped()) {
                    // 1. 점프스케어 발동
                    PacketDistributor.sendToPlayer(player, new JumpscarePayload("jumpscare"));

                    // 2. 비명 소리
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.GHAST_SCREAM, SoundSource.PLAYERS, 2.0F, 0.5F);

                    player.sendSystemMessage(Component.literal("§4[!] 함정에 걸렸습니다!"));

                    // 3. 함정 해제
                    trashCan.setTrapped(false);

                    // 4. 쓰레기통 열기 취소
                    event.setCancellationResult(InteractionResult.SUCCESS);
                    event.setCanceled(true);
                }
            }
        }
    }

    private static final int TAUNT_SLOT = 8; // 9번째 슬롯 (인덱스 8)

    // 1. 아이템 버리기(Q키) 방지
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return; // 서버 측에서만 처리

        // [1] 관전자는 모든 로직 건너뛰기
        if (player.isSpectator()) return;

        // [2] 게임 중이 아니거나 플레이어가 죽어가는 상태라면 모든 아이템 삭제
        // (사망 시 아이템 증발을 틱 단위로 보장하고 싶을 때)
        if (!GameManager.isGameActive || !player.isAlive()) {
            if (!player.getInventory().isEmpty()) {
                player.getInventory().clearContent();
                player.inventoryMenu.broadcastChanges();
            }
            return;
        }

        // [3] 술래가 아닌 도망자인 경우의 특수 아이템(도발템) 관리 로직
        if (!player.getTags().contains("seeker")) {
            boolean changed = false;

            // [A] 마우스 커서에 들고 있는 경우 -> 즉시 삭제
            if (player.containerMenu.getCarried().is(ITEMS.TAUNT_ITEM.get())) {
                player.containerMenu.setCarried(ItemStack.EMPTY);
                changed = true;
            }

            // [B] 8번 슬롯(TAUNT_SLOT) 강제 고정
            ItemStack stackInSlot = player.getInventory().getItem(TAUNT_SLOT);
            if (!stackInSlot.is(ITEMS.TAUNT_ITEM.get())) {
                player.getInventory().setItem(TAUNT_SLOT, new ItemStack(ITEMS.TAUNT_ITEM.get()));
                changed = true;
            }

            // [C] 8번 슬롯 외의 다른 곳에 도발 아이템이 있다면 삭제
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                if (i != TAUNT_SLOT) {
                    if (player.getInventory().getItem(i).is(ITEMS.TAUNT_ITEM.get())) {
                        player.getInventory().setItem(i, ItemStack.EMPTY);
                        changed = true;
                    }
                }
            }

            // 인벤토리 변경 사항 반영
            if (changed) {
                player.inventoryMenu.broadcastChanges();
            }
        } else {
            // 술래인 경우: 도발 아이템을 가지고 있으면 안 됨
            if (player.getInventory().contains(new ItemStack(ITEMS.TAUNT_ITEM.get()))) {
                player.getInventory().clearOrCountMatchingItems(stack -> stack.is(ITEMS.TAUNT_ITEM.get()), -1, player.inventoryMenu.getCraftSlots());
                player.inventoryMenu.broadcastChanges();
            }
        }
    }
}