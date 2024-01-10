package org.cubeville.cvcreativeutils.handlers;

import net.minecraft.nbt.NBTTagCompound;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Sign;
import org.bukkit.craftbukkit.v1_20_R2.inventory.CraftItemStack;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SpawnEggMeta;
import org.bukkit.potion.PotionEffect;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ItemHandler implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryCreative(InventoryClickEvent event) {
        if(event.getClickedInventory() != null) checkEntireInventory(event.getClickedInventory());
        checkEntireInventory(event.getInventory());
        /*if(event.isCancelled()) return;
        ItemStack itemStack = event.getCursor();
        int byteCount = getByteCount(itemStack);
        if(byteCount == 0) return;
        if(byteCount > 100000 || isItemNBTBanned(itemStack)) {
            event.setCancelled(true);
            if(event.getClickedInventory() != null) event.getClickedInventory().setItem(event.getSlot(), createNewStack(itemStack));
            event.getWhoClicked().sendMessage(ChatColor.RED + String.valueOf(itemStack.getType()) + " had a NBT value above what is allowed! NBT Cleared!");
        }*/
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryOpen(InventoryOpenEvent event) {
        checkEntireInventory(event.getInventory());
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
                if(getByteCount(itemStack) > 100000|| isItemNBTBanned(itemStack)) {
                    inventory.setItem(i, createNewStack(itemStack));
                }
            }
            i++;
        }
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

    private boolean isItemNBTBanned(ItemStack itemStack) {
        boolean banned = false;
        boolean hasItemMeta = itemStack.hasItemMeta();
        if(hasItemMeta && itemStack.getItemMeta().hasAttributeModifiers()) {
            for(Attribute attribute : itemStack.getItemMeta().getAttributeModifiers().keySet()) {
                List<AttributeModifier> list = new ArrayList<>(itemStack.getItemMeta().getAttributeModifiers().get(attribute));
                for(AttributeModifier modifier : list) {
                    if(modifier.getAmount() > 10) {
                        banned = true;
                        break;
                    }
                }
            }
        }
        if(itemStack instanceof Sign || (hasItemMeta && itemStack.getItemMeta() instanceof SpawnEggMeta)) {
            banned = true;
        } else if(hasItemMeta && itemStack.getItemMeta() instanceof PotionMeta) {
            for(PotionEffect effect : ((PotionMeta)itemStack.getItemMeta()).getCustomEffects()) {
                if(effect.getAmplifier() > 10) {
                    banned = true;
                    break;
                }
            }
        }
        return banned;
    }
}
