package org.cubeville.cvcreativeutils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.java.JavaPlugin;
import org.cubeville.commons.commands.CommandParser;
import org.cubeville.cvcreativeutils.commands.MobCountCommand;
import org.cubeville.cvcreativeutils.commands.MobKillCommand;
import org.cubeville.cvcreativeutils.commands.MobTpCommand;
import org.cubeville.cvcreativeutils.commands.ReloadCommand;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CVCreativeUtils extends JavaPlugin {

    private Logger logger;
    private CommandParser reloadParser;
    private CommandParser mobcountParser;
    private CommandParser mobtpParser;
    private CommandParser mobkillParser;

    private int entitiesPerPlot;
    private List<World> controlledWorlds;
    private List<EntityType> bannedEntities;

    @Override
    public void onEnable() {
        this.logger = this.getLogger();
        this.controlledWorlds = new ArrayList<>();
        this.bannedEntities = new ArrayList<>();
        initConfig();
        PlotManager plotManager = new PlotManager(entitiesPerPlot, controlledWorlds);
        EntityHandler entityHandler = new EntityHandler(this, plotManager, bannedEntities);
        Bukkit.getPluginManager().registerEvents(entityHandler, this);
        this.reloadParser = new CommandParser();
        this.reloadParser.addCommand(new ReloadCommand(this, plotManager, entityHandler));
        this.mobcountParser = new CommandParser();
        this.mobcountParser.addCommand(new MobCountCommand(plotManager));
        this.mobtpParser = new CommandParser();
        this.mobtpParser.addCommand(new MobTpCommand(plotManager));
        this.mobkillParser = new CommandParser();
        this.mobkillParser.addCommand(new MobKillCommand(plotManager));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(command.getName().equalsIgnoreCase("cvcreativeutilsreload")) {
            return reloadParser.execute(sender, args);
        } else if(command.getName().equalsIgnoreCase("mobcount")) {
            mobcountParser.execute(sender, args);
        } else if(command.getName().equalsIgnoreCase("mobtp")) {
            mobtpParser.execute(sender, args);
        } else if(command.getName().equalsIgnoreCase("mobkill")) {
            mobkillParser.execute(sender, args);
        }
        return false;
    }

    private void initConfig() {
        final File dataDir = getDataFolder();
        if(!dataDir.exists()) {
            dataDir.mkdirs();
        }
        File configFile = new File(dataDir, "config.yml");
        if(!configFile.exists()) {
            try {
                configFile.createNewFile();
                final InputStream inputStream = this.getResource(configFile.getName());
                final FileOutputStream fileOutputStream = new FileOutputStream(configFile);
                final byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = Objects.requireNonNull(inputStream).read(buffer)) != -1) {
                    fileOutputStream.write(buffer, 0, bytesRead);
                }
                fileOutputStream.flush();
                fileOutputStream.close();
            } catch(IOException e) {
                this.logger.log(Level.SEVERE, ChatColor.RED + "Unable to generate config file");
                throw new RuntimeException(ChatColor.LIGHT_PURPLE + "Unable to generate config file", e);
            }
        }
        YamlConfiguration mainConfig = new YamlConfiguration();
        try {
            mainConfig.load(configFile);
            this.entitiesPerPlot = mainConfig.getInt("entities-per-plot", 10);
            for(String world : mainConfig.getStringList("controlled-worlds")) {
                if(Bukkit.getWorld(world) != null) this.controlledWorlds.add(Bukkit.getWorld(world));
            }
            for(String entity : mainConfig.getStringList("banned-entities")) {
                try {
                    EntityType type = EntityType.valueOf(entity.toUpperCase());
                    this.bannedEntities.add(type);
                } catch(IllegalArgumentException ignored) { }
            }
        } catch(IOException | InvalidConfigurationException e) {
            this.logger.severe("Unable to load config file!");
            e.printStackTrace();
        }
    }
}
