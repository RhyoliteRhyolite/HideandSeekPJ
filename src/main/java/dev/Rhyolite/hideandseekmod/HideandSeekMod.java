package dev.Rhyolite.hideandseekmod;

import com.mojang.logging.LogUtils;
import dev.Rhyolite.hideandseekmod.item.TrapEventHandler;
import dev.Rhyolite.hideandseekmod.network.JumpscarePayload;
import dev.Rhyolite.hideandseekmod.network.PacketHandler;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

@Mod(HideandSeekMod.MODID)
public class HideandSeekMod {
    public static final String MODID = "hideandseekmod";
    public static final Logger LOGGER = LogUtils.getLogger();

    // 탭 등록기
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    // 탭 정의
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> HIDE_AND_SEEK_TAB = CREATIVE_MODE_TABS.register("hideandseek_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.hideandseekmod"))
                    .icon(() -> new ItemStack(dev.Rhyolite.hideandseekmod.ITEMS.ENERGY_DRINK.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(dev.Rhyolite.hideandseekmod.ITEMS.ENERGY_DRINK.get());
                        output.accept(dev.Rhyolite.hideandseekmod.ITEMS.CHALK_ERASER.get());
                        output.accept(dev.Rhyolite.hideandseekmod.ITEMS.TRAP.get());
                    })
                    .build()
    );

    public HideandSeekMod(IEventBus modEventBus, ModContainer modContainer) {
        // ModItems에 있는 등록기를 가져와서 등록합니다.
        dev.Rhyolite.hideandseekmod.ITEMS.ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::addCreative);
        modEventBus.addListener(PacketHandler::register);

        NeoForge.EVENT_BUS.register(this);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        NeoForge.EVENT_BUS.register(TrapEventHandler.class);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("HIDE AND SEEK MOD SETUP START");
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        // ITEMS를 앞에 붙여서 어디 있는 아이템인지 명시합니다.
        if (event.getTabKey() == CreativeModeTabs.FOOD_AND_DRINKS) {
            event.accept(dev.Rhyolite.hideandseekmod.ITEMS.ENERGY_DRINK.get());
            event.accept(dev.Rhyolite.hideandseekmod.ITEMS.CHALK_ERASER.get());
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(net.neoforged.neoforge.event.RegisterCommandsEvent event) {
        event.getDispatcher().register(
                net.minecraft.commands.Commands.literal("jumpscare_test")
                        .executes(context -> {
                            Player player = context.getSource().getPlayerOrException();
                            PacketDistributor.sendToPlayer((net.minecraft.server.level.ServerPlayer) player, new JumpscarePayload("jumpscare.png"));
                            context.getSource().sendSuccess(() -> Component.literal("점프스캐어 테스트!"), false);
                            return 1;
                        })
        );
    }
}