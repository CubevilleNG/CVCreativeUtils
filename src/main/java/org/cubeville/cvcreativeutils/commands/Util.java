package org.cubeville.cvcreativeutils.commands;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.entity.Player;

import java.util.List;

public class Util {

    public static ProtectedRegion getRegion(Player player, List<ProtectedRegion> standingInRegions) {
        for(ProtectedRegion region : standingInRegions) {
            if(region.getOwners().contains(player.getUniqueId()) || player.hasPermission("cvcreativeutils.mobcommandsoverride")) return region;
        }
        return null;
    }
}
