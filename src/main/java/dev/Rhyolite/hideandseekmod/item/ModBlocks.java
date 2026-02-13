package dev.Rhyolite.hideandseekmod.item;

import dev.Rhyolite.hideandseekmod.HideandSeekMod;
import dev.Rhyolite.hideandseekmod.block.CabinetBlock;
import dev.Rhyolite.hideandseekmod.block.TrapBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, HideandSeekMod.MODID);

    // 덫 블록이 여기에 있어야 합니다.
    public static final DeferredHolder<Block, Block> TRAP_BLOCK = BLOCKS.register("trap",
            () -> new TrapBlock(BlockBehaviour.Properties.of().noCollission().instabreak().noOcclusion()));


    //캐비넷
    public static final DeferredHolder<Block, Block> CABINET = BLOCKS.register("cabinet",
            () -> new CabinetBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f, 3.0f) // 경도, 저항
                    .sound(SoundType.WOOD) // 소리
                    .noOcclusion() // 투명한 부분 허용 (모델링 위해)
            ));
}
