package org.cubeville.cvcreativeutils.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.cubeville.commons.commands.BaseCommand;
import org.cubeville.commons.commands.CommandResponse;
import org.cubeville.cvcreativeutils.CVCreativeUtils;
import org.cubeville.cvcreativeutils.handlers.EntityHandler;
import org.cubeville.cvcreativeutils.PlotManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ReloadCommand extends BaseCommand {

    private final CVCreativeUtils plugin;
    private final PlotManager plotManager;
    private final EntityHandler entityHandler;

    public ReloadCommand(CVCreativeUtils plugin, PlotManager plotManager, EntityHandler entityHandler) {
        super("");
        setPermission("cvcreativeutils.reload");
        this.plugin = plugin;
        this.plotManager = plotManager;
        this.entityHandler = entityHandler;
    }

    @Override
    public CommandResponse execute(CommandSender sender, Set<String> flags, Map<String, Object> parameters, List<Object> baseParameters) {
        plotManager.setMaxEntitiesPerPlot(plugin.getConfig().getInt("entities-per-plot", 10));
        List<EntityType> bannedEntities = new ArrayList<>();
        for(String entity : plugin.getConfig().getStringList("banned-entities")) {
            try {
                EntityType type = EntityType.valueOf(entity.toUpperCase());
                bannedEntities.add(type);
            } catch(IllegalArgumentException ignored) { }
        }
        entityHandler.setBannedEntities(bannedEntities);
        return new CommandResponse("Config reloaded! entities-per-plot set to " + plotManager.maxEntitiesPerPlot + " and banned-entities set to " + bannedEntities);
    }
}
