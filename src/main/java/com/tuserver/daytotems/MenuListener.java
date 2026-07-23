package com.tuserver.daytotems;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;

public class MenuListener implements Listener {

    private static final String DIFFICULTY_PREFIX = ChatColor.DARK_RED + "Practica de Totems - Dificultad";
    private static final String TICKS_PREFIX = ChatColor.DARK_AQUA + "Practica de Totems - Ticks";

    private final DayTotems plugin;

    public MenuListener(DayTotems plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        boolean isDifficultyMenu = title.equals(DIFFICULTY_PREFIX);
        boolean isTicksMenu = title.startsWith(TICKS_PREFIX);

        if (!isDifficultyMenu && !isTicksMenu) return;

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta() || clicked.getType() == Material.GRAY_STAINED_GLASS_PANE) return;

        String displayName = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());

        if (isDifficultyMenu) {
            if (clicked.getType() == Material.ARROW) return;
            plugin.getSelectedDifficulty().put(player.getUniqueId(), displayName);
            player.openInventory(plugin.createTicksMenu(displayName));
            return;
        }

        // Menu de ticks
        if (clicked.getType() == Material.ARROW) {
            player.openInventory(plugin.createDifficultyMenu());
            return;
        }

        int ticks = extractTicks(displayName);
        String difficulty = plugin.getSelectedDifficulty().getOrDefault(player.getUniqueId(), "Facil");
        startPractice(player, difficulty, ticks);
        player.closeInventory();
    }

    private int extractTicks(String name) {
        try {
            return Integer.parseInt(name.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return 20;
        }
    }

    private void startPractice(Player player, String difficulty, int ticks) {
        UUID id = player.getUniqueId();

        BukkitTask previous = plugin.getActiveTasks().remove(id);
        if (previous != null) previous.cancel();

        fillTotems(player, difficulty);
        player.sendMessage(ChatColor.GREEN + "Practica iniciada: " + ChatColor.YELLOW + difficulty +
                ChatColor.GREEN + " cada " + ChatColor.YELLOW + ticks + ChatColor.GREEN + " ticks.");
        player.sendMessage(ChatColor.GRAY + "Usa /daytotems stop para detenerla.");

        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) {
                BukkitTask t = plugin.getActiveTasks().remove(id);
                if (t != null) t.cancel();
                return;
            }
            fillTotems(player, difficulty);
        }, ticks, ticks);

        plugin.getActiveTasks().put(id, task);
    }

    private void fillTotems(Player player, String difficulty) {
        PlayerInventory inv = player.getInventory();

        switch (difficulty) {
            case "Facil":
                for (int i = 0; i
