package com.tuserver.daytotems;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class DayTotems extends JavaPlugin {

    private final Map<UUID, BukkitTask> activeTasks = new HashMap<>();
    private final Map<UUID, String> selectedDifficulty = new HashMap<>();

    private static final ItemStack PANEL = buildItem(Material.GRAY_STAINED_GLASS_PANE, " ", null);

    @Override
    public void onEnable() {
        getCommand("daytotems").setExecutor(this);
        Bukkit.getPluginManager().registerEvents(new MenuListener(this), this);
        getLogger().info("DayTotems habilitado.");
    }

    @Override
    public void onDisable() {
        activeTasks.values().forEach(BukkitTask::cancel);
        activeTasks.clear();
    }

    public Map<UUID, String> getSelectedDifficulty() {
        return selectedDifficulty;
    }

    public Map<UUID, BukkitTask> getActiveTasks() {
        return activeTasks;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Este comando solo puede usarse en el juego.");
            return true;
        }
        Player player = (Player) sender;

        if (args.length > 0 && args[0].equalsIgnoreCase("stop")) {
            BukkitTask task = activeTasks.remove(player.getUniqueId());
            if (task != null) {
                task.cancel();
                player.sendMessage(ChatColor.RED + "Practica de totems detenida.");
            } else {
                player.sendMessage(ChatColor.GRAY + "No tenias ninguna practica activa.");
            }
            return true;
        }

        player.openInventory(createDifficultyMenu());
        return true;
    }

    /** Menu de 54 slots con borde de paneles grises y 3 opciones centradas. */
    public Inventory createDifficultyMenu() {
        Inventory inv = Bukkit.createInventory(null, 54, ChatColor.DARK_RED + "Practica de Totems - Dificultad");

        fillBorder(inv);

        // Fila central (index 2 => slots 18-26), columnas 2, 4 y 6 centradas
        inv.setItem(20, buildItem(Material.LIME_WOOL, ChatColor.GREEN + "Facil",
                Arrays.asList(ChatColor.GRAY + "El inventario se mantiene",
                        ChatColor.GRAY + "siempre lleno de totems.")));

        inv.setItem(22, buildItem(Material.YELLOW_WOOL, ChatColor.YELLOW + "Avanzado",
                Arrays.asList(ChatColor.GRAY + "Solo la hotbar se mantiene",
                        ChatColor.GRAY + "cargada de totems.")));

        inv.setItem(24, buildItem(Material.RED_WOOL, ChatColor.RED + "Dificil",
                Arrays.asList(ChatColor.GRAY + "Un unico totem disponible",
                        ChatColor.GRAY + "en todo momento.")));

        return inv;
    }

    /** Menu de 54 slots con borde de paneles grises y 5 opciones de ticks centradas. */
    public Inventory createTicksMenu(String difficulty) {
        Inventory inv = Bukkit.createInventory(null, 54,
                ChatColor.DARK_AQUA + "Practica de Totems - Ticks (" + difficulty + ")");

        fillBorder(inv);

        int[] ticks = {5, 10, 15, 20, 25};
        int[] slots = {19, 21, 22, 23, 25}; // fila central, distribuidos y centrados

        for (int i = 0; i < ticks.length; i++) {
            inv.setItem(slots[i], buildItem(Material.TOTEM_OF_UNDYING, ChatColor.AQUA + "Cada " + ticks[i] + " ticks",
                    Collections.singletonList(ChatColor.GRAY + "Click para comenzar la practica")));
        }

        // Botón volver, esquina inferior izquierda del área útil
        inv.setItem(45, buildItem(Material.ARROW, ChatColor.GRAY + "« Volver", null));

        return inv;
    }

    /** Rellena los bordes (fila superior, inferior y columnas laterales) con paneles grises. */
    private void fillBorder(Inventory inv) {
        int size = inv.getSize();
        int rows = size / 9;

        for (int slot = 0; slot < size; slot++) {
            int row = slot / 9;
            int col = slot % 9;
            boolean isBorder = row == 0 || row == rows - 1 || col == 0 || col == 8;
            if (isBorder) {
                inv.setItem(slot, PANEL);
            }
        }
    }

    public static ItemStack buildItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        if (lore != null) {
            meta.setLore(lore);
        }
        item.setItemMeta(meta);
        return item;
    }
  }
