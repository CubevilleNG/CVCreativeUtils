package org.cubeville.cvcreativeutils.handlers;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import io.papermc.paper.event.entity.EntityMoveEvent;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerEggThrowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.util.Vector;
import org.cubeville.cvcreativeutils.CVCreativeUtils;
import org.cubeville.cvcreativeutils.PlotManager;
import org.spigotmc.event.entity.EntityDismountEvent;
import org.spigotmc.event.entity.EntityMountEvent;

import java.util.*;

public class EntityHandler implements Listener {

    private final CVCreativeUtils plugin;
    private final PlotManager plotManager;
    private List<EntityType> bannedEntities;

    private static Set<UUID> beingLaunched;
    private static Map<Entity, Location> beingRidden;
    private static Map<Entity, Location> everyLivingEntity;
    private final Map<UUID, List<ProtectedRegion>> vehicleRegions;
    private final Map<UUID, List<ProtectedRegion>> fireballRegions;
    private final Map<UUID, Integer> fireballTaskIDs;

    public EntityHandler(CVCreativeUtils plugin, PlotManager plotManager, List<EntityType> bannedEntities) {
        this.plugin = plugin;
        this.plotManager = plotManager;
        this.bannedEntities = bannedEntities;
        beingLaunched = new HashSet<>();
        beingRidden = new HashMap<>();
        everyLivingEntity = new HashMap<>();
        vehicleRegions = new HashMap<>();
        fireballRegions = new HashMap<>();
        fireballTaskIDs = new HashMap<>();
        activeChunkCheck();
        startDismountedCheck();
        startEveryEntityCheck();
    }

    public void setBannedEntities(List<EntityType> bannedEntities) {
        this.bannedEntities = bannedEntities;
    }

    private boolean isEntityBanned(EntityType type) {
        return this.bannedEntities.contains(type);
    }

    public static boolean beingLaunched(UUID uuid) {
        return beingLaunched.contains(uuid);
    }

    public static boolean beingRidden(UUID uuid) {
        for(Entity e : beingRidden.keySet()) {
            if(e.getUniqueId().equals(uuid)) return true;
        }
        return false;
    }

    private void activeChunkCheck() {
        for(World world : Bukkit.getWorlds()) {
            if(!plotManager.isWorldControlled(world)) continue;
            for(Chunk chunk : world.getLoadedChunks()) {
                for(Entity entity : chunk.getEntities()) {
                    if(!(entity instanceof Mob)) continue;
                    if(plotManager.isEntityUnknown(entity.getUniqueId(), world)) {
                        Location loc = entity.getLocation();
                        List<ProtectedRegion> regions = this.plotManager.getRegionsAtLoc(loc);
                        if(regions.isEmpty()) continue;
                        for(ProtectedRegion region : regions) {
                            plotManager.addEntity(region, world, entity.getUniqueId(), entity.getLocation());
                            everyLivingEntity.put(entity, entity.getLocation());
                        }
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityMount(EntityMountEvent event) {
        if(event.isCancelled()) return;
        if(this.plotManager.isEntityUnknown(event.getMount().getUniqueId(), event.getMount().getWorld())) return;
        beingRidden.put(event.getMount(), event.getMount().getLocation());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDismount(EntityDismountEvent event) {
        if(event.isCancelled()) return;
        if(this.plotManager.isEntityUnknown(event.getDismounted().getUniqueId(), event.getDismounted().getWorld())) return;
        if(beingRidden.containsKey(event.getDismounted()) && entityOutsideOfRegion(event.getDismounted())) {
            teleportEntityBackAfterRiding(event.getDismounted());
            return;
        }
        beingRidden.remove(event.getDismounted());
    }

    private void startDismountedCheck() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            Map<Entity, Location> copyBeingRidden = new HashMap<>(beingRidden);
            for(Entity entity : copyBeingRidden.keySet()) {
                if(entityOutsideOfRegion(entity)) {
                    teleportEntityBackAfterRiding(entity);
                } else {
                    beingRidden.put(entity, entity.getLocation());
                }
            }
        }, 20, 40);
    }

    private void startEveryEntityCheck() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            Map<Entity, Location> copyEveryLivingEntity = new HashMap<>(everyLivingEntity);
            for(Entity entity : copyEveryLivingEntity.keySet()) {
                if(!beingLaunched(entity.getUniqueId())) {
                    if(entityOutsideOfRegion(entity)) {
                        teleportEntityBack(entity);
                        beingLaunched.add(entity.getUniqueId());
                    } else {
                        everyLivingEntity.put(entity, entity.getLocation());
                    }
                }
            }
        }, 20, 40);
    }

