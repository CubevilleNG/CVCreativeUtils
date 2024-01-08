package org.cubeville.cvcreativeutils.commands;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.cubeville.commons.commands.Command;
import org.cubeville.commons.commands.CommandParameterString;
import org.cubeville.commons.commands.CommandResponse;
import org.cubeville.cvcreativeutils.PlotManager;

import java.util.*;

public class MobKillCommand extends Command {

    private final PlotManager plotManager;

    public MobKillCommand(PlotManager plotManager) {
        super("");
        addOptionalBaseParameter(new CommandParameterString());
        setPermission("cvcreativeutils.mobkill");
        this.plotManager = plotManager;
    }

    @Override
    public CommandResponse execute(Player player, Set<String> flags, Map<String, Object> parameters, List<Object> baseParameters) {
        if(baseParameters.size() != 1) return new CommandResponse(ChatColor.RED + "Invalid usage! Use /mobkill [all or entityType] - Example: /mobkill chicken");
        ProtectedRegion region = Util.getRegion(player, plotManager.getRegionsAtLoc(player.getLocation()));
        if(region == null) return new CommandResponse(ChatColor.RED + "You must be standing in your plot to run this command!");
        plotManager.verifyEntities(region, player.getWorld());

        int killed;
        if(baseParameters.get(0).equals("all")) {
            killed = killEntities(region, player.getWorld(), null, true);
        } else {
            EntityType type;
            try {
                type = EntityType.valueOf(((String)baseParameters.get(0)).toUpperCase());
            } catch(IllegalArgumentException ignored) {
                return new CommandResponse(ChatColor.RED + "Invalid usage! Use /mobkill [all or entityType] - Example: /mobkill chicken");
            }
            killed = killEntities(region, player.getWorld(), type, false);
        }
        player.sendMessage(Component.text(ChatColor.RED + String.valueOf(killed) + " mobs killed. " + ChatColor.AQUA + "New Mob Count for plot: " +
                        ChatColor.GREEN + plotManager.countEntities(region, player.getWorld()) + ChatColor.WHITE + "/" + ChatColor.GREEN + plotManager.maxEntitiesPerPlot)
                .hoverEvent(HoverEvent.hoverEvent(HoverEvent.Action.SHOW_TEXT, Component.text("Show detailed view of entities")))
                .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.RUN_COMMAND, "/mobcount detailed")));
        return new CommandResponse("");
    }

    public int killEntities(ProtectedRegion region, World world, EntityType type, boolean killAll) {
        int i = 0;
        LinkedHashSet<UUID> uuids = new LinkedHashSet<>(plotManager.getEntitiesAtRegion(region, world));
        for(UUID uuid : uuids) {
            Entity entity = Bukkit.getEntity(uuid);
            if(entity instanceof LivingEntity && !entity.isDead() && (killAll || entity.getType().equals(type))) {
                ((LivingEntity)entity).setHealth(0);
                i++;
            }
        }
        return i;
    }
}
