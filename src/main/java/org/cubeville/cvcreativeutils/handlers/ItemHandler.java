package org.cubeville.cvcreativeutils.handlers;

import net.kyori.adventure.text.Component;
import net.minecraft.nbt.NBTTagCompound;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.block.ChiseledBookshelf;
import org.bukkit.block.Container;
import org.bukkit.block.Sign;
import org.bukkit.craftbukkit.v1_20_R2.inventory.CraftItemStack;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.*;
import org.bukkit.potion.PotionEffect;
import org.cubeville.cvcreativeutils.CVCreativeUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ItemHandler implements Listener {

    CVCreativeUtils plugin;

    public ItemHandler(CVCreativeUtils plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryCreative(InventoryClickEvent event) {
        if(event.getClickedInventory() != null) checkEntireInventory(event.getClickedInventory());
        checkEntireInventory(event.getInventory());
        ItemStack cursorStack = event.getCursor();
        if(cursorStack != null && isItemBanned(cursorStack)) event.setCursor(createNewStack(cursorStack));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryOpen(InventoryOpenEvent event) {
        checkEntireInventory(event.getInventory());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryMove(InventoryMoveItemEvent event) {
        checkEntireInventory(event.getSource());
        checkEntireInventory(event.getDestination());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onContainerPlace(BlockPlaceEvent event) {
        if(event.isCancelled()) return;
        if(event.getBlockPlaced().getState() instanceof Container) {
            checkEntireInventory(((Container)event.getBlockPlaced().getState()).getInventory());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBookshelfTake(PlayerInteractEvent event) {
        if(event.isCancelled()) return;
        if(event.getAction().isRightClick()) {
            Block block = event.getClickedBlock();
            if(block != null && block.getState() instanceof ChiseledBookshelf) {
                ChiseledBookshelf bookshelf = (ChiseledBookshelf) block.getState();
                checkEntireInventory(bookshelf.getInventory());
                Bukkit.getScheduler().runTaskLater(plugin, () -> checkEntireInventory(event.getPlayer().getInventory()), 1);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntitySpawn(EntitySpawnEvent event) {
        if(event.isCancelled()) return;
        if(event.getEntity() instanceof Player) return;
        EntityType type = event.getEntityType();
        if(type.equals(EntityType.DROPPED_ITEM)) { //check dropped items
            Item item = ((Item) event.getEntity());
            ItemStack itemStack = item.getItemStack();
            if(isItemBanned(itemStack)) {
                item.setItemStack(createNewStack(itemStack));
            }
        } else if(event.getEntity() instanceof LivingEntity) { //check living entity equipment contents
            checkEntireLivingEntityEquipment((LivingEntity) event.getEntity());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteractAtLivingEntity(PlayerInteractAtEntityEvent event) {
        if(event.isCancelled()) return;
        Entity entity = event.getRightClicked();
        if(entity instanceof Player) return;
        if(!(entity instanceof LivingEntity)) return;
        checkEntireLivingEntityEquipment((LivingEntity) entity);
        checkEntireInventory(event.getPlayer().getInventory());
        if(entity instanceof Villager) {
            Villager villager = (Villager) entity;
            List<MerchantRecipe> recipes = villager.getRecipes();
            int i = 0;
            for(MerchantRecipe recipe : recipes) {
                if(isItemBanned(recipe.getResult())) {
                    MerchantRecipe newRecipe = new MerchantRecipe(createNewStack(recipe.getResult()),
                            recipe.getUses(), recipe.getMaxUses(), recipe.hasExperienceReward(), recipe.getVillagerExperience(),
                            recipe.getPriceMultiplier(), recipe.getDemand(), recipe.getSpecialPrice(), recipe.shouldIgnoreDiscounts());
                    for(ItemStack ingredient : recipe.getIngredients()) newRecipe.addIngredient(ingredient);
                    villager.setRecipe(i, newRecipe);
                }
                i++;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onItemPickup(PlayerAttemptPickupItemEvent event) {
        if(event.isCancelled()) return;
        checkEntireInventory(event.getPlayer().getInventory());
    }

    private void checkEntireLivingEntityEquipment(LivingEntity livingEntity) {
        EntityEquipment equipment = livingEntity.getEquipment();
        if(equipment == null) return;
        if(isItemBanned(equipment.getHelmet())) equipment.setItem(EquipmentSlot.HEAD, createNewStack(equipment.getHelmet()));
        if(isItemBanned(equipment.getChestplate())) equipment.setItem(EquipmentSlot.CHEST, createNewStack(equipment.getChestplate()));
        if(isItemBanned(equipment.getLeggings())) equipment.setItem(EquipmentSlot.LEGS, createNewStack(equipment.getLeggings()));
        if(isItemBanned(equipment.getBoots())) equipment.setItem(EquipmentSlot.FEET, createNewStack(equipment.getBoots()));
        if(isItemBanned(equipment.getItemInMainHand())) equipment.setItem(EquipmentSlot.HAND, createNewStack(equipment.getItemInMainHand()));
        if(isItemBanned(equipment.getItemInOffHand())) equipment.setItem(EquipmentSlot.OFF_HAND, createNewStack(equipment.getItemInOffHand()));
    }

    /*private void checkInventoryHolderEntity(InventoryHolder entity) {
        Inventory inv = entity.getInventory();
        int i = 0;
        for(ItemStack item : inv) {
            if(item != null && isItemBanned(item)) entity.getInventory().setItem(i, createNewStack(item));
            i++;
        }
    }*/

    private ItemStack createNewStack(ItemStack itemStack) {
        return new ItemStack(itemStack.getType(), itemStack.getAmount());
    }

    private void checkEntireInventory(Inventory inventory) {
        int i = 0;
        for(ItemStack itemStack : inventory.getContents()) {
            if(itemStack != null && !itemStack.getType().equals(Material.AIR)) {
                if(isItemBanned(itemStack)) {
                    inventory.setItem(i, createNewStack(itemStack));
                }
            }
            i++;
        }
    }

    private boolean isItemBanned(ItemStack itemStack) {
        return getByteCount(itemStack) > 100000 || failedDetailedCheck(itemStack);
    }

    private int getByteCount(ItemStack itemStack) {
        net.minecraft.world.item.ItemStack nmsItem = CraftItemStack.asNMSCopy(itemStack);
        NBTTagCompound nbtTagCompound = nmsItem.v();
        int byteCount = 0;
        if(nbtTagCompound != null) {
            try {
                nbtTagCompound.a(ByteCountDataOutput.INSTANCE);
                byteCount = ByteCountDataOutput.INSTANCE.getCount();
                ByteCountDataOutput.INSTANCE.reset();
            } catch(IOException e) {
                e.printStackTrace();
            }
        }
        return byteCount;
    }

    private boolean failedDetailedCheck(ItemStack itemStack) {
        boolean hasItemMeta = itemStack.hasItemMeta();
        if(hasItemMeta && itemStack.getItemMeta().hasAttributeModifiers()) { //checks if any attributes have values higher than 10
            for(Attribute attribute : itemStack.getItemMeta().getAttributeModifiers().keySet()) {
                List<AttributeModifier> list = new ArrayList<>(itemStack.getItemMeta().getAttributeModifiers().get(attribute));
                for(AttributeModifier modifier : list) {
                    if(modifier.getAmount() > 10) {
                        return true;
                    }
                }
            }
        }
        if(itemStack instanceof Sign) { //check any sign nbt. could possibly treat this like books(see below) in future but for now wiping them all.
            return true;
        } else if(hasItemMeta && itemStack.getItemMeta() instanceof SpawnEggMeta) {
            if(((SpawnEggMeta) itemStack.getItemMeta()).getCustomSpawnedType() != null) return true;
        } else if(hasItemMeta) {
            if(itemStack.getItemMeta() instanceof PotionMeta) { //check if amplifiers have value higher than 10
                for(PotionEffect effect : ((PotionMeta)itemStack.getItemMeta()).getCustomEffects()) {
                    if(effect.getAmplifier() > 10) {
                        return true;
                    }
                }
            } else if(itemStack.getItemMeta() instanceof SkullMeta) { //check if skull has noteblocksound associated with it
                return ((SkullMeta) itemStack.getItemMeta()).getNoteBlockSound() != null;
            } else if(itemStack.getItemMeta() instanceof BundleMeta) { //check each itemstack within the bundle
                if(((BundleMeta)itemStack.getItemMeta()).hasItems()) {
                    for(ItemStack stack : ((BundleMeta)itemStack.getItemMeta()).getItems()) {
                        if(isItemBanned(stack)) return true;
                    }
                }
            } else if(itemStack.getItemMeta() instanceof BookMeta) { //check if any pages of the book have components with click events
                for(Component component : ((BookMeta)itemStack.getItemMeta()).pages()) {
                    if(component.clickEvent() != null) return true;
                }
            } else if(!itemStack.getEnchantments().isEmpty()) { //check if enchantments have value higher than 10
                for(Enchantment enchantment : itemStack.getEnchantments().keySet()) {
                    if(itemStack.getEnchantments().get(enchantment) > 10) return true;
                }
            }
        }
        return false;
    }
}