    private boolean entityOutsideOfRegion(Entity entity) {
        boolean outside = true;
        for(ProtectedRegion currentRegion : this.plotManager.getRegionsAtLoc(entity.getLocation())) {
            if(this.plotManager.getRegionForEntity(entity.getUniqueId(), entity.getWorld()) != null &&
                    this.plotManager.getRegionForEntity(entity.getUniqueId(), entity.getWorld()).equals(currentRegion)) {
                outside = false;
                break;
            }
        }
        return outside;
    }

    private void teleportEntityBackAfterRiding(Entity entity) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if(beingRidden.containsKey(entity)) {
                if(!entity.getPassengers().isEmpty()) entity.eject();
                entity.teleport(beingRidden.get(entity));
                beingRidden.remove(entity);
            }
        }, 5);
    }

    private void teleportEntityBack(Entity entity) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            entity.teleport(everyLivingEntity.get(entity));
            beingLaunched.remove(entity.getUniqueId());
        }, 5);
    }

    private void passPlayerMoveEvents(Player player, Location fromLoc, Location toLoc) {
        if(!plotManager.isWorldControlled(fromLoc.getWorld())) return;
        boolean leftShoulder = false;
        boolean rightShoulder = false;
        if(player.getShoulderEntityLeft() != null && (player.getShoulderEntityLeft() instanceof Mob)) leftShoulder = true;
        if(player.getShoulderEntityRight() != null && (player.getShoulderEntityRight() instanceof Mob)) rightShoulder = true;
        if(!leftShoulder && !rightShoulder) return;

        List<ProtectedRegion> fromRegions = this.plotManager.getRegionsAtLoc(fromLoc);
        if(!fromRegions.isEmpty()) {
            List<ProtectedRegion> toRegions = this.plotManager.getRegionsAtLoc(toLoc);
            if(!fromRegions.equals(toRegions)) {
                if(leftShoulder) player.releaseLeftShoulderEntity();
                if(rightShoulder) player.releaseRightShoulderEntity();
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if(event.isCancelled()) return;
        Player player = event.getPlayer();
        Location fromLoc = event.getFrom();
        Location toLoc = event.getTo();
        passPlayerMoveEvents(player, fromLoc, toLoc);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerMove(PlayerMoveEvent event) {
        if(event.isCancelled()) return;
        Player player = event.getPlayer();
        Location fromLoc = event.getFrom();
        Location toLoc = event.getTo();
        passPlayerMoveEvents(player, fromLoc, toLoc);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityTeleport(EntityTeleportEvent event) {
        if(event.isCancelled()) return;
        if(!(event.getEntity() instanceof Mob)) return;
        if(!plotManager.isWorldControlled(event.getFrom().getWorld())) return;
        if(beingRidden.containsKey(event.getEntity())) return;
        if(beingLaunched(event.getEntity().getUniqueId())) return;
        Location fromLoc = event.getFrom();
        Location toLoc = event.getTo();
        List<ProtectedRegion> fromRegions = this.plotManager.getRegionsAtLoc(fromLoc);
        if(fromRegions.isEmpty()) return;
        List<ProtectedRegion> toRegions = this.plotManager.getRegionsAtLoc(toLoc);
        if(!fromRegions.equals(toRegions)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityMove(EntityMoveEvent event) {
        if(event.isCancelled()) return;
        if(!(event.getEntity() instanceof Mob)) return;
        if(beingLaunched(event.getEntity().getUniqueId())) return;
        if(!plotManager.isWorldControlled(event.getFrom().getWorld())) return;
        Location fromLoc = event.getFrom();
        Location toLoc = event.getTo();
        List<ProtectedRegion> fromRegions = this.plotManager.getRegionsAtLoc(fromLoc);
        if(fromRegions.isEmpty()) return;
        List<ProtectedRegion> toRegions = this.plotManager.getRegionsAtLoc(toLoc);
        if(!fromRegions.equals(toRegions)) {
            event.setCancelled(true);
            beingLaunched.add(event.getEntity().getUniqueId());
            launchEntityBack(fromLoc, toLoc, event.getEntity(), fromRegions);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onVehicleMove(VehicleMoveEvent event) {
        if(!plotManager.isWorldControlled(event.getFrom().getWorld())) return;
        if(event.getVehicle() instanceof LivingEntity) return;
        if(beingLaunched(event.getVehicle().getUniqueId())) return;
        Location fromLoc = event.getFrom();
        Location toLoc = event.getTo();
        List<ProtectedRegion> fromRegions = this.plotManager.getRegionsAtLoc(fromLoc);
        if(fromRegions.isEmpty()) return;
        List<ProtectedRegion> toRegions = this.plotManager.getRegionsAtLoc(toLoc);
        if(!fromRegions.equals(toRegions)) {
            UUID uuid = event.getVehicle().getUniqueId();
            beingLaunched.add(uuid);
            if(!vehicleRegions.containsKey(uuid)) {
                vehicleRegions.put(uuid, fromRegions);
                checkVehicleRegion(event.getVehicle().getUniqueId());
            }
            launchEntityBack(fromLoc, toLoc, event.getVehicle(), fromRegions);
        }
    }

    private void checkVehicleRegion(UUID uuid) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Vehicle vehicle = (Vehicle) Bukkit.getEntity(uuid);
            if(vehicle != null) {
                List<ProtectedRegion> currentRegions = this.plotManager.getRegionsAtLoc(vehicle.getLocation());
                if(!vehicleRegions.get(vehicle.getUniqueId()).equals(currentRegions)) {
                    vehicle.remove();
                }
            }
            vehicleRegions.remove(uuid);
        }, 100);
    }

    private void checkProjectileRegion(UUID uuid) {
        int taskID = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            boolean remove = true;
            Projectile projectile = (Projectile) Bukkit.getEntity(uuid);
            if(projectile != null) {
                List<ProtectedRegion> currentRegions = this.plotManager.getRegionsAtLoc(projectile.getLocation());
                if(!fireballRegions.get(projectile.getUniqueId()).equals(currentRegions)) {
                    projectile.remove();
                } else {
                    remove = false;
                }
            }
            if(remove) {
                vehicleRegions.remove(uuid);
                int task = fireballTaskIDs.remove(uuid);
                Bukkit.getScheduler().cancelTask(task);
            }
        }, 5, 5).getTaskId();
        fireballTaskIDs.put(uuid, taskID);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChunkLoad(ChunkLoadEvent event) {
        if(!plotManager.isWorldControlled(event.getWorld())) return;
        for(Entity entity : event.getChunk().getEntities()) {
            if(!(entity instanceof Mob)) continue;
            Location loc = entity.getLocation();
            if(plotManager.isEntityUnknown(entity.getUniqueId(), loc.getWorld())) {
                List<ProtectedRegion> regions = this.plotManager.getRegionsAtLoc(loc);
                if(regions.isEmpty()) continue;
                for(ProtectedRegion region : regions) {
                    plotManager.addEntity(region, loc.getWorld(), entity.getUniqueId(), entity.getLocation());
                    everyLivingEntity.put(entity, entity.getLocation());
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntitySpawn(EntitySpawnEvent event) {
        if(event.isCancelled()) return;
        boolean isMob = event.getEntity() instanceof Mob;
        boolean isFireball = event.getEntity() instanceof Fireball;
        if(!isMob && !isFireball) return;
        Location loc = event.getLocation();
        if(!plotManager.isWorldControlled(loc.getWorld())) return;
        List<ProtectedRegion> regions = this.plotManager.getRegionsAtLoc(loc);
        if(regions.isEmpty()) return;
        if(isMob) {
            for(ProtectedRegion region : regions) {
                this.plotManager.verifyEntities(region, loc.getWorld());
                if(plotManager.maxEntitiesSpawned(region, loc.getWorld())) {
                    event.setCancelled(true);
                    return;
                }
            }
            for(ProtectedRegion region : regions) {
                plotManager.addEntity(region, loc.getWorld(), event.getEntity().getUniqueId(), event.getLocation());
                everyLivingEntity.put(event.getEntity(), event.getLocation());
            }
        } else {
            fireballRegions.put(event.getEntity().getUniqueId(), regions);
            checkProjectileRegion(event.getEntity().getUniqueId());
        }
    }

    private void passEntityRemoveEvents(Entity entity) {
        if(!plotManager.isWorldControlled(entity.getWorld())) return;
        Location loc = entity.getLocation();
        List<ProtectedRegion> regions = this.plotManager.getRegionsAtLoc(loc);
        if(regions.isEmpty()) return;
        for(ProtectedRegion region : regions) {
            plotManager.removeEntity(region, entity.getUniqueId(), entity.getWorld());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDeath(EntityDeathEvent event) {
        passEntityRemoveEvents(event.getEntity());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBeeEnterBlock(EntityEnterBlockEvent event) {
        passEntityRemoveEvents(event.getEntity());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSilverfishEnterBlock(EntityChangeBlockEvent event) {
        passEntityRemoveEvents(event.getEntity());
    }

    private void launchEntityBack(Location fromLoc, Location toLoc, Entity entity, List<ProtectedRegion> fromRegions) {
        entity.setInvulnerable(true);
        Vector origVector = fromLoc.toVector().subtract(toLoc.toVector()).normalize();
        Vector blastVector = origVector.clone();
        blastVector.multiply(1).setY(.4);
        entity.setVelocity(blastVector);
        entity.getWorld().playEffect(entity.getLocation(), Effect.GHAST_SHOOT, 0, 15);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if(!this.plotManager.getRegionsAtLoc(entity.getLocation()).contains(this.plotManager.getRegionForEntity(entity.getUniqueId(), entity.getWorld()))) {
            //if(!fromRegions.equals(this.plotManager.getRegionsAtLoc(entity.getLocation()))) {
                if(!entity.getPassengers().isEmpty()) entity.eject();
                entity.teleport(fromLoc);
                entity.setVelocity(new Vector());
            }
            beingLaunched.remove(entity.getUniqueId());
            entity.setInvulnerable(false);
        }, 20);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBannedEntitySpawn(EntitySpawnEvent event) {
        if(event.isCancelled()) return;
        if(event.getLocation().getWorld() != null && plotManager.isWorldControlled(event.getLocation().getWorld()) && isEntityBanned(event.getEntityType())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteractDragonEgg(PlayerInteractEvent event) {
        if(!plotManager.isWorldControlled(event.getPlayer().getWorld())) return;
        if(event.getAction().equals(Action.RIGHT_CLICK_BLOCK) && event.getClickedBlock() != null && event.getClickedBlock().getType().equals(Material.DRAGON_EGG)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerEggThrow(PlayerEggThrowEvent event) {
        event.setHatching(false);
    }
}
