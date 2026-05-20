package ru.leymooo.antirelog.manager;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import ru.leymooo.antirelog.config.Settings;
import ru.leymooo.antirelog.util.Utils;
import ru.leymooo.antirelog.util.VersionUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BossbarManager {

    private final Map<UUID, BossBar> playerBossBars = new HashMap<>();
    private final Settings settings;

    public BossbarManager(Settings settings) {
        this.settings = settings;
    }

    public void createBossBars() {
    }

    public void setBossBar(Player player, int time) {
        if (!VersionUtils.isVersion(9)) {
            return;
        }

        String title = Utils.color(settings.getMessages().getInPvpBossbar());
        if (title.isEmpty()) {
            return;
        }

        BossBar existingBar = playerBossBars.get(player.getUniqueId());
        if (existingBar != null) {
            existingBar.removePlayer(player);
            existingBar.setVisible(false);
        }

        String actualTitle = Utils.replaceTime(title, time);
        double progress = Math.min(1.0, Math.max(0.0, (double) time / (double) time));

        BossBar bar = Bukkit.createBossBar(actualTitle, BarColor.RED, BarStyle.SOLID);
        bar.setProgress(progress);
        bar.addPlayer(player);

        playerBossBars.put(player.getUniqueId(), bar);
    }

    public void updateBossBar(Player player, int time) {
        if (!VersionUtils.isVersion(9)) {
            return;
        }

        BossBar bar = playerBossBars.get(player.getUniqueId());
        if (bar == null) {
            setBossBar(player, time);
            return;
        }

        String title = Utils.color(settings.getMessages().getInPvpBossbar());
        if (title.isEmpty()) {
            return;
        }

        double progress;
        int maxTime = settings.getPvpTime();
        if (maxTime > 0 && time <= maxTime) {
            progress = (double) time / (double) maxTime;
        } else {
            progress = 1.0;
        }

        progress = Math.min(1.0, Math.max(0.0, progress));

        String actualTitle = Utils.replaceTime(title, time);
        bar.setTitle(actualTitle);
        bar.setProgress(progress);
    }

    public void clearBossbar(Player player) {
        BossBar bar = playerBossBars.remove(player.getUniqueId());
        if (bar != null) {
            bar.removePlayer(player);
            bar.setVisible(false);
        }
    }

    public void clearBossbars() {
        for (BossBar bar : playerBossBars.values()) {
            bar.removeAll();
            bar.setVisible(false);
        }
        playerBossBars.clear();
    }
}