package com.nonkungch.dynamicsurvival;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.io.File;
import java.io.IOException;
import java.util.*;

// (Enum Season ไม่มีการเปลี่ยนแปลง)
enum Season {
    SPRING("ฤดูใบไม้ผลิ", "§a§l"),
    SUMMER("ฤดูร้อน", "§6§l"),
    AUTUMN("ฤดูใบไม้ร่วง", "§c§l"),
    WINTER("ฤดูหนาว", "§b§l");

    private final String thaiName;
    private final String chatColor;
    Season(String thaiName, String chatColor) { this.thaiName = thaiName; this.chatColor = chatColor; }
    public Season next() { return values()[(ordinal() + 1) % values().length]; }
    public String getThaiName() { return thaiName; }
    public String getChatColor() { return chatColor; }
    public void processSeasonStart(DynamicSurvival plugin) { new SeasonProcessor(plugin, this).runTask(plugin); }
}

public class DynamicSurvival extends JavaPlugin implements Listener {

    private Season currentSeason = Season.SPRING;
    private int currentDay = 1;
    private long lastDayTime = 0;
    private long nextWeatherChangeTick = 0;

    private final Map<Player, PlayerStats> playerStats = new HashMap<>();
    private final Map<Player, Scoreboard> playerBoards = new HashMap<>();
    public final Random random = new Random();
    private World trackedWorld;
    private ConfigManager configManager;

    private File dataFile;
    private FileConfiguration dataConfig;

    @Override
    public void onEnable() {
        getLogger().info("DynamicSurvival Plugin (v" + getDescription().getVersion() + ") is enabled!");

        this.configManager = new ConfigManager(this);
        this.configManager.loadConfig();

        setupDataFile();
        loadData();

        if (!Bukkit.getWorlds().isEmpty()) {
            trackedWorld = Bukkit.getWorlds().get(0);
            lastDayTime = trackedWorld.getFullTime();
        } else {
            getLogger().warning("No world found! Season system might not work correctly.");
        }

        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getPluginManager().registerEvents(new ThirstListener(this), this);

        PouchManager pouchManager = new PouchManager(this);
        RecipeManager recipeManager = new RecipeManager(this, pouchManager);
        recipeManager.registerRecipes();
        Bukkit.getPluginManager().registerEvents(new PouchListener(this, pouchManager), this);

        this.getCommand("ds").setExecutor(new DSCommand(this));
        new CalendarGUI(this);

        startSeasonAndWeatherLoop();
        startStatsUpdateLoop();
    }

