package com.tuserver.daytotems;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class DayTotems extends JavaPlugin implements CommandExecutor, TabCompleter {

    private final Map<UUID, BukkitTask> activeTasks = new HashMap<>();
    private final Map<UUID, String> selectedDifficulty = new HashMap<>();
    private final Map<UUID, Integer> popCounts = new HashMap<>();
    private final Map<UUID, Integer> sessionPops = new HashMap<>();

    private static final ItemStack PANEL = buildItem(Material.GRAY_STAINED_GLASS_PANE, " ", null);

    private File statsFile;
    private YamlConfiguration statsConfig;

    @Override
    public void onEnable() {
        getCommand("daytotems").setExecutor(this);
        getCommand("daytotems").setTabCompleter(this);
        getCommand("daytop").setExecutor(this);
        getCommand("daytop").setTabCompleter(this);
        Bukkit.getPluginManager().registerEvents(new MenuListener(this), this);
        Bukkit.getPluginManager().registerEvents(new TotemPopListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PracticeDeathListener(this), this);
        loadStats();
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

    // ==================== Estadisticas de totems popeados ====================

    private void loadStats() {
        statsFile = new File(getDataFolder(), "popstats.yml");
        if (!statsFile.exists()) {
            getDataFolder().mkdirs();
            try {
                statsFile.createNewFile();
            } catch (IOException e) {
                getLogger().warning("No se pudo crear popstats.yml");
            }
        }
        statsConfig = YamlConfiguration.loadConfiguration(statsFile);
        popCounts.clear();
        if (statsConfig.isConfigurationSection("pops")) {
            for (String key : statsConfig.getConfigurationSection("pops").getKeys(false)) {
                try {
                    popCounts.put(UUID.fromString(key), statsConfig.getInt("pops." + key));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
    }

    private void saveStats() {
        try {
            statsConfig.save(statsFile);
        } catch (IOException e) {
            getLogger().warning("No se pudo guardar popstats.yml");
        }
    }

    /** Suma 1 al contador de totems popeados de un jugador y lo guarda en disco. */
    public void incrementPop(UUID uuid) {
        popCounts.merge(uuid, 1, Integer::sum);
        statsConfig.set("pops." + uuid.toString(), popCounts.get(uuid));
        saveStats();
    }

    // ==================== Contador de popeadas de la sesion actual ====================

    /** Reinicia el contador de popeadas de la practica actual (se llama al empezar una practica). */
    public void resetSessionPops(UUID uuid) {
        sessionPops.put(uuid, 0);
    }

    /** Suma 1 al contador de popeadas de la practica en curso de ese jugador. */
    public void incrementSessionPop(UUID uuid) {
        sessionPops.merge(uuid, 1, Integer::sum);
    }

    /** Devuelve cuantas popeadas lleva en la practica actual (0 si no tiene una registrada). */
    public int getSessionPops(UUID uuid) {
        return sessionPops.getOrDefault(uuid, 0);
    }

    /** Limpia el contador de popeadas de sesion de un jugador. */
    public void clearSessionPops(UUID uuid) {
        sessionPops.remove(uuid);
    }

    /** Devuelve el top N de jugadores segun totems popeados. */
    public List<Map.Entry<UUID, Integer>> getTopPoppers(int n) {
        return popCounts.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .limit(n)
                .collect(Collectors.toList());
    }

    /** Elimina del top a un jugador puntual por su nick. Devuelve true si estaba y se borro. */
    public boolean removePlayerStats(String nick) {
        for (UUID uuid : new ArrayList<>(popCounts.keySet())) {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
            if (offlinePlayer.getName() != null && offlinePlayer.getName().equalsIgnoreCase(nick)) {
                popCounts.remove(uuid);
                statsConfig.set("pops." + uuid.toString(), null);
                saveStats();
                return true;
            }
        }
        return false;
    }

    /** Borra el top completo (puestos 1, 2 y 3 y cualquier otro registro). */
    public void clearAllStats() {
        popCounts.clear();
        statsConfig.set("pops", null);
        saveStats();
    }

    /** Recarga los datos de estadisticas desde el archivo en disco. */
    public void reloadStats() {
        loadStats();
    }

    // ==================== Comando ====================

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Este comando solo puede usarse en el juego.");
            return true;
        }
        Player player = (Player) sender;

        if (label.equalsIgnoreCase("daytop")) {
            if (args.length > 0) {
                String sub = args[0].toLowerCase();

                if (sub.equals("clear") || sub.equals("reload")) {
                    clearAllStats();
                    player.sendMessage(ChatColor.GREEN + "Se reinicio el top de totems popeados (puestos 1, 2 y 3).");
                    return true;
                }

                if (sub.equals("removeplayer") || sub.equals("rmpl")) {
                    if (args.length < 2) {
                        player.sendMessage(ChatColor.RED + "Uso: /daytop removeplayer <nick>");
                        return true;
                    }
                    boolean removed = removePlayerStats(args[1]);
                    if (removed) {
                        player.sendMessage(ChatColor.GREEN + "Se elimino a " + args[1] + " del top.");
                    } else {
                        player.sendMessage(ChatColor.GRAY + "Ese jugador no aparece en el top.");
                    }
                    return true;
                }
            }

            player.openInventory(createTopMenu());
            return true;
        }

        // daytotems
        if (args.length > 0 && args[0].equalsIgnoreCase("help")) {
            enviarAyuda(player);
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            reloadStats();
            player.sendMessage(ChatColor.GREEN + "DayTotems: datos recargados.");
            return true;
        }

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

    private void enviarAyuda(Player player) {
        player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "==== Comandos de DayTotems ====");
        player.sendMessage(ChatColor.YELLOW + "/daytotems" + ChatColor.GRAY + " - Abre el menu de practica de totems.");
        player.sendMessage(ChatColor.YELLOW + "/daytotems stop" + ChatColor.GRAY + " - Detiene tu practica activa.");
        player.sendMessage(ChatColor.YELLOW + "/daytotems reload" + ChatColor.GRAY + " - Recarga los datos guardados.");
        player.sendMessage(ChatColor.YELLOW + "/daytotems help" + ChatColor.GRAY + " - Muestra esta ayuda.");
        player.sendMessage(ChatColor.YELLOW + "/daytop" + ChatColor.GRAY + " - Muestra el top 3 de popeadores de totems.");
        player.sendMessage(ChatColor.YELLOW + "/daytop clear" + ChatColor.GRAY + " - Reinicia el top completo (alias: reload).");
        player.sendMessage(ChatColor.YELLOW + "/daytop removeplayer <nick>" + ChatColor.GRAY + " - Saca a un jugador del top (alias: rmpl).");
    }

    // ==================== Autocompletado ====================

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> opciones;
            if (command.getName().equalsIgnoreCase("daytop")) {
                opciones = Arrays.asList("clear", "reload", "removeplayer", "rmpl");
            } else {
                opciones = Arrays.asList("stop", "reload", "help");
            }

            String actual = args[0].toLowerCase();
            List<String> coincidencias = new ArrayList<>();
            for (String opcion : opciones) {
                if (opcion.startsWith(actual)) {
                    coincidencias.add(opcion);
                }
            }
            return coincidencias;
        }

        if (args.length == 2 && command.getName().equalsIgnoreCase("daytop")
                && (args[0].equalsIgnoreCase("removeplayer") || args[0].equalsIgnoreCase("rmpl"))) {
            List<String> nombres = new ArrayList<>();
            for (Player online : Bukkit.getOnlinePlayers()) {
                nombres.add(online.getName());
            }
            return nombres;
        }

        return Collections.emptyList();
    }

    // ==================== Menus ====================

    /** Menu de 54 slots con borde de paneles grises y 3 opciones centradas. */
    public Inventory createDifficultyMenu() {
        Inventory inv = Bukkit.createInventory(null, 54, ChatColor.DARK_RED + "Practica de Totems - Dificultad");

        fillBorder(inv);

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
        int[] slots = {19, 21, 22, 23, 25};

        for (int i = 0; i < ticks.length; i++) {
            inv.setItem(slots[i], buildItem(Material.TOTEM_OF_UNDYING, ChatColor.AQUA + "Cada " + ticks[i] + " ticks",
                    Collections.singletonList(ChatColor.GRAY + "Click para comenzar la practica")));
        }

        inv.setItem(45, buildItem(Material.ARROW, ChatColor.GRAY + "« Volver", null));

        return inv;
    }

    /** Menu de 54 slots con borde de paneles grises mostrando el top 3 de totems popeados. */
    public Inventory createTopMenu() {
        Inventory inv = Bukkit.createInventory(null, 54, ChatColor.GOLD + "Top 3 Popeadores de Totems");

        fillBorder(inv);

        List<Map.Entry<UUID, Integer>> top = getTopPoppers(3);
        int[] slots = {20, 22, 24};
        String[] medals = {"#1", "#2", "#3"};

        for (int i = 0; i < 3; i++) {
            if (i < top.size()) {
                UUID uuid = top.get(i).getKey();
                int count = top.get(i).getValue();
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
                String name = offlinePlayer.getName() != null ? offlinePlayer.getName() : "Desconocido";

                ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta meta = (SkullMeta) head.getItemMeta();
                meta.setOwningPlayer(offlinePlayer);
                meta.setDisplayName(ChatColor.GOLD + medals[i] + " " + ChatColor.YELLOW + name);
                meta.setLore(Collections.singletonList(ChatColor.GRAY + "Totems popeados: " + ChatColor.AQUA + count));
                head.setItemMeta(meta);

                inv.setItem(slots[i], head);
            } else {
                inv.setItem(slots[i], buildItem(Material.GRAY_DYE, ChatColor.GRAY + medals[i] + " - Sin datos", null));
            }
        }

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
