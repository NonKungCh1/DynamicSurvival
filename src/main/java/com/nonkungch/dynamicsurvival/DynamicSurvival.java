// /src/main/java/com/nonkungch/dynamicsurvival/DynamicSurvival.java (ฉบับแก้ไขสมบูรณ์)

package com.nonkungch.dynamicsurvival;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
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

import java.util.*;

// (สมมติว่ามีการสร้างไฟล์ Season.java แยกออกมาแล้ว)
// (สมมติว่ามีการสร้างไฟล์ DynamicSurvivalAPI.java แยกออกมาแล้ว)
// (สมมติว่ามีการสร้างไฟล์ ConfigManager.java แยกออกมาแล้ว)
// (สมมติว่ามีการสร้างไฟล์ PouchManager.java แยกออกมาแล้ว)
// (สมมติว่ามีการสร้างไฟล์ ThirstListener.java แยกออกมาแล้ว)
// (สมมติว่ามีการสร้างไฟล์ RecipeManager.java แยกออกมาแล้ว)
// (สมมติว่ามีการสร้างไฟล์ PouchListener.java แยกออกมาแล้ว)
// (สมมติว่ามีการสร้างไฟล์ DSCommand.java แยกออกมาแล้ว)
// (สมมติว่ามีการสร้างไฟล์ CalendarGUI.java แยกออกมาแล้ว)

public class DynamicSurvival extends JavaPlugin implements Listener {

    private Season currentSeason = Season.SPRING;
    private long nextWeatherChangeTick = 0;
    private int totalDaysInYear = 0;

    private final Map<Player, PlayerStats> playerStats = new HashMap<>();
    // ใช้ Map นี้ในการเก็บ Scoreboard ของผู้เล่นแต่ละคน
    private final Map<Player, Scoreboard> playerBoards = new HashMap<>();
    public final Random random = new Random();
    private World trackedWorld;
    private ConfigManager configManager;

    @Override
    public void onEnable() {
        getLogger().info("DynamicSurvival Plugin (v" + getDescription().getVersion() + ") is enabled!");

        this.configManager = new ConfigManager(this);
        this.configManager.loadConfig();

        if (!Bukkit.getWorlds().isEmpty()) {
            trackedWorld = Bukkit.getWorlds().get(0);
        } else {
            getLogger().warning("No world found! Season system might not work correctly.");
            return;
        }

        // เปิดใช้งาน API
        DynamicSurvivalAPI.initialize(this);
        getLogger().info("DynamicSurvival API has been initialized for addons!"); 

        // คำนวณวันทั้งหมดใน 1 ปีของปลั๊กอิน
        for (Season s : Season.values()) {
            totalDaysInYear += configManager.getSeasonDuration(s);
        }

        Bukkit.getPluginManager().registerEvents(this, this);

        PouchManager pouchManager = new PouchManager(this);
        Bukkit.getPluginManager().registerEvents(new ThirstListener(this), this);
        RecipeManager recipeManager = new RecipeManager(this, pouchManager);
        recipeManager.registerRecipes();
        Bukkit.getPluginManager().registerEvents(new PouchListener(this, pouchManager), this);

        this.getCommand("ds").setExecutor(new DSCommand(this));
        new CalendarGUI(this);

        startMainLoop();
    }

