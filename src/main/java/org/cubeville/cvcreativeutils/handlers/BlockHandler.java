package org.cubeville.cvcreativeutils.handlers;

import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;
import org.cubeville.cvcreativeutils.PlotManager;

public class BlockHandler implements Listener {

    private final PlotManager plotManager;

    public BlockHandler(PlotManager plotManager) {
        this.plotManager = plotManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLiquidFlow(BlockFromToEvent event) {
        if(event.isCancelled()) return;
        if(!plotManager.isWorldControlled(event.getBlock().getWorld())) return;
        Location fromLoc = event.getBlock().getLocation();
        Location toLoc = event.getToBlock().getLocation();
        if(!plotManager.getRegionsAtLoc(fromLoc).equals(plotManager.getRegionsAtLoc(toLoc))) {
            event.setCancelled(true);
        }
    }
}
