package ru.leymooo.antirelog;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.codemc.worldguardwrapper.WorldGuardWrapper;
import ru.leymooo.annotatedyaml.Configuration;
import ru.leymooo.annotatedyaml.ConfigurationProvider;
import ru.leymooo.annotatedyaml.provider.BukkitConfigurationProvider;
import ru.leymooo.antirelog.commands.AntirelogCommand;
import ru.leymooo.antirelog.config.Settings;
import ru.leymooo.antirelog.listeners.CooldownListener;
import ru.leymooo.antirelog.listeners.EssentialsTeleportListener;
import ru.leymooo.antirelog.listeners.PvPListener;
import ru.leymooo.antirelog.listeners.WorldGuardListener;
import ru.leymooo.antirelog.manager.BossbarManager;
import ru.leymooo.antirelog.manager.CooldownManager;
import ru.leymooo.antirelog.manager.PowerUpsManager;
import ru.leymooo.antirelog.manager.PvPManager;
import ru.leymooo.antirelog.util.ProtocolLibUtils;
import ru.leymooo.antirelog.util.VersionUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Stream;

public class Antirelog extends JavaPlugin {
    private Settings settings;
    private PvPManager pvpManager;
    private CooldownManager cooldownManager;
    private boolean protocolLib;
    private boolean worldguard;

    @Override
    public void onEnable() {
        loadConfig();
        pvpManager = new PvPManager(settings, this);
        detectPlugins();
        cooldownManager = new CooldownManager(this, settings);

        if (protocolLib) {
            ProtocolLibUtils.createListener(cooldownManager, pvpManager, this);
        }

        getServer().getPluginManager().registerEvents(new PvPListener(this, pvpManager, settings), this);
        getServer().getPluginManager().registerEvents(new CooldownListener(this, cooldownManager, pvpManager, settings), this);

        if (getCommand("antirelog") != null) {
            AntirelogCommand commandExecutor = new AntirelogCommand(this, pvpManager, settings);
            getCommand("antirelog").setExecutor(commandExecutor);
            getCommand("antirelog").setTabCompleter(commandExecutor);
            getLogger().info("Команда /antirelog успешно зарегистрирована");
        } else {
            getLogger().warning("Не удалось зарегистрировать команду /antirelog! Проверьте plugin.yml");
        }

        getLogger().info("╔════════════════════════════════════╗");
        getLogger().info("║      ⭐  StarsAntiRelogFork ⭐       ║");
        getLogger().info("║ Переписано студией t.me/StarsDev   ║");
        getLogger().info("║ StarsAntiRelogFork успешно включен!║");
        getLogger().info("╚════════════════════════════════════╝");
    }

    @Override
    public void onDisable() {
        if (pvpManager != null) {
            pvpManager.onPluginDisable();
        }
        if (cooldownManager != null) {
            cooldownManager.clearAll();
        }
        getLogger().info("╔════════════════════════════════════╗");
        getLogger().info("║      ⭐  StarsAntiRelogFork ⭐       ║");
        getLogger().info("║ Переписано студией t.me/StarsDev   ║");
        getLogger().info("║ StarsAntiRelogFork успешно выключен║");
        getLogger().info("╚════════════════════════════════════╝");
    }

