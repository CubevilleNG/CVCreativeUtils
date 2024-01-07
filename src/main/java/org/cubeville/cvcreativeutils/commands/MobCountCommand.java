package org.cubeville.cvcreativeutils.commands;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.cubeville.commons.commands.Command;
import org.cubeville.commons.commands.CommandParameterString;
import org.cubeville.commons.commands.CommandResponse;
import org.cubeville.cvcreativeutils.PlotManager;

import java.util.*;

public class MobCountCommand extends Command {

    private final PlotManager plotManager;

    public MobCountCommand(PlotManager plotManager) {
        super("");
        addOptionalBaseParameter(new CommandParameterString());
        setPermission("cvcreativeutils.mobcount");
        this.plotManager = plotManager;
    }

    @Override
    public CommandResponse execute(Player player, Set<String> flags, Map<String, Object> parameters, List<Object> baseParameters) {
        boolean detailed;
        LinkedHashSet<TextComponent> out = null;
        ProtectedRegion region = getRegion(player);
        if(region == null) return new CommandResponse(ChatColor.RED + "You must be standing in your plot to run this command!");
        plotManager.verifyEntities(region, player.getWorld());
        if(baseParameters.isEmpty()) {
            detailed = false;
        } else if(baseParameters.size() == 1) {
            if(((String)baseParameters.get(0)).equalsIgnoreCase("detailed")) {
                detailed = true;
            } else {
                try {
                    out = getFormattedEntityList(region, player.getWorld(), false, EntityType.valueOf(((String)baseParameters.get(0)).toUpperCase()));
                    detailed = false;
                } catch(IllegalArgumentException ignored) {
                    return new CommandResponse(ChatColor.RED + "Invalid usage! Use /mobcount [detailed]");
                }
            }
        } else {
            return new CommandResponse(ChatColor.RED + "Invalid usage! Use /mobcount [detailed]");
        }
        if(out == null) out = getFormattedEntityList(region, player.getWorld(), detailed, null);
        for(TextComponent t : out) {
            player.sendMessage(t);
        }
        return new CommandResponse("");
    }

    public ProtectedRegion getRegion(Player player) {
        List<ProtectedRegion> standingInRegions = plotManager.getRegionsAtLoc(player.getLocation());
        for(ProtectedRegion region : standingInRegions) {
            if(region.getOwners().contains(player.getUniqueId())) return region;
        }
        return null;
    }

    public LinkedHashSet<TextComponent> getFormattedEntityList(ProtectedRegion region, World world, boolean detailed, EntityType type) {
        LinkedHashSet<TextComponent> out = new LinkedHashSet<>();
        LinkedHashSet<UUID> entities = plotManager.getEntitiesAtRegion(region, world);
        if(type == null) {
            out.add(Component.text(ChatColor.AQUA + "Mob Count for plot: " +
                    ChatColor.GREEN + entities.size() + ChatColor.WHITE + "/" + ChatColor.GREEN + plotManager.maxEntitiesPerPlot)
                    .hoverEvent(HoverEvent.hoverEvent(HoverEvent.Action.SHOW_TEXT, Component.text("Show detailed view of entities")))
                    .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.RUN_COMMAND, "/mobcount detailed")));
        } else {
            int count = 0;
            for(UUID uuid : entities) {
                Entity entity = Bukkit.getEntity(uuid);
                if(entity == null) continue;
                if(!entity.getType().equals(type)) continue;
                count++;
            }
            String eType = type.toString().toLowerCase();
            String firstLetter = eType.substring(0, 1).toUpperCase();
            String finalType = firstLetter.concat(eType.substring(1));
            out.add(Component.text(ChatColor.GOLD + finalType + "'s in plot: " +
                    ChatColor.GREEN + count + ChatColor.WHITE + "/" + ChatColor.GREEN + plotManager.maxEntitiesPerPlot));
        }
        if(detailed) {
            Map<EntityType, Integer> types = new HashMap<>();
            for(UUID uuid : entities) {
                Entity entity = Bukkit.getEntity(uuid);
                if(entity == null) continue;
                if(types.get(entity.getType()) != null) {
                    int i = types.get(entity.getType());
                    i++;
                    types.put(entity.getType(), i);
                } else {
                    types.put(entity.getType(), 1);
                }
            }
            for(EntityType e : types.keySet()) {
                TextComponent o = Component.text()
                        .append(Component.text(ChatColor.WHITE + " - "))
                        .append(Component.text(ChatColor.DARK_AQUA + e.toString().toLowerCase()))
                        .append(Component.text(ChatColor.WHITE + " (" + ChatColor.GREEN + types.get(e).toString() + ChatColor.WHITE + ")"))
                        .hoverEvent(HoverEvent.hoverEvent(HoverEvent.Action.SHOW_TEXT, Component.text("Show details of " + e.toString().toLowerCase())))
                        .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.RUN_COMMAND, "/mobcount " + e.toString().toLowerCase()))
                        .build();
                out.add(o);
            }
        } else if(type != null) {
            int index = -1;
            for(UUID uuid : entities) {
                index++;
                Entity entity = Bukkit.getEntity(uuid);
                if(entity == null) continue;
                if(!entity.getType().equals(type)) continue;
                Location loc = entity.getLocation();
                TextComponent o = Component.text()
                        .append(Component.text(ChatColor.WHITE + " - "))
                        .append(Component.text(ChatColor.YELLOW + type.toString().toLowerCase()))
                        .append(Component.text(ChatColor.WHITE + " ("))
                        .append(entity.customName() == null ? Component.text(entity.getName()) : entity.customName())
                        .append(Component.text(ChatColor.WHITE + ") "))
                        .append(Component.text(ChatColor.YELLOW + String.valueOf(loc.getBlockX()) + ChatColor.WHITE + ", "))
                        .append(Component.text(ChatColor.YELLOW + String.valueOf(loc.getBlockY()) + ChatColor.WHITE + ", "))
                        .append(Component.text(ChatColor.YELLOW + String.valueOf(loc.getBlockZ())))
                        .hoverEvent(HoverEvent.hoverEvent(HoverEvent.Action.SHOW_TEXT, Component.text("Teleport to " + entity.getName())))
                        .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.RUN_COMMAND, "/mobtp " + index))
                        .build();
                out.add(o);
            }
        }
        return out;
    }
}
