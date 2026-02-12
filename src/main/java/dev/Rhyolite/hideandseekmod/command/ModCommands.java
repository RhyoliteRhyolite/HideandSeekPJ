package dev.Rhyolite.hideandseekmod.command;

import com.mojang.brigadier.CommandDispatcher;
import dev.Rhyolite.hideandseekmod.event.GameEvents;
import dev.Rhyolite.hideandseekmod.logic.GameManager;
import dev.Rhyolite.hideandseekmod.network.JumpscarePayload;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

public class ModCommands {

    // [수정] 이 메서드는 더 이상 이벤트를 직접 구독하지 않고,
    // 메인 클래스나 이벤트 핸들러에서 전달해주는 '알맹이(dispatcher)'만 받습니다.
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        // /resetgame 명령어 등록
        dispatcher.register(Commands.literal("resetgame")
                .requires(source -> source.hasPermission(2))
                .executes(context -> {
                    // resetGame에 level 인자가 필요하다면 context.getSource().getLevel()을 넣으세요.
                    GameEvents.resetGame(context.getSource().getLevel());
                    return 1;
                })
        );

        // /startgame 명령어 등록
        dispatcher.register(Commands.literal("startgame")
                .requires(source -> source.hasPermission(2))
                .executes(context -> {
                    GameManager.startGame(context.getSource().getLevel());
                    return 1;
                })
        );

        // /stopgame 명령어 등록
        dispatcher.register(Commands.literal("stopgame")
                .requires(source -> source.hasPermission(2))
                .executes(context -> {
                    ServerLevel level = context.getSource().getLevel();
                    GameManager.stopGame(level, "§c[!] 운영자에 의해 게임이 강제 종료되었습니다.");
                    context.getSource().sendSuccess(() -> Component.literal("§a게임을 강제로 중단했습니다."), true);
                    return 1;
                })
        );

        // /jumpscare_test 명령어 등록
        dispatcher.register(Commands.literal("jumpscare_test")
                .executes(context -> {
                    Player player = context.getSource().getPlayerOrException();
                    PacketDistributor.sendToPlayer((net.minecraft.server.level.ServerPlayer) player, new JumpscarePayload("jumpscare.png"));
                    context.getSource().sendSuccess(() -> Component.literal("점프스캐어 테스트!"), false);
                    return 1;
                })
        );
    }
}