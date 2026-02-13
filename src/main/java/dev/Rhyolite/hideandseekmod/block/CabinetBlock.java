package dev.Rhyolite.hideandseekmod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import com.mojang.serialization.MapCodec;

import java.util.List;

public class CabinetBlock extends HorizontalDirectionalBlock {
    // [추가 1] 코덱 정의 (필수)
    public static final MapCodec<CabinetBlock> CODEC = simpleCodec(CabinetBlock::new);

    public CabinetBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    // [추가 2] 추상 메서드 구현 (필수)
    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    // 캐비닛은 2칸 높이 (0~32)
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 32, 16);

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    // 블록 설치 시 플레이어가 보는 방향의 반대(나를 보게)로 설치
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    // ★ 핵심: 우클릭 시 실행되는 로직
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide) {

            // 1. 이미 누가 숨어있는지 확인
            List<ArmorStand> seats = level.getEntitiesOfClass(ArmorStand.class, state.getShape(level, pos).bounds().move(pos),
                    entity -> entity.getTags().contains("cabinet_seat"));

            long lastExit = player.getPersistentData().getLong("last_cabinet_exit");
            long currentTime = level.getGameTime();

            if (currentTime - lastExit < 80) { // 80틱 = 4초
                long remain = (80 - (currentTime - lastExit)) / 20;
                player.displayClientMessage(Component.literal("§c재입장 쿨타임: " + (remain + 1) + "초"), true);
                return InteractionResult.FAIL;
            }

            if (!seats.isEmpty()) {
                // 이미 누군가 있음
                ArmorStand seat = seats.get(0);
                if (seat.getPassengers().isEmpty()) {
                    seat.discard(); // 비어있으면 삭제 (오류 방지)
                } else {
                    // 술래가 열었을 경우 숨은 사람 쫓아내기!
                    if (player.getTags().contains("seeker")) {
                        seat.ejectPassengers(); // 강제 하차
                        seat.discard();
                        level.playSound(null, pos, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 1.0F, 1.0F);
                        player.displayClientMessage(Component.literal("§c숨어있던 쥐새끼를 찾았습니다!"), true);
                        return InteractionResult.SUCCESS;
                    } else {
                        player.displayClientMessage(Component.literal("§c이 캐비닛은 이미 꽉 찼습니다."), true);
                        return InteractionResult.FAIL;
                    }
                }
            }

            // 2. 술래는 들어갈 수 없음
            if (player.getTags().contains("seeker")) {
                player.displayClientMessage(Component.literal("§c술래는 캐비닛에 숨을 수 없습니다."), true);
                return InteractionResult.FAIL;
            }

            if (!level.isClientSide) {
                // Y좌표를 -0.6으로 설정 (너무 낮으면 땅에 파묻힘)
                double spawnY = pos.getY() + 0.4;
                ArmorStand seat = new ArmorStand(level, pos.getX() + 0.5, spawnY, pos.getZ() + 0.5);

                seat.setInvisible(true);
                seat.setNoGravity(true);
                seat.setInvulnerable(true);

                // EntityData를 사용하여 Marker(0x10)와 Small(0x01) 강제 설정
                byte flags = seat.getEntityData().get(ArmorStand.DATA_CLIENT_FLAGS);
                seat.getEntityData().set(ArmorStand.DATA_CLIENT_FLAGS, (byte)(flags | 0x01 | 0x10));

                seat.addTag("cabinet_seat");

                // 위치 확정
                seat.moveTo(pos.getX() + 0.5, spawnY, pos.getZ() + 0.5, 0, 0);

                level.addFreshEntity(seat);
                player.startRiding(seat);

                // 산소 데이터 초기화 (300틱 = 15초)
                player.getPersistentData().putInt("cabinet_oxygen", 300);
            }

            level.playSound(null, pos, SoundEvents.BARREL_OPEN, SoundSource.BLOCKS, 1.0F, 1.0F);
            player.displayClientMessage(Component.literal("§a캐비닛에 숨었습니다. (Shift: 나가기)"), true);
        }

        return InteractionResult.SUCCESS;
    }

    // 블록이 파괴되면 숨어있던 사람도 나오게 처리
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            List<ArmorStand> seats = level.getEntitiesOfClass(ArmorStand.class, state.getShape(level, pos).bounds().move(pos),
                    entity -> entity.getTags().contains("cabinet_seat"));
            for (ArmorStand seat : seats) {
                seat.ejectPassengers();
                seat.discard();
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}