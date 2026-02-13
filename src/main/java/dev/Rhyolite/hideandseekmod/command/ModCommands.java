package dev.Rhyolite.hideandseekmod.command;

import com.mojang.brigadier.CommandDispatcher;
import dev.Rhyolite.hideandseekmod.logic.GameManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class ModCommands { // @EventBusSubscriber 삭제!
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("gamestart")
                .requires(s -> s.hasPermission(2))
                .executes(context -> {
                    GameManager.startGame(context.getSource().getLevel());
                    return 1;
                })
        );

        dispatcher.register(Commands.literal("gamestop")
                .requires(s -> s.hasPermission(2))
                .executes(context -> {
                    GameManager.stopGame(context.getSource().getLevel(), "§c게임 종료!");
                    return 1;
                })
        );
    }
}