package dev.Rhyolite.hideandseekmod.event;

import dev.Rhyolite.hideandseekmod.HideandSeekMod;
import dev.Rhyolite.hideandseekmod.logic.GameManager;
import dev.Rhyolite.hideandseekmod.network.JumpscarePayload;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.network.PacketDistributor;

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
}