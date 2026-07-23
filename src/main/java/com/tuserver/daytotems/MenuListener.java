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
    private static final String TOP_MENU_TITLE = ChatColor.GOLD + "Top 3 Popeadores de Totems";

    private final DayTotems plugin;

    public MenuListener(DayTotems plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        boolean isDifficultyMenu = title.equals(DIFFICULTY_PREFIX);
        boolean isTicksMenu = title.startsWith(TICKS_PREFIX);
        boolean isTopMenu = title.equals(TOP_MENU_TITLE);

        if (!isDifficultyMenu && !isTicksMenu && !isTopMenu) return;

        // Cancelamos siempre: ninguno de estos menus deja sacar ni mover items.
        event.setCancelled(true);

        if (isTopMenu) {
            // Es solo informativo, no hay nada que procesar al clickear.
            return;
        }

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

        restockAll(player, difficulty);
        player.sendMessage(ChatColor.GREEN + "Practica iniciada: " + ChatColor.YELLOW + difficulty +
                ChatColor.GREEN + " cada " + ChatColor.YELLOW + ticks + ChatColor.GREEN + " ticks.");
        player.sendMessage(ChatColor.GRAY + "Reponete los totems vos mismo. Usa /daytotems stop para detenerla.");

        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) {
                BukkitTask t = plugin.getActiveTasks().remove(id);
                if (t != null) t.cancel();
                return;
            }

            // Golpe letal: si el jugador tiene un totem en la mano u offhand, se activa solo.
            // Si no tiene ninguno equipado, el golpe lo mata de verdad: hay que reponerselo a tiempo.
            player.damage(1000.0);

            // Recien reabastecemos todo si se quedo sin NINGUN totem en inventario, hotbar y offhand.
            if (contarTotemsTotales(player) == 0) {
                restockAll(player, difficulty);
            }
        }, ticks, ticks);

        plugin.getActiveTasks().put(id, task);
    }

    /** Suma los totems que tiene el jugador en su inventario/hotbar (36 slots) y en la offhand. */
    private int contarTotemsTotales(Player player) {
        PlayerInventory inv = player.getInventory();
        int total = 0;

        for (int i = 0; i < 36; i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && item.getType() == Material.TOTEM_OF_UNDYING) {
                total += item.getAmount();
            }
        }

        ItemStack offhand = inv.getItemInOffHand();
        if (offhand.getType() == Material.TOTEM_OF_UNDYING) {
            total += offhand.getAmount();
        }

        return total;
    }

    /** Reabastece todo desde cero: inventario segun dificultad, armadura y offhand. Se usa solo al iniciar
     *  la practica o cuando el jugador se quedo sin ningun totem en inv, hotbar y offhand. */
    private void restockAll(Player player, String difficulty) {
        PlayerInventory inv = player.getInventory();

        switch (difficulty) {
            case "Facil":
                for (int i = 0; i < 36; i++) {
                    ItemStack current = inv.getItem(i);
                    if (current == null || current.getType() == Material.AIR) {
                        inv.setItem(i, new ItemStack(Material.TOTEM_OF_UNDYING, 1));
                    }
                }
                break;

            case "Avanzado":
                for (int i = 0; i < 9; i++) {
                    ItemStack current = inv.getItem(i);
                    if (current == null || current.getType() == Material.AIR) {
                        inv.setItem(i, new ItemStack(Material.TOTEM_OF_UNDYING, 1));
                    }
                }
                break;

            case "Dificil":
                // En dificil no se rellena el inventario principal.
                break;
        }

        equiparArmaduraDeTotems(inv);
        inv.setItemInOffHand(new ItemStack(Material.TOTEM_OF_UNDYING, 1));
    }

    private void equiparArmaduraDeTotems(PlayerInventory inv) {
        if (inv.getHelmet() == null || inv.getHelmet().getType() == Material.AIR) {
            inv.setHelmet(new ItemStack(Material.TOTEM_OF_UNDYING, 1));
        }
        if (inv.getChestplate() == null || inv.getChestplate().getType() == Material.AIR) {
            inv.setChestplate(new ItemStack(Material.TOTEM_OF_UNDYING, 1));
        }
        if (inv.getLeggings() == null || inv.getLeggings().getType() == Material.AIR) {
            inv.setLeggings(new ItemStack(Material.TOTEM_OF_UNDYING, 1));
        }
        if (inv.getBoots() == null || inv.getBoots().getType() == Material.AIR) {
            inv.setBoots(new ItemStack(Material.TOTEM_OF_UNDYING, 1));
        }
    }
            }
