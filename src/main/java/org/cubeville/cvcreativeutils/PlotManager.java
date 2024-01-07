package org.cubeville.cvcreativeutils;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.util.Vector;

import java.util.*;

public class PlotManager {

    public int maxEntitiesPerPlot;
    public List<World> controlledWorlds;
    private Map<World, Map<ProtectedRegion, LinkedHashSet<UUID>>> plots;
    private Map<World, Map<ProtectedRegion, Long>> regionRealizedTime;

    public PlotManager(int entitiesPerPlot, List<World> controlledWorlds) {
        this.maxEntitiesPerPlot = entitiesPerPlot;
        this.controlledWorlds = controlledWorlds;
        this.plots = new HashMap<>();
        this.regionRealizedTime = new HashMap<>();
    }

    public boolean isWorldControlled(World world) {
        return this.controlledWorlds.contains(world);
    }

    public void setMaxEntitiesPerPlot(int i) {
        maxEntitiesPerPlot = i;
    }

    public int countEntities(ProtectedRegion region, World world) {
        if(this.plots.containsKey(world) && this.plots.get(world).containsKey(region)) return this.plots.get(world).get(region).size();
        return 0;
    }

    public boolean maxEntitiesSpawned(ProtectedRegion region, World world) {
        return countEntities(region, world) >= this.maxEntitiesPerPlot;
    }

    public void addEntity(ProtectedRegion region, World world, UUID uuid, Location firstSeenLoc) {
        Map<ProtectedRegion, LinkedHashSet<UUID>> innerPlots;
        Map<ProtectedRegion, Long> innerRegionRealizedTime;
        LinkedHashSet<UUID> entities;
        if(this.plots.containsKey(world)) {
            innerPlots = this.plots.get(world);
            if(innerPlots.containsKey(region)) {
                entities = innerPlots.get(region);
                innerRegionRealizedTime = this.regionRealizedTime.get(world);
            } else {
                entities = new LinkedHashSet<>();
                innerRegionRealizedTime = new HashMap<>();
            }
        } else {
            innerPlots = new HashMap<>();
            entities = new LinkedHashSet<>();
            innerRegionRealizedTime = new HashMap<>();
        }
        entities.add(uuid);
        innerPlots.put(region, entities);
        this.plots.put(world, innerPlots);
        innerRegionRealizedTime.put(region, System.currentTimeMillis());
        this.regionRealizedTime.put(world, innerRegionRealizedTime);
    }

    public void removeEntity(ProtectedRegion region, UUID uuid, World world) {
        if(this.plots.containsKey(world)) {
            if(this.plots.get(world).containsKey(region)) {
                Map<ProtectedRegion, LinkedHashSet<UUID>> innerPlots = this.plots.get(world);
                innerPlots.get(region).remove(uuid);
                this.plots.put(world, innerPlots);
            }
        }
    }

    public boolean isEntityUnknown(UUID uuid, World world) {
        if(this.plots.containsKey(world)) {
            for(LinkedHashSet<UUID> plotUUIDS : this.plots.get(world).values()) {
                for(UUID u : plotUUIDS) {
                    if(u.equals(uuid)) return false;
                }
            }
        }
        return true;
    }

    public ProtectedRegion getRegionForEntity(UUID uuid, World world) {
        ProtectedRegion out = null;
        if(this.plots.containsKey(world)) {
            for(ProtectedRegion region : this.plots.get(world).keySet()) {
                if(this.plots.get(world).get(region).contains(uuid)) {
                    if(out == null) {
                        out = region;
                    } else if(this.regionRealizedTime.get(world).get(region) > this.regionRealizedTime.get(world).get(out)) {
                        out = region;
                    }
                }
            }
        }
        return out;
    }

    public LinkedHashSet<UUID> getEntitiesAtRegion(ProtectedRegion region, World world) {
        if(!this.plots.containsKey(world) || !this.plots.get(world).containsKey(region)) redefineRegion(region, world);
        return this.plots.get(world).get(region);
    }

    private void redefineRegion(ProtectedRegion region, World world) {
        Vector min = (new Location(world, region.getMinimumPoint().getX(), region.getMinimumPoint().getY(), region.getMinimumPoint().getZ())).toVector();
        Vector max = (new Location(world, region.getMaximumPoint().getX(), region.getMaximumPoint().getY(), region.getMaximumPoint().getZ())).toVector();
        max.setX(max.getX() + 0.999);
        max.setY(max.getY() + 0.999);
        max.setZ(max.getZ() + 0.999);
        LinkedHashSet<UUID> entities = new LinkedHashSet<>();
        for(Entity e : world.getEntities()) {
            if(!(e instanceof Mob)) continue;
            if(e.getLocation().toVector().isInAABB(min, max)) {
                entities.add(e.getUniqueId());
            }
        }
        Map<ProtectedRegion, LinkedHashSet<UUID>> innerPlots;
        Map<ProtectedRegion, Long> innerRegionRealizedTime;
        if(this.plots.containsKey(world)) {
            innerPlots = this.plots.get(world);
            innerRegionRealizedTime = this.regionRealizedTime.get(world);
        } else {
            innerPlots = new HashMap<>();
            innerRegionRealizedTime = new HashMap<>();
        }
        innerPlots.put(region, entities);
        innerRegionRealizedTime.put(region, System.currentTimeMillis());
        this.plots.put(world, innerPlots);
        this.regionRealizedTime.put(world, innerRegionRealizedTime);
    }

    public void verifyEntities(ProtectedRegion region, World world) {
        if(this.plots.containsKey(world)) {
            if(this.plots.get(world).containsKey(region)) {
                Iterator<UUID> iter = this.plots.get(world).get(region).iterator();
                while(iter.hasNext()) {
                    UUID uuid = iter.next();
                    Entity entity = Bukkit.getEntity(uuid);
                    if((entity == null || !getRegionsAtLoc(entity.getLocation()).contains(region)) && !EntityHandler.beingRidden(uuid) && !EntityHandler.beingLaunched(uuid)) {
                        iter.remove();
                    }
                }
            }
        }
    }

    public List<ProtectedRegion> getRegionsAtLoc(Location loc) {
        List<ProtectedRegion> regions = new ArrayList<>();
        RegionQuery regionQuery = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
        ApplicableRegionSet applicableRegionSet = regionQuery.getApplicableRegions(BukkitAdapter.adapt(loc));
        for(ProtectedRegion region : applicableRegionSet) {
            if(!region.getId().equalsIgnoreCase("__global__") && region.getParent() == null) {
                regions.add(region);
            }
        }
        return regions;
    }
}