    private void loadConfig() {
        fixFolder();
        settings = Configuration.builder(Settings.class)
                .file(new File(getDataFolder(), "config.yml"))
                .provider(BukkitConfigurationProvider.class).build();
        ConfigurationProvider provider = settings.getConfigurationProvider();
        provider.reloadFileFromDisk();
        File file = provider.getConfigFile();

        if (file.exists() && provider.get("config-version") == null) {
            try {
                Files.move(file.toPath(), new File(file.getParentFile(), "config.old." + System.nanoTime()).toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                e.printStackTrace();
            }
            provider.reloadFileFromDisk();
        }

        if (!file.exists()) {
            settings.save();
            settings.loaded();
            getLogger().info("config.yml успешно создан");
        } else if (provider.isFileSuccessfullyLoaded()) {
            if (settings.load()) {
                if (!((String) provider.get("config-version")).equals(settings.getConfigVersion())) {
                    getLogger().info("Конфиг был обновлен. Проверьте новые значения");
                    settings.save();
                }
                getLogger().info("Конфиг успешно загружен");
            } else {
                getLogger().warning("Не удалось загрузить конфиг");
                settings.loaded();
            }
        } else {
            getLogger().warning("Не удалось загрузить настройки из файла, используются стандартные...");
        }
    }

    private void fixFolder() {
        File oldFolder = new File(getDataFolder().getParentFile(), "Antirelog");
        if (!oldFolder.exists()) {
            return;
        }

        try {
            File actualFolder = oldFolder.getCanonicalFile();
            if (actualFolder.getName().equals("Antirelog")) {
                File oldConfig = new File(actualFolder, "config.yml");
                if (!oldConfig.exists()) {
                    deleteFolder(actualFolder.toPath());
                    return;
                }
                List<String> oldConfigLines = Files.readAllLines(oldConfig.toPath(), StandardCharsets.UTF_8);
                String firstLine = oldConfigLines.size() > 0 ? oldConfigLines.get(0) : null;
                deleteFolder(actualFolder.toPath());

                File newFolder = getDataFolder();
                if (!newFolder.exists()) {
                    newFolder.mkdir();
                }
                File oldConfigInNewFolder = new File(newFolder, "config.yml");

                if (firstLine != null && firstLine.startsWith("config-version")) {
                    if (oldConfigInNewFolder.exists()) {
                        Files.move(oldConfigInNewFolder.toPath(), new File(oldConfigInNewFolder.getParentFile(),
                                "config.old." + System.nanoTime()).toPath());
                    }
                    Files.write(oldConfigInNewFolder.toPath(), oldConfigLines, StandardCharsets.UTF_8, StandardOpenOption.CREATE);
                    getLogger().log(Level.WARNING, "Старый файл config.yml из папки 'Antirelog' был перемещен в папку 'AntiRelog'");
                } else {
                    Files.write(new File(oldConfigInNewFolder.getParentFile(), "config.old." + System.nanoTime()).toPath(),
                            oldConfigLines, StandardCharsets.UTF_8, StandardOpenOption.CREATE);
                    getLogger().log(Level.WARNING, "Старый файл config.yml из папки 'Antirelog' был перемещен в папку 'AntiRelog' с другим именем");
                }
            }
        } catch (IOException e) {
            getLogger().log(Level.WARNING, "Произошла ошибка при переименовании папки Antirelog -> AntiRelog", e);
        }
    }

    private void deleteFolder(Path folder) throws IOException {
        try (Stream<Path> walk = Files.walk(folder)) {
            walk.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }

    public void reloadSettings() {
        settings.getConfigurationProvider().reloadFileFromDisk();
        if (settings.getConfigurationProvider().isFileSuccessfullyLoaded()) {
            settings.load();
        }
        getServer().getScheduler().cancelTasks(this);
        pvpManager.onPluginDisable();
        pvpManager.onPluginEnable();
        cooldownManager.clearAll();
        getLogger().info("Конфигурация перезагружена!");
    }

    public boolean isProtocolLibEnabled() {
        return protocolLib;
    }

    public boolean isWorldguardEnabled() {
        return worldguard;
    }

    private void detectPlugins() {
        if (Bukkit.getPluginManager().isPluginEnabled("WorldGuard")) {
            WorldGuardWrapper.getInstance().registerEvents(this);
            Bukkit.getPluginManager().registerEvents(new WorldGuardListener(settings, pvpManager), this);
            worldguard = true;
            getLogger().info("Найден WorldGuard! Поддержка регионов включена.");
        }

        try {
            Class.forName("net.ess3.api.events.teleport.PreTeleportEvent");
            Bukkit.getPluginManager().registerEvents(new EssentialsTeleportListener(pvpManager, settings), this);
            getLogger().info("Найден Essentials! Поддержка телепортов включена.");
        } catch (ClassNotFoundException e) {
        }

        protocolLib = Bukkit.getPluginManager().isPluginEnabled("ProtocolLib") && VersionUtils.isVersion(9);
        if (protocolLib) {
            getLogger().info("Найден ProtocolLib! Поддержка кулдаунов предметов включена.");
        }
    }

    public Settings getSettings() {
        return settings;
    }

    public PvPManager getPvpManager() {
        return pvpManager;
    }

    public PowerUpsManager getPowerUpsManager() {
        return pvpManager.getPowerUpsManager();
    }

    public BossbarManager getBossbarManager() {
        return pvpManager.getBossbarManager();
    }

    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }
}