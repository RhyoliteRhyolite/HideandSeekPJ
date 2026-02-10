package dev.Rhyolite.hideandseekmod.item; // 본인의 패키지명에 맞게 수정

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.TickTask;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.server.level.ServerLevel;

public class TrapBlock extends Block {
    // 속성 수정을 위한 고유 ID
    private static final ResourceLocation TRAP_SPEED_MOD = ResourceLocation.fromNamespaceAndPath("hideandseek", "trap_speed");
    private static final ResourceLocation TRAP_JUMP_MOD = ResourceLocation.fromNamespaceAndPath("hideandseek", "trap_jump");

    public TrapBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Block.box(1.0D, 0.0D, 1.0D, 15.0D, 1.0D, 15.0D);
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level instanceof ServerLevel serverLevel && entity instanceof Player player) {

            // [조건 1 & 2] 도망자만 밟을 수 있음 (술래는 태그 'seeker'를 가졌다고 가정)
            // 술래가 아니거나, 특정 태그가 없는 유저만 덫에 걸림
            if (!player.getTags().contains("seeker")) {
                applyTrapEffects(player);

                // 덫 발동음 및 블록 제거
                level.playSound(null, pos, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 1.5F, 0.8F);
                level.destroyBlock(pos, false);
            }
        }
    }

    private void applyTrapEffects(Player player) {
        var speedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        var jumpAttr = player.getAttribute(Attributes.JUMP_STRENGTH);

        if (speedAttr != null && jumpAttr != null) {
            // 속도 -95%, 점프 -100% (완전 포획)
            speedAttr.addTransientModifier(new AttributeModifier(TRAP_SPEED_MOD, -0.95, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            jumpAttr.addTransientModifier(new AttributeModifier(TRAP_JUMP_MOD, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

            player.displayClientMessage(Component.literal("§c§l덫에 걸렸습니다! 웅크려서 탈출하세요!"), true);
            player.addTag("is_trapped"); // 웅크리기 감지용 태그
        }
    }

    // [조건 3] 웅크리고 있으면 덫 제거 (PlayerTickEvent에서 처리하는 것이 가장 정확함)
    // 아래는 별도의 핸들러 클래스에 작성하거나, TrapBlock 내부에 이벤트 리스너로 등록할 수 있는 로직입니다.
}