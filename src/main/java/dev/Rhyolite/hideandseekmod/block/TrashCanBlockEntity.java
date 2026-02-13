package dev.Rhyolite.hideandseekmod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

// TrashCanBlockEntity.java (쓰레기통의 엔티티 클래스)
public class TrashCanBlockEntity extends BlockEntity {

    // 함정 설치 여부 변수
    private boolean isTrapped = false;

    public TrashCanBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TRASH_CAN.get(), pos, state);
    }

    // 함정 설정/해제 메서드
    public void setTrapped(boolean trapped) {
        this.isTrapped = trapped;
        setChanged(); // 데이터 변경 알림 (저장됨)
    }

    public boolean isTrapped() {
        return this.isTrapped;
    }

}