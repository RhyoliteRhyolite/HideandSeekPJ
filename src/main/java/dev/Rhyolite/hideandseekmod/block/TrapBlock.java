package dev.Rhyolite.hideandseekmod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class TrapBlock extends Block {
    // 덫 효과를 위한 고유 ID
    public static final ResourceLocation TRAP_SPEED_MOD = ResourceLocation.fromNamespaceAndPath("hideandseek", "trap_speed");
    public static final ResourceLocation TRAP_JUMP_MOD = ResourceLocation.fromNamespaceAndPath("hideandseek", "trap_jump");

    // 덫 모양 (납작하게)
    private static final VoxelShape SHAPE = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 1.0D, 15.0D);

    public TrapBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!level.isClientSide && entity instanceof Player player) {

            // [조건] 이미 덫에 걸렸거나, 'seeker'(술래) 태그가 있는 사람은 무시
            if (player.getTags().contains("is_trapped") || player.getTags().contains("seeker")) {
                return;
            }

            // [발동] 도망자가 밟음
            trapPlayer(player, level, pos);
        }
    }

    private void trapPlayer(Player player, Level level, BlockPos pos) {
        var speedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        var jumpAttr = player.getAttribute(Attributes.JUMP_STRENGTH);

        if (speedAttr != null && jumpAttr != null) {
            // 1. 기존 효과 제거 (중복 방지)
            speedAttr.removeModifier(TRAP_SPEED_MOD);
            jumpAttr.removeModifier(TRAP_JUMP_MOD);

            // 2. 이동 불가 (-100%), 점프 불가 (-100%) 적용
            speedAttr.addTransientModifier(new AttributeModifier(TRAP_SPEED_MOD, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            jumpAttr.addTransientModifier(new AttributeModifier(TRAP_JUMP_MOD, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

            // 3. 상태 태그 부여 및 알림
            player.addTag("is_trapped");
            player.displayClientMessage(Component.literal("§c§l덫에 걸렸습니다! §e1초 동안 웅크려서 해체하세요!"), true);

            // 4. 소리 재생 (철컥!)
            level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 1.5F, 0.8F);
        }

    }
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (!level.isClientSide) {
            level.playSound(null, pos, SoundEvents.METAL_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }
}