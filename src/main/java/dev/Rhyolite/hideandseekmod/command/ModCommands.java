package dev.Rhyolite.hideandseekmod.command;

import com.mojang.brigadier.CommandDispatcher;
import dev.Rhyolite.hideandseekmod.event.GameEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = "hideandseekmod")
public class ModCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        // /resetgame 명령어 등록
        dispatcher.register(Commands.literal("resetgame")
                .requires(source -> source.hasPermission(2))
                .executes(context -> {
                    // 1. 데이터 초기화 실행
                    GameEvents.resetGame();

                    // 2. 명령어 실행자에게 알림
                    context.getSource().sendSuccess(() -> Component.literal("§a§l[!] 모든 보급 블록과 쿨타임이 초기화되었습니다!"), true);
                    return 1;
                })
        );
    }
}