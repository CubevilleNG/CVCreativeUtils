package org.cubeville.cvcreativeutils.commands;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.cubeville.commons.commands.Command;
import org.cubeville.commons.commands.CommandParameterInteger;
import org.cubeville.commons.commands.CommandResponse;
import org.cubeville.cvcreativeutils.PlotManager;

import java.util.*;

public class MobTpCommand extends Command {

    private final PlotManager plotManager;

    public MobTpCommand(PlotManager plotManager) {
        super("");
        setPermission("cvcreativeutils.mobtp");
        addBaseParameter(new CommandParameterInteger());
        this.plotManager = plotManager;
    }

    @Override
    public CommandResponse execute(Player player, Set<String> flags, Map<String, Object> parameters, List<Object> baseParameters) {
        ProtectedRegion region = getRegion(player);
        if(region == null) return new CommandResponse(ChatColor.RED + "You must be standing in your plot to run this command!");
        plotManager.verifyEntities(region, player.getWorld());
        if(baseParameters.size() != 1) {
            return new CommandResponse(ChatColor.RED + "Invalid usage! Use /mobcount <type> and click the entity from there!");
        } else {
            LinkedList<UUID> eList = new LinkedList<>(plotManager.getEntitiesAtRegion(region, player.getWorld()));
            if(eList.size() <= (int) baseParameters.get(0)) return new CommandResponse(ChatColor.RED + "Invalid usage! Use /mobcount <type> and click the entity from there!");
            Entity entity = Bukkit.getEntity(eList.get((int)baseParameters.get(0)));
            if(entity == null) return new CommandResponse(ChatColor.RED + "Entity is unloaded or dead! This shouldn't happen!");
            player.teleport(entity.getLocation());
            return new CommandResponse(ChatColor.GREEN + "Teleport to " + entity.getName());
        }
    }

    public ProtectedRegion getRegion(Player player) {
        List<ProtectedRegion> standingInRegions = plotManager.getRegionsAtLoc(player.getLocation());
        for(ProtectedRegion region : standingInRegions) {
            if(region.getOwners().contains(player.getUniqueId())) return region;
        }
        return null;
    }
}
