package com.tuserver.daytotems;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityResurrectEvent;

public class TotemPopListener implements Listener {

    private final DayTotems plugin;

    public TotemPopListener(DayTotems plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onResurrect(EntityResurrectEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            plugin.incrementPop(player.getUniqueId());
        }
    }
    }
