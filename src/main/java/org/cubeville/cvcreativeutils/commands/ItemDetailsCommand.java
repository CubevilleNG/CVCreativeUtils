package org.cubeville.cvcreativeutils.commands;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.cubeville.commons.commands.Command;
import org.cubeville.commons.commands.CommandResponse;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ItemDetailsCommand extends Command {

    private Logger logger;

    public ItemDetailsCommand(Logger logger) {
        super("");
        addFlag("inchat");
        setPermission("cvcreativeutils.itemdetails");
        this.logger = logger;
    }

    @Override
    public CommandResponse execute(Player player, Set<String> flags, Map<String, Object> parameters, List<Object> baseParameters) {
        ItemStack itemStack = player.getInventory().getItemInMainHand();
        logger.log(Level.INFO, "Item details for itemstack in " + player.getName() + "'s main hand: " + itemStack);
        if(flags.contains("inchat")) {
            return new CommandResponse(ChatColor.LIGHT_PURPLE + "Item details for " + ChatColor.GOLD + itemStack.getType() + ChatColor.LIGHT_PURPLE + ": " +
                    ChatColor.GOLD + itemStack);
        } else {
            return new CommandResponse(ChatColor.LIGHT_PURPLE + "Item details for " + ChatColor.GOLD + itemStack.getType() + ChatColor.LIGHT_PURPLE + " sent to console. " +
                    "Use " + ChatColor.GOLD + "/itemdetails inchat " + ChatColor.LIGHT_PURPLE + "to display item details in chat.");
        }
    }
}
