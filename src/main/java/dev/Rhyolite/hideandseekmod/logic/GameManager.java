package dev.Rhyolite.hideandseekmod.logic;

import net.minecraft.world.entity.player.Player;
import java.util.*;

public class GameManager {
    private static final GameManager INSTANCE = new GameManager();
    private final Map<UUID, PlayerRole> playerRoles = new HashMap<>();

    public enum PlayerRole { NONE, RUNNER, KILLER }

    public static GameManager get() { return INSTANCE; }

    public void setRole(Player player, PlayerRole role) {
        playerRoles.put(player.getUUID(), role);
    }

    public boolean isKiller(Player player) {
        return playerRoles.getOrDefault(player.getUUID(), PlayerRole.NONE) == PlayerRole.KILLER;
    }

    public boolean isRunner(Player player) {
        return playerRoles.getOrDefault(player.getUUID(), PlayerRole.NONE) == PlayerRole.RUNNER;
    }
}