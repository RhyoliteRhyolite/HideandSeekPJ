package dev.Rhyolite.hideandseekmod.item;

import dev.Rhyolite.hideandseekmod.block.TrashCanBlockEntity; // 쓰레기통 블록
import dev.Rhyolite.hideandseekmod.event.GameEvents;
import dev.Rhyolite.hideandseekmod.logic.GameManager;
import dev.Rhyolite.hideandseekmod.network.JumpscarePayload; // 점프스케어 패킷
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = "hideandseekmod")
public class TrashCanBlockEventHandler {

    // [1] 관전자가 함정 설치 (우클릭)
    @SubscribeEvent
    public static void onSpectatorPlaceTrap(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide) return;

        ServerPlayer player = (ServerPlayer) event.getEntity();

        // 관전자이고 + 함정 아이템을 들고 있고 + 대상이 쓰레기통 블록일 때
        if (player.isSpectator()
                && player.getMainHandItem().is(ITEMS.SPECTATOR_TRAP.get())
                && event.getLevel().getBlockState(event.getPos()).is(GameEvents.TRASH_CAN_BLOCK)) {

            BlockEntity be = event.getLevel().getBlockEntity(event.getPos());

            if (be instanceof TrashCanBlockEntity trashCan) {
                // 이미 함정이 있는지 확인
                if (trashCan.isTrapped()) {
                    player.displayClientMessage(Component.literal("§c이미 함정이 설치된 쓰레기통입니다."), true);
                } else {
                    // 함정 설치!
                    trashCan.setTrapped(true);

                    // 소리와 메시지 (관전자에게만)
                    player.level().playSound(null, event.getPos(), SoundEvents.SCULK_BLOCK_CHARGE, SoundSource.PLAYERS, 1.0F, 1.0F);
                    player.displayClientMessage(Component.literal("§5[!] 보급소에 점프스케어 함정을 설치했습니다!"), true);

                    // 성공 처리 (관전자라도 이벤트가 취소되지 않게 SUCCESS 반환)
                    event.setCancellationResult(InteractionResult.SUCCESS);
                    event.setCanceled(true);
                }
            }
        }
    }

    // [2] 도망자가 함정 밟음 (우클릭)
    @SubscribeEvent
    public static void onRunnerTriggerTrap(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide) return;

        ServerPlayer player = (ServerPlayer) event.getEntity();

        // 도망자(술래 아님, 관전자 아님)가 쓰레기통을 열려고 할 때
        if (!player.isSpectator() && !player.getTags().contains("seeker")
                && event.getLevel().getBlockState(event.getPos()).is(GameEvents.TRASH_CAN_BLOCK)) {

            BlockEntity be = event.getLevel().getBlockEntity(event.getPos());

            if (be instanceof TrashCanBlockEntity trashCan) {
                // 함정이 설치되어 있는가?
                if (trashCan.isTrapped()) {

                    // 1. 점프스케어 발동
                    PacketDistributor.sendToPlayer(player, new JumpscarePayload("jumpscare"));

                    // 2. 끔찍한 비명 소리 (주변 사람도 들음)
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.GHAST_SCREAM, SoundSource.PLAYERS, 2.0F, 0.5F);

                    player.sendSystemMessage(Component.literal("§4[!] 함정에 걸렸습니다!"));

                    // 3. 함정 해제 (일회용)
                    trashCan.setTrapped(false);

                    // 4. [중요] 쓰레기통이 열리지 않게 이벤트 취소 (깜짝 놀라게만 하고 못 열게 함)
                    // 만약 열리게 하고 싶으면 이 줄을 지우세요.
                    event.setCanceled(true);
                }
            }
        }
    }
}