package com.tuserver.daytotems;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;

public class PracticeDeathListener implements Listener {

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

        // Mientras estaba en practica, no pierde nada al morir (como un keep inventory personal)
        event.setKeepInventory(true);
        event.getDrops().clear();
        event.setDroppedExp(0);

        player.sendMessage(ChatColor.RED + "Moriste: la practica de totems se detuvo automaticamente.");
    }
  }
