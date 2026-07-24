package com.tuserver.daytotems;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;

public class PracticeDeathListener implements Listener {

    private static final String MENSAJE_DERROTA = "&c&l[!] &c¡Perdiste! Lograste hacer %pops% popeadas.";

    private final DayTotems plugin;

    public PracticeDeathListener(DayTotems plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID id = player.getUniqueId();

        BukkitTask task = plugin.getActiveTasks().remove(id);
        if (task == null) return; // No estaba en practica, no tocamos nada

        task.cancel();

        // Mientras estaba en practica, no dropea nada al morir...
        event.setKeepInventory(true);
        event.getDrops().clear();
        event.setDroppedExp(0);

        // ...pero tampoco se queda con nada: se le vacia todo el inventario, como un /clear.
        limpiarInventario(player.getInventory());

        int pops = plugin.getSessionPops(id);
        plugin.clearSessionPops(id);

        String mensaje = ChatColor.translateAlternateColorCodes('&',
                MENSAJE_DERROTA.replace("%pops%", String.valueOf(pops)));
        player.sendMessage(mensaje);
    }

    private void limpiarInventario(PlayerInventory inv) {
        inv.clear();
        inv.setHelmet(null);
        inv.setChestplate(null);
        inv.setLeggings(null);
        inv.setBoots(null);
        inv.setItemInOffHand(null);
    }
            }
