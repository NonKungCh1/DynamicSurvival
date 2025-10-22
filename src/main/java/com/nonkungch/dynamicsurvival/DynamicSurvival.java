// /src/main/java/com/nonkungch/dynamicsurvival/DynamicSurvival.java (ฉบับแก้ไข isWalking)

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
    // ตัวแปรสำหรับเก็บวันปัจจุบันที่ได้ทำการแจ้งเตือนไปแล้ว
    private int lastAnnouncedDay = 0; 

    private final Map<Player, PlayerStats> playerStats = new HashMap<>();
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
        
        // กำหนดค่าเริ่มต้นของ lastAnnouncedDay เมื่อปลั๊กอินเปิด
        lastAnnouncedDay = getCurrentDayInWorld();

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
        // new CalendarGUI(this); 

        startMainLoop();
    }

    @Override
    public void onDisable() {
        getLogger().info("DynamicSurvival Plugin is disabled!");
        Bukkit.getScheduler().cancelTasks(this);
        
        // ล้าง Scoreboard ของผู้เล่นทุกคนก่อนปิดปลั๊กอิน
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (playerBoards.containsKey(p)) {
                // คืนค่า Scoreboard เป็น Main Scoreboard
                p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard()); 
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // ส่งค่า maxThirst เป็น float ให้ PlayerStats
        playerStats.putIfAbsent(player, new PlayerStats(configManager.getBaseTemp(getCurrentSeason()), (float)configManager.getMaxThirst()));
        
        // เมื่อเข้าเกม ให้เริ่มตั้งค่า Scoreboard ทันที
        sendScoreboardUpdate(player, getPlayerStats(player)); 
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // ลบ Scoreboard ของเราออกเมื่อผู้เล่นออกจากเกม
        if (playerBoards.containsKey(player)) {
            // คืนค่า Scoreboard เป็น Main Scoreboard ก่อนออก
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard()); 
        }
        playerBoards.remove(player);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        // แก้ปัญหา Scoreboard หายไป: เรียกให้สร้างและส่ง Scoreboard อีกครั้งหลังจากเกิด
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    sendScoreboardUpdate(player, getPlayerStats(player));
                }
            }
        }.runTaskLater(this, 1L); 
    }
    
    public int getCurrentDayInWorld() {
        if (trackedWorld == null) return 0; // วันที่ 0 คือวันแรก
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
        // ส่งค่า maxThirst เป็น float ให้ PlayerStats
        return playerStats.getOrDefault(p, new PlayerStats(configManager.getBaseTemp(getCurrentSeason()), (float)configManager.getMaxThirst()));
    }
    public ConfigManager getConfigManager() { return configManager; }

    private void startMainLoop() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (trackedWorld == null) return;
                
                int currentDay = getCurrentDayInWorld();
                
                // ตรวจสอบการเปลี่ยนวันเพื่อแจ้งเตือน
                if (currentDay > lastAnnouncedDay) {
                    lastAnnouncedDay = currentDay;
                    
                    Season newSeason = getCurrentSeason();
                    if (newSeason != currentSeason) {
                        currentSeason = newSeason;
                        String msg = String.format("§l§e--- %sการเปลี่ยนฤดูกาลครั้งใหญ่!%s ---", ChatColor.GOLD, ChatColor.RESET);
                        String seasonMsg = String.format("%s! ฤดูกาลใหม่คือ: %s%s", ChatColor.YELLOW, currentSeason.getChatColor(), currentSeason.getThaiName());
                        Bukkit.broadcastMessage(msg);
                        Bukkit.broadcastMessage(seasonMsg);
                        // currentSeason.processSeasonStart(DynamicSurvival.this); 
                        Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage("§7(โปรดทราบว่าสภาพแวดล้อมได้เปลี่ยนไปแล้ว!)"));
                    }
                    
                    // แจ้งเตือนวันใหม่
                    Bukkit.getOnlinePlayers().forEach(player -> {
                        sendDayInfo(player);
                    });
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
                        
                        // เรียกใช้เมท็อด updateThirst ที่ปรับปรุงแล้ว
                        updateThirst(player, stats);
                        
                        // อัปเดต Scoreboard
                        sendScoreboardUpdate(player, stats);
                        
                        // ตรวจสอบสถานะค่าน้ำและแจ้งเตือนเฉพาะเงื่อนไขสำคัญ
                        checkAndSendThirstStatus(player, stats); 

                        applyStatusEffects(player, stats);
                    }
                }
            }
        }.runTaskTimer(this, 40L, 40L); // รันทุก 2 วินาที (40 Ticks)
    }
    
    // เมท็อดสำหรับส่งข้อมูลฤดูกาล/วัน (ใช้เมื่อเป็นวันใหม่เท่านั้น)
    private void sendDayInfo(Player player) {
        Season season = getCurrentSeason();
        int totalDay = getCurrentDayInWorld();
        int dayInSeason = getCurrentDayInSeason();
        
        // ข้อความจะขึ้นในแชทเมื่อเปลี่ยนวันเท่านั้น
        String message = String.format("§7[DS] %s[%s] §f| วันรวม: §e%d §f| วันในฤดู: §e%d", 
            season.getChatColor(), 
            season.getThaiName(),
            totalDay + 1, // +1 เพราะวันรวมจะเริ่มนับจาก 0
            dayInSeason
        );

        player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }
    
    // เมท็อดสำหรับตรวจสอบและแจ้งเตือนสถานะค่าน้ำ
    private void checkAndSendThirstStatus(Player player, PlayerStats stats) {
        float thirst = stats.getThirst();
        int maxThirst = configManager.getMaxThirst();
        double ratio = (double) thirst / maxThirst;
        
        // ใช้ PersistentDataContainer (PDC) เพื่อเก็บสถานะการแจ้งเตือน
        NamespacedKey thirstKey = new NamespacedKey(this, "thirst_status_check");
        // สถานะ: 0=ปกติ, 1=เต็มแล้ว, 2=ใกล้หมด (<10%), 3=หมดแล้ว (<=0)
        int currentStatus = player.getPersistentDataContainer().getOrDefault(thirstKey, PersistentDataType.INTEGER, 0);

        String warningMessage = null;
        int newStatus = currentStatus; // กำหนดค่าเริ่มต้นให้เป็นสถานะปัจจุบัน

        if (thirst >= maxThirst) {
            if (currentStatus != 1) {
                warningMessage = "§a[DS] ค่าน้ำของคุณเต็มแล้ว!";
                newStatus = 1;
            }
        } else if (ratio <= 0.10 && ratio > 0) {
            if (currentStatus != 2) {
                warningMessage = "§c[DS] ค่าน้ำใกล้จะหมดแล้ว! โปรดดื่มน้ำโดยเร็ว";
                newStatus = 2;
            }
        } else if (thirst <= 0) {
            if (currentStatus != 3) {
                warningMessage = "§4[DS] ค่าน้ำหมดแล้ว! คุณเริ่มรู้สึกกระหายน้ำอย่างรุนแรง";
                newStatus = 3;
            }
        } else if (currentStatus != 0) {
            // หากค่าน้ำไม่ได้อยู่ในช่วงวิกฤต (>0.10 ratio) หรือไม่ได้เต็ม
            // และสถานะเดิมเป็น 1, 2, หรือ 3 ให้รีเซ็ตกลับไปเป็นปกติ (0)
            newStatus = 0; 
        }

        if (warningMessage != null) {
            player.sendMessage(warningMessage);
            player.getPersistentDataContainer().set(thirstKey, PersistentDataType.INTEGER, newStatus);
        } else if (newStatus != currentStatus) {
            // อัปเดตสถานะใน PDC เมื่อผู้เล่นดื่มน้ำจนพ้นช่วงอันตราย (เปลี่ยนสถานะเป็น 0)
             player.getPersistentDataContainer().set(thirstKey, PersistentDataType.INTEGER, newStatus);
        }
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

    // เมท็อด Scoreboard ที่ปรับปรุงแล้ว
    public void sendScoreboardUpdate(Player player, PlayerStats stats) {
        Scoreboard board = playerBoards.computeIfAbsent(player, p -> {
            Scoreboard newBoard = Bukkit.getScoreboardManager().getNewScoreboard();
            p.setScoreboard(newBoard); 
            return newBoard;
        });

        if (player.getScoreboard() != board) {
             player.setScoreboard(board);
        }
        
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
            if (processedLine.isEmpty() || processedLine.equals(" ")) { 
                processedLine = "§" + Integer.toHexString(score) + "§r";
            }
            obj.getScore(processedLine).setScore(score--);
        }
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
        // แสดงค่าจำนวนเต็มใน Scoreboard
        line = line.replace("%thirst_value%", String.valueOf(stats.getThirst().intValue())); 
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

    // เมท็อด updateThirst ถูกปรับปรุงใหม่ให้ใช้ค่าทศนิยมและเงื่อนไขการอยู่นิ่ง
    private void updateThirst(Player player, PlayerStats stats) {
        double thirstLoss = configManager.getBaseThirstLoss();
        
        Biome biome = player.getLocation().getBlock().getBiome();
        if (biome.name().contains("DESERT")) {
            thirstLoss *= configManager.getDesertThirstMultiplier();
        }
        
        if (stats.getTemperature() > configManager.getHotTempThreshold()) thirstLoss *= 2;
        
        if (player.isSprinting()) {
            thirstLoss += 0.10; 
        } else {
            // ตรวจสอบว่ากำลังเคลื่อนที่หรือไม่ (ไม่วิ่ง)
            // ใช้ 0.005 เป็นค่า Threshold สำหรับการเดิน
            if (player.getVelocity().lengthSquared() > 0.005) { 
                // กำลังเดิน (Moving/Walking)
                thirstLoss *= 0.5; 
            } else {
                // อยู่นิ่ง (Stationary)
                thirstLoss = 0.0;
            }
        }
        
        if (player.getWorld().getEnvironment() == World.Environment.NETHER) {
            thirstLoss *= configManager.getNetherThirstMultiplier();
        }
        
        // ลดค่าน้ำด้วยค่าทศนิยมจริง ๆ (Thirst ถูกเปลี่ยนเป็น float แล้ว)
        stats.setThirst(Math.max(0f, stats.getThirst() - (float)thirstLoss));
    }

    private void applyStatusEffects(Player player, PlayerStats stats) {
        float temp = stats.getTemperature();
        // ใช้ stats.getThirst() แทน stats.getThirst() <= 0
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

    // เมท็อดช่วยสำหรับสีค่าน้ำ (ยังคงเก็บไว้เผื่อใช้ใน Scoreboard)
    private ChatColor getThirstColor(int current, int max) {
        double ratio = (double) current / max;
        if (ratio > 0.75) return ChatColor.AQUA;
        if (ratio > 0.50) return ChatColor.GREEN;
        if (ratio > 0.25) return ChatColor.YELLOW;
        if (ratio > 0.00) return ChatColor.RED;
        return ChatColor.DARK_RED;
    }


    // คลาส PlayerStats ถูกเปลี่ยนให้ใช้ float สำหรับ thirst
    public static class PlayerStats {
        private float temperature;
        private float thirst; // เปลี่ยนจาก int เป็น float
        
        // Constructor รับค่า thirst เป็น float
        public PlayerStats(float temp, float thirst) { 
            this.temperature = temp; 
            this.thirst = thirst; 
        }
        
        public float getTemperature() { return temperature; }
        public void setTemperature(float t) { this.temperature = t; }
        
        // Getter/Setter เป็น float
        public Float getThirst() { return thirst; } // เปลี่ยนเป็น Float
        public void setThirst(float t) { this.thirst = t; }
        
        // addThirst รับและจัดการค่าเป็น float
        public void addThirst(int amount, int max) { 
            this.thirst = Math.min((float)max, this.thirst + amount); 
        }
    }
}
