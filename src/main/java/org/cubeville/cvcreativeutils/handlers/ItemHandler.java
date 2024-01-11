package org.cubeville.cvcreativeutils.handlers;

import net.kyori.adventure.text.Component;
import net.minecraft.nbt.NBTTagCompound;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.block.ChiseledBookshelf;
import org.bukkit.block.Container;
import org.bukkit.block.Sign;
import org.bukkit.craftbukkit.v1_20_R2.inventory.CraftItemStack;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.*;
import org.bukkit.potion.PotionEffect;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ItemHandler implements Listener {

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
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onItemDrop(EntitySpawnEvent event) {
        if(event.isCancelled()) return;
        if(event.getEntityType().equals(EntityType.DROPPED_ITEM)) {
            Item item = ((Item) event.getEntity());
            ItemStack itemStack = item.getItemStack();
            if(isItemBanned(itemStack)) {
                item.setItemStack(createNewStack(itemStack));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onItemPickup(PlayerAttemptPickupItemEvent event) {
        if(event.isCancelled()) return;
        checkEntireInventory(event.getPlayer().getInventory());
    }

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
        if(hasItemMeta && itemStack.getItemMeta().hasAttributeModifiers()) {
            for(Attribute attribute : itemStack.getItemMeta().getAttributeModifiers().keySet()) {
                List<AttributeModifier> list = new ArrayList<>(itemStack.getItemMeta().getAttributeModifiers().get(attribute));
                for(AttributeModifier modifier : list) {
                    if(modifier.getAmount() > 10) {
                        return true;
                    }
                }
            }
        }
        if(itemStack instanceof Sign || (hasItemMeta && itemStack.getItemMeta() instanceof SpawnEggMeta)) {
            return true;
        } else if(hasItemMeta && itemStack.getItemMeta() instanceof PotionMeta) {
            for(PotionEffect effect : ((PotionMeta)itemStack.getItemMeta()).getCustomEffects()) {
                if(effect.getAmplifier() > 10) {
                    return true;
                }
            }
        } else if(hasItemMeta && itemStack.getItemMeta() instanceof SkullMeta) {
            return ((SkullMeta) itemStack.getItemMeta()).getNoteBlockSound() != null;
        } else if(hasItemMeta && itemStack.getItemMeta() instanceof BundleMeta) {
            if(((BundleMeta)itemStack.getItemMeta()).hasItems()) {
                for(ItemStack stack : ((BundleMeta)itemStack.getItemMeta()).getItems()) {
                    if(isItemBanned(stack)) return true;
                }
            }
        } else if(hasItemMeta && itemStack.getItemMeta() instanceof BookMeta) {
            for(Component component : ((BookMeta)itemStack.getItemMeta()).pages()) {
                if(component.clickEvent() != null) return true;
            }
        }
        return false;
    }
}