    @Override
    public void onDisable() {
        getLogger().info("DynamicSurvival Plugin is disabled!");
        Bukkit.getScheduler().cancelTasks(this);
        
        // ล้าง Scoreboard ของผู้เล่นทุกคนก่อนปิดปลั๊กอิน
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (playerBoards.containsKey(p)) {
                p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        playerStats.putIfAbsent(player, new PlayerStats(configManager.getBaseTemp(getCurrentSeason()), configManager.getMaxThirst()));
        
        // เมื่อเข้าเกม ให้เริ่มตั้งค่า Scoreboard ทันที
        sendScoreboardUpdate(player, getPlayerStats(player)); 
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // ลบ Scoreboard ของเราออกเมื่อผู้เล่นออกจากเกม
        if (playerBoards.containsKey(player)) {
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
        playerBoards.remove(player);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        // ❌ [แก้ไข] ไม่ต้อง remove ตรงนี้ เพราะ computeIfAbsent ใน sendScoreboardUpdate จะจัดการ
        // เพื่อป้องกันปัญหา Scoreboard หายไปเพราะถูกรีเซ็ตตอนตาย
        // เราจะเรียก sendScoreboardUpdate หลังจากเกิดใหม่เล็กน้อย
        
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    // เรียกให้สร้างและส่ง Scoreboard อีกครั้งหลังจากเกิด
                    sendScoreboardUpdate(player, getPlayerStats(player));
                }
            }
        // ใช้ 1L เพื่อให้แน่ใจว่าเกิดใหม่เสร็จสมบูรณ์แล้ว
        }.runTaskLater(this, 1L); 
    }
    
    public int getCurrentDayInWorld() {
        if (trackedWorld == null) return 1;
        // การนับวันของ Minecraft เริ่มต้นที่ 0 ในวันแรก
        return (int) (trackedWorld.getFullTime() / 24000L); 
    }
    
    public Season getCurrentSeason() {
        if (trackedWorld == null || totalDaysInYear <= 0) return Season.SPRING;
        
        int dayOfYear = getCurrentDayInWorld() % totalDaysInYear;
        int dayCounter = 0;

        for (Season season : Season.values()) {
            dayCounter += configManager.getSeasonDuration(season);
            if (dayOfYear < dayCounter) {
                return season;
            }
        }
        return Season.SPRING;
    }

    public int getCurrentDayInSeason() {
        if (trackedWorld == null || totalDaysInYear <= 0) return 1;

        int dayOfYear = getCurrentDayInWorld() % totalDaysInYear;
        int previousSeasonsDuration = 0;

        for (Season season : Season.values()) {
            if (season == getCurrentSeason()) {
                // บวก 1 เพื่อให้เริ่มต้นจากวันที่ 1
                return dayOfYear - previousSeasonsDuration + 1; 
            }
            previousSeasonsDuration += configManager.getSeasonDuration(season);
        }
        return 1;
    }

    public PlayerStats getPlayerStats(Player p) {
        return playerStats.getOrDefault(p, new PlayerStats(configManager.getBaseTemp(getCurrentSeason()), configManager.getMaxThirst()));
    }
    public ConfigManager getConfigManager() { return configManager; }

    private void startMainLoop() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (trackedWorld == null) return;
                
                Season newSeason = getCurrentSeason();
                if (newSeason != currentSeason) {
                    currentSeason = newSeason;
                    String msg = String.format("§l§e--- %sการเปลี่ยนฤดูกาลครั้งใหญ่!%s ---", ChatColor.GOLD, ChatColor.RESET);
                    String seasonMsg = String.format("%s! ฤดูกาลใหม่คือ: %s%s", ChatColor.YELLOW, currentSeason.getChatColor(), currentSeason.getThaiName());
                    Bukkit.broadcastMessage(msg);
                    Bukkit.broadcastMessage(seasonMsg);
                    // (สมมติว่า Season.processSeasonStart มีการใช้งาน)
                    // currentSeason.processSeasonStart(DynamicSurvival.this); 
                    Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage("§7(โปรดทราบว่าสภาพแวดล้อมได้เปลี่ยนไปแล้ว!)"));
                }
                
                if (trackedWorld.getFullTime() >= nextWeatherChangeTick) {
                    applyRandomWeather();
                    long cooldownTicks = (long) configManager.getWeatherChangeCooldownMinutes() * 60 * 20;
                    nextWeatherChangeTick = trackedWorld.getFullTime() + cooldownTicks;
                }

                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.isOnline()) {
                        PlayerStats stats = getPlayerStats(player);
                        float newTemp = calculateTemperature(player);
                        stats.setTemperature(newTemp);
                        updateThirst(player, stats);
                        
                        // อัปเดต Scoreboard
                        sendScoreboardUpdate(player, stats);
                        
                        // ✅ [เพิ่ม] ส่งข้อมูลในแชท/ActionBar
                        sendPlayerInfo(player, stats); 

                        applyStatusEffects(player, stats);
                    }
                }
            }
        }.runTaskTimer(this, 40L, 40L); // รันทุก 2 วินาที (40 Ticks)
    }

    private void applyRandomWeather() {
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

    // ✅ [แก้ไข] เมท็อดนี้ถูกปรับปรุงเพื่อให้ Scoreboard มีความคงทนมากขึ้น
    public void sendScoreboardUpdate(Player player, PlayerStats stats) {
        // 1. รับหรือสร้าง Scoreboard ใหม่
        Scoreboard board = playerBoards.computeIfAbsent(player, p -> {
            Scoreboard newBoard = Bukkit.getScoreboardManager().getNewScoreboard();
            // ตั้งค่า Scoreboard ทันทีที่สร้าง (ป้องกันปลั๊กอินอื่นมาแย่ง)
            p.setScoreboard(newBoard); 
            return newBoard;
        });

        // 2. ตรวจสอบและบังคับตั้งค่า Scoreboard ให้ผู้เล่นทุกครั้ง
        // เพื่อแก้ไขปัญหา Scoreboard หายไปเมื่อมีปลั๊กอินอื่นเข้ามาแทรก
        if (player.getScoreboard() != board) {
             player.setScoreboard(board);
        }
        
        // 3. จัดการ Objective
        Objective obj = board.getObjective("ds_stats");
        if (obj == null) {
            obj = board.registerNewObjective("ds_stats", "dummy", ChatColor.translateAlternateColorCodes('&', configManager.getScoreboardTitle()));
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        }
        obj.setDisplayName(ChatColor.translateAlternateColorCodes('&', configManager.getScoreboardTitle()));
        
        // 4. อัปเดต Score
        board.getEntries().forEach(board::resetScores);
        List<String> lines = configManager.getScoreboardLines();
        int score = lines.size();
        for (String line : lines) {
            String processedLine = replacePlaceholders(line, player, stats);
            // เพิ่มสีให้บรรทัดว่างด้วยตัวเลข Score
            if (processedLine.isEmpty() || processedLine.equals(" ")) { 
                processedLine = "§" + Integer.toHexString(score) + "§r";
            }
            obj.getScore(processedLine).setScore(score--);
        }
    }

    // ✅ [เพิ่ม] เมท็อดสำหรับส่งข้อมูลผู้เล่นใน ActionBar
    private void sendPlayerInfo(Player player, PlayerStats stats) {
        Season season = getCurrentSeason();
        int totalDay = getCurrentDayInWorld();
        int dayInSeason = getCurrentDayInSeason();
        int thirst = stats.getThirst();
        int maxThirst = configManager.getMaxThirst();
        
        // สร้างข้อความสำหรับ ActionBar
        String message = String.format(" %s[%s] §f| วันรวม: §e%d §f| วันในฤดู: §e%d §f| น้ำ: %s%d§f/%d", 
            season.getChatColor(), 
            season.getThaiName(),
            totalDay + 1, // +1 เพราะวันรวมจะเริ่มนับจาก 0
            dayInSeason,
            getThirstColor(thirst, maxThirst),
            thirst,
            maxThirst
        );

        player.sendActionBar(ChatColor.translateAlternateColorCodes('&', message));
    }


    private String replacePlaceholders(String line, Player player, PlayerStats stats) {
        Season season = getCurrentSeason();
        int dayInSeason = getCurrentDayInSeason();
        int seasonDuration = configManager.getSeasonDuration(season);
        int daysLeft = seasonDuration - dayInSeason;

        line = ChatColor.translateAlternateColorCodes('&', line);
        line = line.replace("%player_name%", player.getName());
        line = line.replace("%season_name%", season.getThaiName());
        line = line.replace("%season_color%", season.getChatColor());
        line = line.replace("%current_day%", String.valueOf(dayInSeason));
        line = line.replace("%season_duration%", String.valueOf(seasonDuration));
        line = line.replace("%days_left%", String.valueOf(daysLeft));
        line = line.replace("%temperature%", getTemperatureColor(stats.getTemperature()) + String.format("%.1f°C", stats.getTemperature()));
        line = line.replace("%thirst_value%", String.valueOf(stats.getThirst()));
        line = line.replace("%thirst_max%", String.valueOf(configManager.getMaxThirst()));
        return line;
    }

    private float calculateTemperature(Player player) {
        float temp = configManager.getBaseTemp(getCurrentSeason());
        Biome biome = player.getLocation().getBlock().getBiome();
        String biomeName = biome.name();

        if (biomeName.contains("DESERT")) {
        } else if (biomeName.contains("SNOW") || biomeName.contains("TAIGA") || biomeName.contains("ICE_SPIKES") || biomeName.contains("FROZEN")) {
            temp += configManager.getColdBiomeTempDecrease();
        }

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
        
        if (player.getLocation().getBlock().getRelative(0, -1, 0).getType() == Material.FIRE ||
            player.getLocation().getBlock().getRelative(0, -1, 0).getType() == Material.LAVA) {
            temp += 5.0f;
        }
        return temp;
    }

    private void updateThirst(Player player, PlayerStats stats) {
        double thirstLoss = configManager.getBaseThirstLoss();
        
        Biome biome = player.getLocation().getBlock().getBiome();
        if (biome.name().contains("DESERT")) {
            thirstLoss *= configManager.getDesertThirstMultiplier();
        }
        
        if (stats.getTemperature() > configManager.getHotTempThreshold()) thirstLoss *= 2;
        
        // ✅ [แก้ไข] ลดค่าการเสียน้ำตอนวิ่ง จากเดิม +1.0 (ซึ่งเยอะมาก)
        // เหลือแค่ +0.10
        if (player.isSprinting()) thirstLoss += 0.10; 
        
        if (player.getWorld().getEnvironment() == World.Environment.NETHER) {
            thirstLoss *= configManager.getNetherThirstMultiplier();
        }
        
        // เราใช้ (int) ในการปัดเศษทิ้ง ดังนั้นค่า thirstLoss จะถูกสะสมไปเรื่อยๆ
        // จนกว่าจะรวมกันได้ 1.0 จึงจะลดค่า Thirst จริงๆ 1 หน่วย
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
            player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 100, 0, true, false));
        }
    }

    private ChatColor getTemperatureColor(float temp) {
        if (temp < configManager.getColdTempThreshold()) return ChatColor.DARK_BLUE;
        if (temp < configManager.getFreezingTempThreshold()) return ChatColor.BLUE;
        if (temp > configManager.getDeadlyHotTempThreshold()) return ChatColor.DARK_RED;
        if (temp > configManager.getHotTempThreshold()) return ChatColor.RED;
        if (temp > configManager.getNormalTempThreshold()) return ChatColor.YELLOW;
        return ChatColor.GREEN;
    }

    // ✅ [เพิ่ม] เมท็อดช่วยสำหรับสีค่าน้ำ
    private ChatColor getThirstColor(int current, int max) {
        double ratio = (double) current / max;
        if (ratio > 0.75) return ChatColor.AQUA;
        if (ratio > 0.50) return ChatColor.GREEN;
        if (ratio > 0.25) return ChatColor.YELLOW;
        if (ratio > 0.00) return ChatColor.RED;
        return ChatColor.DARK_RED;
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