    @Override
    public void onDisable() {
        saveData();
        getLogger().info("DynamicSurvival Plugin is disabled!");
        Bukkit.getScheduler().cancelTasks(this);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        playerStats.putIfAbsent(player, new PlayerStats(configManager.getBaseTemp(currentSeason), configManager.getMaxThirst()));
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        playerBoards.remove(player);
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    sendScoreboardUpdate(player, getPlayerStats(player));
                }
            }
        }.runTaskLater(this, 1L);
    }

    public Season getCurrentSeason() { return currentSeason; }
    public int getCurrentDay() { return currentDay; }
    public PlayerStats getPlayerStats(Player p) {
        return playerStats.getOrDefault(p, new PlayerStats(configManager.getBaseTemp(currentSeason), configManager.getMaxThirst()));
    }
    public ConfigManager getConfigManager() { return configManager; }

    private void startSeasonAndWeatherLoop() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (trackedWorld == null) return;
                long currentTime = trackedWorld.getFullTime();
                if (currentTime / 24000 > lastDayTime / 24000) {
                    onNewDay();
                }
                lastDayTime = currentTime;
                if (currentTime >= nextWeatherChangeTick) {
                    applyRandomWeather();
                    long cooldownTicks = (long) configManager.getWeatherChangeCooldownMinutes() * 60 * 20;
                    nextWeatherChangeTick = currentTime + cooldownTicks;
                }
            }
        }.runTaskTimer(this, 20L, 20L);
    }

    private void onNewDay() {
        currentDay++;
        if (currentDay > configManager.getSeasonDuration(currentSeason)) {
            changeSeason();
        } else {
            int daysLeft = configManager.getSeasonDuration(currentSeason) - currentDay;
            Bukkit.broadcastMessage(String.format("§e[ปฏิทิน] วันที่ %d ใน %s%s§e (%d วันที่เหลือ)",
                currentDay, currentSeason.getChatColor(), currentSeason.getThaiName(), daysLeft));
        }
        saveData();
    }

    private void changeSeason() {
        currentSeason = currentSeason.next();
        currentDay = 1;
        String msg = String.format("§l§e--- %sการเปลี่ยนฤดูกาลครั้งใหญ่!%s ---", ChatColor.GOLD, ChatColor.RESET);
        String seasonMsg = String.format("%s! ฤดูกาลใหม่คือ: %s%s", ChatColor.YELLOW, currentSeason.getChatColor(), currentSeason.getThaiName());
        Bukkit.broadcastMessage(msg);
        Bukkit.broadcastMessage(seasonMsg);
        currentSeason.processSeasonStart(this);
        Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage("§7(โปรดทราบว่าสภาพแวดล้อมได้เปลี่ยนไปแล้ว!)"));
        saveData();
    }

    private void applyRandomWeather() {
        if (trackedWorld == null) return;
        if (trackedWorld.hasStorm() || trackedWorld.isThundering()) {
            if (random.nextDouble() < 0.5) {
                trackedWorld.setStorm(false);
                trackedWorld.setThundering(false);
                Bukkit.broadcastMessage("§e[สภาพอากาศ] ท้องฟ้าแจ่มใสแล้ว");
                return;
            }
        }
        double chance = random.nextDouble();
        if (chance < 0.1) {
            trackedWorld.setStorm(true);
            trackedWorld.setThundering(true);
            Bukkit.broadcastMessage("§4[สภาพอากาศ] เกิดพายุตกหนัก!");
        } else if (chance < 0.25) {
            trackedWorld.setStorm(true);
            trackedWorld.setThundering(false);
            Bukkit.broadcastMessage("§b[สภาพอากาศ] มีฝน/หิมะตก");
        }
    }

    private void startStatsUpdateLoop() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.isOnline()) {
                        PlayerStats stats = getPlayerStats(player);
                        float newTemp = calculateTemperature(player);
                        stats.setTemperature(newTemp);
                        updateThirst(player, stats);
                        sendScoreboardUpdate(player, stats);
                        applyStatusEffects(player, stats);
                    }
                }
            }
        }.runTaskTimer(this, 40L, 40L);
    }

    public void sendScoreboardUpdate(Player player, PlayerStats stats) {
        Scoreboard board = playerBoards.computeIfAbsent(player, p -> {
            Scoreboard newBoard = Bukkit.getScoreboardManager().getNewScoreboard();
            p.setScoreboard(newBoard);
            return newBoard;
        });
        Objective obj = board.getObjective("ds_stats");
        if (obj == null) {
            obj = board.registerNewObjective("ds_stats", "dummy", ChatColor.translateAlternateColorCodes('&', configManager.getScoreboardTitle()));
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        }
        obj.setDisplayName(ChatColor.translateAlternateColorCodes('&', configManager.getScoreboardTitle()));
        board.getEntries().forEach(board::resetScores);
        List<String> lines = configManager.getScoreboardLines();
        int score = lines.size();
        for (String line : lines) {
            String processedLine = replacePlaceholders(line, player, stats);
            if (processedLine.isEmpty()) {
                processedLine = "§" + Integer.toHexString(score) + "§r";
            }
            obj.getScore(processedLine).setScore(score--);
        }
    }

    private String replacePlaceholders(String line, Player player, PlayerStats stats) {
        int daysLeft = configManager.getSeasonDuration(currentSeason) - currentDay;
        line = ChatColor.translateAlternateColorCodes('&', line);
        line = line.replace("%player_name%", player.getName());
        line = line.replace("%season_name%", currentSeason.getThaiName());
        line = line.replace("%season_color%", currentSeason.getChatColor());
        line = line.replace("%current_day%", String.valueOf(currentDay));
        line = line.replace("%season_duration%", String.valueOf(configManager.getSeasonDuration(currentSeason)));
        line = line.replace("%days_left%", String.valueOf(daysLeft));
        line = line.replace("%temperature%", getTemperatureColor(stats.getTemperature()) + String.format("%.1f°C", stats.getTemperature()));
        line = line.replace("%thirst_value%", String.valueOf(stats.getThirst()));
        line = line.replace("%thirst_max%", String.valueOf(configManager.getMaxThirst()));
        return line;
    }

    private float calculateTemperature(Player player) {
        float temp = configManager.getBaseTemp(currentSeason);

        int leatherPieces = 0;
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            if (armor != null) {
                Material type = armor.getType();
                if (type == Material.LEATHER_HELMET || type == Material.LEATHER_CHESTPLATE || type == Material.LEATHER_LEGGINGS || type == Material.LEATHER_BOOTS) {
                    leatherPieces++;
                }
            }
        }
        if (leatherPieces >= configManager.getLeatherArmorRequiredPieces()) {
            temp += configManager.getLeatherArmorWarmthBonus();
        }

        if (player.getWorld().getEnvironment() == World.Environment.NETHER) {
            float netherHeat = configManager.getNetherTempIncrease();
            int leafArmorPieces = 0;
            NamespacedKey key = new NamespacedKey(this, "is_leaf_armor");
            for (ItemStack armorPiece : player.getInventory().getArmorContents()) {
                if (armorPiece != null && armorPiece.hasItemMeta()) {
                    ItemMeta meta = armorPiece.getItemMeta();
                    if (meta.getPersistentDataContainer().has(key, PersistentDataType.BYTE)) {
                        leafArmorPieces++;
                    }
                }
            }
            if (leafArmorPieces >= configManager.getLeafArmorRequiredPieces()) {
                netherHeat -= configManager.getLeafArmorHeatReduction();
            }
            temp += Math.max(0, netherHeat);
        }
        
        Block playerBlock = player.getLocation().getBlock();
        // **แก้ไข: ใช้ POWDER_SNOW แทน POWDERED_SNOW**
        if (playerBlock.getType() == Material.POWDER_SNOW) {
            temp += configManager.getPowderSnowTempDecrease();
        } else {
            boolean foundEnvEffect = false;
            for (int x = -2; x <= 2; x++) { for (int y = -1; y <= 2; y++) { for (int z = -2; z <= 2; z++) {
                Block block = playerBlock.getRelative(x, y, z);
                Block blockBelow = playerBlock.getRelative(0, -1, 0);
                if (block.getType() == Material.WATER || blockBelow.getType() == Material.SNOW_BLOCK || blockBelow.getType() == Material.SNOW) {
                    temp += configManager.getWaterSnowTempDecrease();
                    foundEnvEffect = true; break;
                }
                if (block.getType() == Material.FIRE || block.getType() == Material.CAMPFIRE || block.getType() == Material.SOUL_CAMPFIRE) {
                    temp += configManager.getFireTempIncrease();
                    foundEnvEffect = true; break;
                }
            } if (foundEnvEffect) break; } if (foundEnvEffect) break; }
        }

        long time = player.getWorld().getTime();
        if (time < 12000) temp += (12000 - time) / 12000.0f * 5.0f;
        else temp -= (time - 12000) / 12000.0f * 5.0f;
        if (player.getWorld().hasStorm()) temp -= 3.0f;
        if (player.getWorld().isThundering()) temp -= 5.0f;
        String biome = player.getLocation().getBlock().getBiome().toString();
        if (biome.contains("DESERT")) temp += 5.0f;
        if (biome.contains("SNOW") || biome.contains("TAIGA")) temp -= 5.0f;
        if (player.getLocation().getBlock().getRelative(0, -1, 0).getType() == Material.FIRE ||
            player.getLocation().getBlock().getRelative(0, -1, 0).getType() == Material.LAVA) {
            temp += 5.0f;
        }
        return temp;
    }

    private void updateThirst(Player player, PlayerStats stats) {
        double thirstLoss = configManager.getBaseThirstLoss();
        if (stats.getTemperature() > configManager.getHotTempThreshold()) thirstLoss *= 2;
        if (player.isSprinting()) thirstLoss += 1;
        if (player.getWorld().getEnvironment() == World.Environment.NETHER) {
            thirstLoss *= configManager.getNetherThirstMultiplier();
        }
        stats.setThirst(Math.max(0, (int) (stats.getThirst() - thirstLoss)));
    }

    private void applyStatusEffects(Player player, PlayerStats stats) {
        float temp = stats.getTemperature();
        if (temp < configManager.getColdTempThreshold()) {
            player.damage(1.0);
            player.sendTitle("", "§bคุณกำลังจะแข็งตาย!", 10, 40, 10);
        } else if (temp < configManager.getFreezingTempThreshold()) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 0, true, false));
        } else if (temp > configManager.getDeadlyHotTempThreshold()) {
            player.damage(1.0);
            player.sendTitle("", "§cคุณกำลังจะถูกเผา!", 10, 40, 10);
        } else if (temp > configManager.getHotTempThreshold()) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 60, 0, true, false));
        }
        if (stats.getThirst() <= 0) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1, true, false));
            player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 200, 0, true, false));
        }
    }

    private void setupDataFile() {
        dataFile = new File(getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            try { dataFile.createNewFile(); } catch (IOException e) { getLogger().severe("Could not create data.yml!"); }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    private void loadData() {
        if (dataConfig.contains("season")) {
            this.currentSeason = Season.valueOf(dataConfig.getString("season", "SPRING"));
        }
        if (dataConfig.contains("day")) {
            this.currentDay = dataConfig.getInt("day", 1);
        }
    }

    private void saveData() {
        dataConfig.set("season", currentSeason.name());
        dataConfig.set("day", currentDay);
        try { dataConfig.save(dataFile); } catch (IOException e) { getLogger().severe("Could not save data to data.yml!"); }
    }

    private ChatColor getTemperatureColor(float temp) {
        if (temp < configManager.getColdTempThreshold()) return ChatColor.DARK_BLUE;
        if (temp < configManager.getFreezingTempThreshold()) return ChatColor.BLUE;
        if (temp > configManager.getDeadlyHotTempThreshold()) return ChatColor.DARK_RED;
        if (temp > configManager.getHotTempThreshold()) return ChatColor.RED;
        if (temp > configManager.getNormalTempThreshold()) return ChatColor.YELLOW;
        return ChatColor.GREEN;
    }

    public static class PlayerStats {
        private float temperature;
        private int thirst;
        public PlayerStats(float temp, int thirst) { this.temperature = temp; this.thirst = thirst; }
        public float getTemperature() { return temperature; }
        public void setTemperature(float t) { this.temperature = t; }
        public int getThirst() { return thirst; }
        public void setThirst(int t) { this.thirst = t; }
        public void addThirst(int amount, int max) { this.thirst = Math.min(max, this.thirst + amount); }
    }
}
