package dev.Rhyolite.hideandseekmod.block; // 패키지명 확인

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, "hideandseekmod");

    // 외부 모드(yuushya)의 쓰레기통 블록 참조
    private static final Block TRASH_CAN_BLOCK = BuiltInRegistries.BLOCK.get(
            ResourceLocation.fromNamespaceAndPath("yuushya", "recycle_bin_0")
    );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TrashCanBlockEntity>> TRASH_CAN =
            BLOCK_ENTITIES.register("trash_can_be", () ->
                    BlockEntityType.Builder.of(TrashCanBlockEntity::new, TRASH_CAN_BLOCK).build(null)
            );
}