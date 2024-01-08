package org.cubeville.cvcreativeutils.handlers;

import net.minecraft.nbt.NBTTagCompound;
import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Sign;
import org.bukkit.craftbukkit.v1_20_R2.inventory.CraftItemStack;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SpawnEggMeta;
import org.bukkit.potion.PotionEffect;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ItemHandler implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryCreative(InventoryCreativeEvent event) {
        if(event.isCancelled()) return;
        ItemStack itemStack = event.getCursor();
        int byteCount = getByteCount(itemStack);
        if(byteCount == 0) return;
        if(byteCount > 100000 || isItemNBTBanned(itemStack)) {
            event.setCancelled(true);
            ItemStack newItem = new ItemStack(itemStack.getType(), itemStack.getAmount());
            if(event.getClickedInventory() != null) event.getClickedInventory().setItem(event.getSlot(), newItem);
            event.getWhoClicked().sendMessage(ChatColor.RED + String.valueOf(itemStack.getType()) + " had a NBT value above what is allowed! NBT Cleared!");
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
        if(itemStack instanceof Sign) {
            banned = true;
        } else if(hasItemMeta && itemStack.getItemMeta() instanceof SpawnEggMeta) {
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
