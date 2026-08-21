package com.carpetplayers.waypoint;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class WaypointCommands implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("carpetplayers.admin")) {
            sender.sendMessage("\u00a7cKamu tidak punya akses ke perintah ini.");
            return true;
        }
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "add":
                return cmdAdd(sender, args);
            case "remove":
                return cmdRemove(sender, args);
            case "list":
                return cmdList(sender);
            case "color":
                return cmdColor(sender, args);
            case "enable":
                return cmdEnable(sender, args, true);
            case "disable":
                return cmdEnable(sender, args, false);
            case "tp":
                return cmdTeleport(sender, args);
            case "here":
                return cmdHere(sender, args);
            default:
                sendHelp(sender);
                return true;
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("\u00a76=== Carpet Players Waypoint ===");
        sender.sendMessage("\u00a7f/cp waypoint add <nama> [warna] - Tambah waypoint di posisi");
        sender.sendMessage("\u00a7f/cp waypoint remove <nama> - Hapus waypoint");
        sender.sendMessage("\u00a7f/cp waypoint list - Lihat semua waypoint");
        sender.sendMessage("\u00a7f/cp waypoint color <nama> <warna> - Ubah warna waypoint");
        sender.sendMessage("\u00a7f/cp waypoint enable <nama> - Aktifkan waypoint");
        sender.sendMessage("\u00a7f/cp waypoint disable <nama> - Nonaktifkan waypoint");
        sender.sendMessage("\u00a7f/cp waypoint tp <nama> - Teleport ke waypoint");
        sender.sendMessage("\u00a7f/cp waypoint here <nama> - Tampilkan lokasi waypoint");
    }

    private boolean cmdAdd(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("\u00a7cPerintah ini hanya untuk pemain.");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage("\u00a7c/cp waypoint add <nama> [warna]");
            return true;
        }
        Player player = (Player) sender;
        String name = args[1];
        String color = args.length >= 3 ? args[2].toLowerCase() : "white";
        if (!Waypoint.isValidColor(color)) {
            sender.sendMessage("\u00a7cWarna tidak valid. Pilihan: \u00a7f" + String.join(", ", Waypoint.validColors()));
            return true;
        }
        if (WaypointManager.addWaypoint(player.getUniqueId(), name, player.getLocation(), color)) {
            Location loc = player.getLocation();
            sender.sendMessage("\u00a7aWaypoint '" + name + "' ditambahkan di " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
        } else {
            sender.sendMessage("\u00a7cWaypoint dengan nama '" + name + "' sudah ada.");
        }
        return true;
    }

    private boolean cmdRemove(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("\u00a7cPerintah ini hanya untuk pemain.");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage("\u00a7c/cp waypoint remove <nama>");
            return true;
        }
        Player player = (Player) sender;
        if (WaypointManager.removeWaypoint(player.getUniqueId(), args[1])) {
            sender.sendMessage("\u00a7aWaypoint '" + args[1] + "' dihapus.");
        } else {
            sender.sendMessage("\u00a7cWaypoint '" + args[1] + "' tidak ditemukan.");
        }
        return true;
    }

    private boolean cmdList(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("\u00a7cPerintah ini hanya untuk pemain.");
            return true;
        }
        Player player = (Player) sender;
        List<Waypoint> waypoints = WaypointManager.getWaypoints(player.getUniqueId());
        if (waypoints.isEmpty()) {
            sender.sendMessage("\u00a7eTidak ada waypoint.");
            return true;
        }
        sender.sendMessage("\u00a76=== Waypoints (" + waypoints.size() + ") ===");
        for (int i = 0; i < waypoints.size(); i++) {
            Waypoint w = waypoints.get(i);
            String status = w.enabled ? "\u00a7a\u2713" : "\u00a7c\u2717";
            String colorCode = Waypoint.resolveColor(w.color);
            sender.sendMessage(status + " \u00a7f" + (i + 1) + ". " + colorCode + w.name + " \u00a77- " + w.coordString() + " [" + w.world + "]");
        }
        return true;
    }

    private boolean cmdColor(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("\u00a7cPerintah ini hanya untuk pemain.");
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage("\u00a7c/cp waypoint color <nama> <warna>");
            sender.sendMessage("\u00a77Warna: " + String.join(", ", Waypoint.validColors()));
            return true;
        }
        Player player = (Player) sender;
        if (!Waypoint.isValidColor(args[2].toLowerCase())) {
            sender.sendMessage("\u00a7cWarna tidak valid.");
            return true;
        }
        if (WaypointManager.setColor(player.getUniqueId(), args[1], args[2].toLowerCase())) {
            sender.sendMessage("\u00a7aWarna waypoint '" + args[1] + "' diubah ke " + args[2]);
        } else {
            sender.sendMessage("\u00a7cWaypoint '" + args[1] + "' tidak ditemukan.");
        }
        return true;
    }

    private boolean cmdEnable(CommandSender sender, String[] args, boolean enabled) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("\u00a7cPerintah ini hanya untuk pemain.");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage("\u00a7c/cp waypoint " + (enabled ? "enable" : "disable") + " <nama>");
            return true;
        }
        Player player = (Player) sender;
        if (WaypointManager.setEnabled(player.getUniqueId(), args[1], enabled)) {
            sender.sendMessage("\u00a7aWaypoint '" + args[1] + "' " + (enabled ? "diaktifkan" : "dinonaktifkan") + ".");
        } else {
            sender.sendMessage("\u00a7cWaypoint '" + args[1] + "' tidak ditemukan.");
        }
        return true;
    }

    private boolean cmdTeleport(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("\u00a7cPerintah ini hanya untuk pemain.");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage("\u00a7c/cp waypoint tp <nama>");
            return true;
        }
        Player player = (Player) sender;
        Waypoint w = WaypointManager.findWaypoint(player.getUniqueId(), args[1]);
        if (w == null) {
            sender.sendMessage("\u00a7cWaypoint '" + args[1] + "' tidak ditemukan.");
            return true;
        }
        World world = org.bukkit.Bukkit.getWorld(w.world);
        if (world == null) {
            sender.sendMessage("\u00a7cDunia '" + w.world + "' tidak ditemukan.");
            return true;
        }
        Location loc = new Location(world, w.x + 0.5, w.y + 1, w.z + 0.5);
        player.teleport(loc);
        sender.sendMessage("\u00a7aTeleport ke waypoint '" + w.name + "'.");
        return true;
    }

    private boolean cmdHere(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("\u00a7cPerintah ini hanya untuk pemain.");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage("\u00a7c/cp waypoint here <nama>");
            return true;
        }
        Player player = (Player) sender;
        Waypoint w = WaypointManager.findWaypoint(player.getUniqueId(), args[1]);
        if (w == null) {
            sender.sendMessage("\u00a7cWaypoint '" + args[1] + "' tidak ditemukan.");
            return true;
        }
        String colorCode = Waypoint.resolveColor(w.color);
        sender.sendMessage(colorCode + w.name + " \u00a77di " + w.coordString() + " [" + w.world + "]");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();
        if (args.length == 1) {
            suggestions.addAll(Arrays.asList("add", "remove", "list", "color", "enable", "disable", "tp", "here"));
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "remove":
                case "color":
                case "enable":
                case "disable":
                case "tp":
                case "here":
                    if (sender instanceof Player) {
                        UUID uuid = ((Player) sender).getUniqueId();
                        suggestions.addAll(WaypointManager.getWaypoints(uuid).stream()
                                .map(w -> w.name).collect(Collectors.toList()));
                    }
                    break;
                default:
                    break;
            }
        } else if (args.length == 3 && "color".equalsIgnoreCase(args[0])) {
            suggestions.addAll(Waypoint.validColors());
        }
        return suggestions;
    }
}
