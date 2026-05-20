package ru.leymooo.antirelog.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import ru.leymooo.antirelog.Antirelog;
import ru.leymooo.antirelog.config.Settings;
import ru.leymooo.antirelog.manager.PvPManager;
import ru.leymooo.antirelog.util.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class AntirelogCommand implements CommandExecutor, TabCompleter {

    private final Antirelog plugin;
    private final PvPManager pvpManager;
    private final Settings settings;

    public AntirelogCommand(Antirelog plugin, PvPManager pvpManager, Settings settings) {
        this.plugin = plugin;
        this.pvpManager = pvpManager;
        this.settings = settings;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                if (sender.hasPermission("antirelog.reload")) {
                    plugin.reloadSettings();
                    sender.sendMessage(Utils.color("&a[AntiRelog] &fКонфигурация перезагружена!"));
                } else {
                    sender.sendMessage(Utils.color("&cУ вас нет прав на выполнение этой команды!"));
                }
                break;

            case "updatepvp":
                if (!sender.hasPermission("antirelog.admin")) {
                    sender.sendMessage(Utils.color("&cУ вас нет прав на выполнение этой команды!"));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(Utils.color("&cИспользование: /antirelog updatepvp <ник>"));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(Utils.color("&cИгрок не найден!"));
                    return true;
                }
                pvpManager.startPvpManually(target, settings.getPvpTime());
                sender.sendMessage(Utils.color("&a[AntiRelog] &fИгроку &e" + target.getName() + " &fвыдан режим боя на &e" + settings.getPvpTime() + " &fсекунд!"));
                break;

            case "updatepvptimer":
                if (!sender.hasPermission("antirelog.admin")) {
                    sender.sendMessage(Utils.color("&cУ вас нет прав на выполнение этой команды!"));
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(Utils.color("&cИспользование: /antirelog updatepvptimer <ник> <сек>"));
                    return true;
                }
                Player targetTimer = Bukkit.getPlayer(args[1]);
                if (targetTimer == null) {
                    sender.sendMessage(Utils.color("&cИгрок не найден!"));
                    return true;
                }
                int seconds;
                try {
                    seconds = Integer.parseInt(args[2]);
                    if (seconds <= 0) {
                        sender.sendMessage(Utils.color("&cВремя должно быть больше 0!"));
                        return true;
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage(Utils.color("&cВремя должно быть числом!"));
                    return true;
                }
                pvpManager.startPvpManually(targetTimer, seconds);
                sender.sendMessage(Utils.color("&a[AntiRelog] &fИгроку &e" + targetTimer.getName() + " &fвыдан режим боя на &e" + seconds + " &fсекунд!"));
                break;

            case "deletepvp":
                if (!sender.hasPermission("antirelog.admin")) {
                    sender.sendMessage(Utils.color("&cУ вас нет прав на выполнение этой команды!"));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(Utils.color("&cИспользование: /antirelog deletepvp <ник>"));
                    return true;
                }
                Player targetDelete = Bukkit.getPlayer(args[1]);
                if (targetDelete == null) {
                    sender.sendMessage(Utils.color("&cИгрок не найден!"));
                    return true;
                }
                if (pvpManager.isInPvP(targetDelete)) {
                    pvpManager.stopPvP(targetDelete);
                    sender.sendMessage(Utils.color("&a[AntiRelog] &fРежим боя снят с игрока &e" + targetDelete.getName()));
                } else {
                    sender.sendMessage(Utils.color("&cИгрок &e" + targetDelete.getName() + " &cне находится в режиме боя!"));
                }
                break;

            case "status":
                if (!sender.hasPermission("antirelog.status")) {
                    sender.sendMessage(Utils.color("&cУ вас нет прав на выполнение этой команды!"));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(Utils.color("&cИспользование: /antirelog status <ник>"));
                    return true;
                }
                Player targetStatus = Bukkit.getPlayer(args[1]);
                if (targetStatus == null) {
                    sender.sendMessage(Utils.color("&cИгрок не найден!"));
                    return true;
                }
                showPlayerStatus(sender, targetStatus);
                break;

            default:
                sendHelp(sender);
                break;
        }
        return true;
    }

    private void showPlayerStatus(CommandSender sender, Player player) {
        sender.sendMessage(Utils.color("&6&m----------------------------------------"));
        sender.sendMessage(Utils.color("&6Статистика игрока &e" + player.getName()));
        sender.sendMessage(Utils.color("&6&m----------------------------------------"));

        boolean inPvp = pvpManager.isInPvP(player);
        boolean inSilentPvp = pvpManager.isInSilentPvP(player);

        if (inPvp) {
            int time = pvpManager.getTimeRemainingInPvP(player);
            sender.sendMessage(Utils.color("&aСостояние: &cВ режиме боя"));
            sender.sendMessage(Utils.color("&aОсталось времени: &e" + time + " &aсекунд"));
            sender.sendMessage(Utils.color("&aПротивники: &f" + getOpponents(player)));
        } else if (inSilentPvp) {
            int time = pvpManager.getTimeRemainingInPvPSilent(player);
            sender.sendMessage(Utils.color("&aСостояние: &eСкрытый режим боя"));
            sender.sendMessage(Utils.color("&aОсталось времени: &e" + time + " &aсекунд"));
        } else {
            sender.sendMessage(Utils.color("&aСостояние: &aНе в бою"));
        }

    }

    private String getOpponents(Player player) {
        Set<Player> opponents = pvpManager.getOpponents(player);
        StringBuilder result = new StringBuilder();

        if (opponents.isEmpty()) {
            return "нет данных";
        }

        for (Player opponent : opponents) {
            if (result.length() > 0) {
                result.append(", ");
            }
            result.append(opponent.getName());
        }
        return result.toString();
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Utils.color("&x&F&B&6&9&0&C&l╔ &x&F&A&6&D&0&E&lS&x&F&A&7&2&1&0&lt&x&F&9&7&6&1&3&la&x&F&8&7&B&1&5&lr&x&F&7&7&F&1&7&ls&x&F&7&8&3&1&9&lA&x&F&6&8&8&1&B&ln&x&F&5&8&C&1&E&lt&x&F&4&9&1&2&0&li&x&F&4&9&5&2&2&lR&x&F&3&9&A&2&4&le&x&F&2&9&E&2&6&ll&x&F&1&A&2&2&9&lo&x&F&1&A&7&2&B&lg&x&F&0&A&B&2&D&lF&x&E&F&B&0&2&F&lo&x&E&F&B&4&3&2&lr&x&E&E&B&8&3&4&lk &x&E&D&B&D&3&6&l- &x&E&C&C&1&3&8&lК&x&E&C&C&6&3&A&lо&x&E&B&C&A&3&D&lм&x&E&A&C&E&3&F&lа&x&E&9&D&3&4&1&lн&x&E&9&D&7&4&3&lд&x&E&8&D&C&4&5&lы &x&E&7&E&0&4&8&lп&x&E&6&E&5&4&A&lл&x&E&6&E&9&4&C&lа&x&E&5&E&D&4&E&lг&x&E&4&F&2&5&0&lи&x&E&3&F&6&5&3&lн&x&E&3&F&B&5&5&lа&x&E&2&F&F&5&7&l╗"));
        sender.sendMessage(Utils.color("&x&F&B&6&9&0&C&l╚  &x&F&A&6&E&0&E&lП&x&F&9&7&3&1&1&lе&x&F&9&7&8&1&3&lр&x&F&8&7&C&1&6&lе&x&F&7&8&1&1&8&lп&x&F&6&8&6&1&B&lи&x&F&5&8&B&1&D&lс&x&F&5&9&0&1&F&lа&x&F&4&9&5&2&2&lн&x&F&3&9&9&2&4&lо &x&F&2&9&E&2&7&lс&x&F&1&A&3&2&9&lт&x&F&1&A&8&2&B&lу&x&F&0&A&D&2&E&lд&x&E&F&B&2&3&0&lи&x&E&E&B&6&3&3&lе&x&E&D&B&B&3&5&lй &x&E&C&C&0&3&8&lt&x&E&C&C&5&3&A&l.&x&E&B&C&A&3&C&lm&x&E&A&C&F&3&F&le&x&E&9&D&3&4&1&l/&x&E&8&D&8&4&4&lS&x&E&8&D&D&4&6&lt&x&E&7&E&2&4&8&la&x&E&6&E&7&4&B&lr&x&E&5&E&C&4&D&ls&x&E&4&F&0&5&0&lD&x&E&4&F&5&5&2&le&x&E&3&F&A&5&5&lv   &x&E&2&F&F&5&7&l╝"));
        sender.sendMessage(Utils.color("&f"));
        sender.sendMessage(Utils.color("&x&F&B&6&9&0&C&l/&x&F&9&7&3&1&1&la&x&F&8&7&D&1&6&ln&x&F&6&8&7&1&B&lt&x&F&4&9&1&2&0&li&x&F&3&9&B&2&5&lr&x&F&1&A&5&2&A&le&x&E&F&A&F&2&F&ll&x&E&E&B&9&3&4&lo&x&E&C&C&3&3&9&lg &x&E&A&C&D&3&E&lr&x&E&9&D&7&4&3&le&x&E&7&E&1&4&8&ll&x&E&5&E&B&4&D&lo&x&E&4&F&5&5&2&la&x&E&2&F&F&5&7&ld &f- Перезагрузить конфиг"));
        if (sender.hasPermission("antirelog.admin")) {
            sender.sendMessage(Utils.color("&x&F&B&6&9&0&C&l/&x&F&A&7&0&0&F&la&x&F&9&7&6&1&3&ln&x&F&8&7&D&1&6&lt&x&F&7&8&3&1&9&li&x&F&6&8&A&1&C&lr&x&F&4&9&0&2&0&le&x&F&3&9&7&2&3&ll&x&F&2&9&D&2&6&lo&x&F&1&A&4&2&9&lg &x&F&0&A&A&2&D&lu&x&E&F&B&1&3&0&lp&x&E&E&B&7&3&3&ld&x&E&D&B&E&3&6&la&x&E&C&C&4&3&A&lt&x&E&B&C&B&3&D&le&x&E&A&D&1&4&0&lp&x&E&9&D&8&4&3&lv&x&E&7&D&E&4&7&lp &x&E&6&E&5&4&A&l<&x&E&5&E&B&4&D&lн&x&E&4&F&2&5&0&lи&x&E&3&F&8&5&4&lк&x&E&2&F&F&5&7&l> &f- Выдать режим боя игроку"));
            sender.sendMessage(Utils.color("&x&F&B&6&9&0&C&l/&x&F&A&6&E&0&E&la&x&F&9&7&2&1&1&ln&x&F&9&7&7&1&3&lt&x&F&8&7&B&1&5&li&x&F&7&8&0&1&7&lr&x&F&6&8&4&1&A&le&x&F&6&8&9&1&C&ll&x&F&5&8&D&1&E&lo&x&F&4&9&2&2&0&lg &x&F&3&9&6&2&3&lu&x&F&3&9&B&2&5&lp&x&F&2&A&0&2&7&ld&x&F&1&A&4&2&A&la&x&F&0&A&9&2&C&lt&x&F&0&A&D&2&E&le&x&E&F&B&2&3&0&lp&x&E&E&B&6&3&3&lv&x&E&D&B&B&3&5&lp&x&E&D&B&F&3&7&lt&x&E&C&C&4&3&9&li&x&E&B&C&8&3&C&lm&x&E&A&C&D&3&E&le&x&E&A&D&2&4&0&lr &x&E&9&D&6&4&3&l<&x&E&8&D&B&4&5&lн&x&E&7&D&F&4&7&lи&x&E&7&E&4&4&9&lк&x&E&6&E&8&4&C&l> &x&E&5&E&D&4&E&l<&x&E&4&F&1&5&0&lс&x&E&4&F&6&5&2&lе&x&E&3&F&A&5&5&lк&x&E&2&F&F&5&7&l> &f- Выдать конкретное время"));
            sender.sendMessage(Utils.color("&x&F&B&6&9&0&C&l/&x&F&A&7&0&0&F&la&x&F&9&7&6&1&3&ln&x&F&8&7&D&1&6&lt&x&F&7&8&3&1&9&li&x&F&6&8&A&1&C&lr&x&F&4&9&0&2&0&le&x&F&3&9&7&2&3&ll&x&F&2&9&D&2&6&lo&x&F&1&A&4&2&9&lg &x&F&0&A&A&2&D&ld&x&E&F&B&1&3&0&le&x&E&E&B&7&3&3&ll&x&E&D&B&E&3&6&le&x&E&C&C&4&3&A&lt&x&E&B&C&B&3&D&le&x&E&A&D&1&4&0&lp&x&E&9&D&8&4&3&lv&x&E&7&D&E&4&7&lp &x&E&6&E&5&4&A&l<&x&E&5&E&B&4&D&lн&x&E&4&F&2&5&0&lи&x&E&3&F&8&5&4&lк&x&E&2&F&F&5&7&l> &f- Снять режим боя игроку"));
        }
        if (sender.hasPermission("antirelog.admin")) {
            sender.sendMessage(Utils.color("&x&F&B&6&9&0&C&l/&x&F&A&7&1&1&0&la&x&F&9&7&8&1&4&ln&x&F&7&8&0&1&7&lt&x&F&6&8&7&1&B&li&x&F&5&8&F&1&F&lr&x&F&4&9&6&2&3&le&x&F&2&9&E&2&6&ll&x&F&1&A&5&2&A&lo&x&F&0&A&D&2&E&lg &x&E&F&B&4&3&2&ls&x&E&D&B&C&3&5&lt&x&E&C&C&3&3&9&la&x&E&B&C&B&3&D&lt&x&E&A&D&2&4&1&lu&x&E&8&D&A&4&4&ls &x&E&7&E&1&4&8&l<&x&E&6&E&9&4&C&lн&x&E&5&F&0&5&0&lи&x&E&3&F&8&5&3&lк&x&E&2&F&F&5&7&l> &f- Показать статус игрока"));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> commands = new ArrayList<>();
            if (sender.hasPermission("antirelog.reload")) {
                commands.add("reload");
            }
            if (sender.hasPermission("antirelog.admin")) {
                commands.add("updatepvp");
                commands.add("updatepvptimer");
                commands.add("deletepvp");
            }
            if (sender.hasPermission("antirelog.status")) {
                commands.add("status");
            }

            for (String cmd : commands) {
                if (cmd.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(cmd);
                }
            }
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("updatepvp") || args[0].equalsIgnoreCase("updatepvptimer") ||
                    args[0].equalsIgnoreCase("deletepvp") || args[0].equalsIgnoreCase("status")) {

                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(player.getName());
                    }
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("updatepvptimer")) {
            completions.add("10");
            completions.add("30");
            completions.add("60");
            completions.add("120");
            completions.add("300");
            completions.add("600");
        }

        return completions;
    }
}