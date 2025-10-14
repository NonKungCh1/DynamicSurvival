package com.nonkungch.dynamicsurvival;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import com.nonkungch.dynamicsurvival.managers.TemperatureManager;
import com.nonkungch.dynamicsurvival.managers.ThirstManager;
import com.nonkungch.dynamicsurvival.managers.TimeManager;

import java.util.Random;

// ====================================================================================
// ENUMERATIONS (Season)
// ====================================================================================

enum Season {
    SPRING("ฤดูใบไม้ผลิ", "§a§l"), 
    SUMMER("ฤดูร้อน", "§6§l"),  
    AUTUMN("ฤดูใบไม้ร่วง", "§c§l"), 
    WINTER("ฤดูหนาว", "§b§l");  

    private final String thaiName;
    private final String chatColor;

    Season(String thaiName, String chatColor) {
        this.thaiName = thaiName;
        this.chatColor = chatColor;
    }

    public String getThaiName() { return thaiName; }
    public String getChatColor() { return chatColor; }
    public static Season getNextSeason(Season current) {
        return Season.values()[(current.ordinal() + 1) % Season.values().length];
    }
}

// ====================================================================================
// MAIN PLUGIN CLASS
// ====================================================================================

public class DynamicSurvival extends JavaPlugin {

    private static DynamicSurvival instance;

    // Managers
    private ConfigManager configManager;
    private TemperatureManager temperatureManager;
    private ThirstManager thirstManager;
    private TimeManager timeManager;
    
    // Core State
    private World trackedWorld;
    private Random random = new Random();
    
    // Season & Time State
    private Season currentSeason = Season.SPRING;
    private int currentDay = 1;
    private long lastDayTime = 0; 
    
    // **WEATHER STATE (ได้รับการแก้ไข)**
    private boolean isWeatherRunning = false;
    private int weatherDurationLeft = 0; // ระยะเวลาคงเหลือของฝน (หน่วยเป็นวัน)

    private BukkitAudiences adventure;

    @Override
    public void onEnable() {
        instance = this;
        adventure = BukkitAudiences.create(this);
        
        // 1. Init Config & Managers
        this.configManager = new ConfigManager(this);
        configManager.loadConfig();
        this.temperatureManager = new TemperatureManager(this);
        this.thirstManager = new ThirstManager(this);
        this.timeManager = new TimeManager(this);
        
        // 2. Setup World (ใช้ Config)
        String worldName = configManager.getTrackedWorldName();
        trackedWorld = Bukkit.getWorld(worldName);
        if (trackedWorld == null) {
            getLogger().warning("ไม่พบโลกที่ระบุใน Config: " + worldName + "! ใช้โลกแรกแทน");
            if (!Bukkit.getWorlds().isEmpty()) {
                trackedWorld = Bukkit.getWorlds().get(0);
            }
        }
        if (trackedWorld != null) {
            this.isWeatherRunning = trackedWorld.hasStorm() || trackedWorld.isThundering();
        }
        
        // 3. Register Events & Command
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this); // assumed PlayerListener
        getServer().getPluginManager().registerEvents(new ThirstListener(this), this);
        // Assuming CommandManager is renamed to DSCommand
        getCommand("ds").setExecutor(new DSCommand(this)); 

        // 4. Start Loops (ใช้ Config สำหรับความถี่)
        startSeasonAndWeatherLoop(); 
        startStatsUpdateLoop();
        
        getLogger().info("DynamicSurvival enabled!");
    }
    
    @Override
    public void onDisable() {
        if (adventure != null) {
            adventure.close();
            adventure = null;
        }
        getLogger().info("DynamicSurvival disabled!");
    }

    // --- Getters ---
    public static DynamicSurvival getInstance() { return instance; }
    public ConfigManager getConfigManager() { return configManager; }
    public TemperatureManager getTempManager() { return temperatureManager; }
    public ThirstManager getThirstManager() { return thirstManager; }
    public TimeManager getTimeManager() { return timeManager; }
    public Season getCurrentSeason() { return currentSeason; }
    public int getCurrentDay() { return currentDay; }

    // ส่ง ActionBar ผ่าน Adventure Platform
    public void sendActionBar(Player player, String message) {
        adventure.player(player).sendActionBar(Component.text(message));
    }
    
    // ====================================================================================
    // 1. ระบบฤดูกาล & ปฏิทิน & สภาพอากาศ (แก้ไข Logic สภาพอากาศ)
    // ====================================================================================

    private void startSeasonAndWeatherLoop() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (trackedWorld == null) return;
                
                long currentTime = trackedWorld.getFullTime();
                // ตรวจสอบการเปลี่ยนวัน
                if (currentTime / 24000 > lastDayTime / 24000) {
                    onNewDay(); 
                }
                lastDayTime = currentTime;
                
                // ตรวจสอบ/ลดระยะเวลาสภาพอากาศที่มีอยู่เมื่อเข้าสู่วันใหม่
                if (isWeatherRunning && trackedWorld.getFullTime() % 24000 == 1000) { 
                    if (weatherDurationLeft > 0) {
                         weatherDurationLeft--;
                    }
                    if (weatherDurationLeft <= 0) {
                        stopWeather();
                    }
                }
            }
        }.runTaskTimer(this, 20L, 20L);
    }
    
    private void onNewDay() {
        currentDay++;
        
        // 1. ตรวจสอบสภาพอากาศใหม่ ถ้าโลกกำลังแจ่มใสเท่านั้น
        if (!isWeatherRunning) {
            checkAndStartWeather();
        }

        // 2. ตรวจสอบการเปลี่ยนฤดูกาล
        if (currentDay > configManager.getSeasonDuration(currentSeason)) {
            changeSeason();
        } else {
            int daysLeft = configManager.getSeasonDuration(currentSeason) - currentDay;
            Bukkit.broadcastMessage(String.format("§e[ปฏิทิน] วันที่ %d ใน %s%s§e (%d วันที่เหลือ)", 
                currentDay, currentSeason.getChatColor(), currentSeason.getThaiName(), daysLeft));
        }
    }
    
    private void checkAndStartWeather() {
        
        // 1. ฝนปกติ: เกิด 1-3 วัน/รอบ, ตก 1-2 วัน (ใช้ Config)
        int normalMin = configManager.getNormalRainMinDay();
        int normalMax = configManager.getNormalRainMaxDay();
        double normalChance = configManager.getNormalRainChance();
        int normalDurationMin = configManager.getNormalRainDurationMin();
        int normalDurationMax = configManager.getNormalRainDurationMax();

        if (currentDay >= normalMin && currentDay <= normalMax) {
            if (random.nextDouble() < normalChance) { 
                int duration = random.nextInt(normalDurationMax - normalDurationMin + 1) + normalDurationMin;
                startRain(duration); 
                return;
            }
        }

        // 2. ฝนตกหนัก: เกิด 5-6 วัน/รอบ, ตก 4-5 วัน (ใช้ Config)
        int heavyMin = configManager.getHeavyStormMinDay();
        int heavyMax = configManager.getHeavyStormMaxDay();
        double heavyChance = configManager.getHeavyStormChance();
        int heavyDurationMin = configManager.getHeavyStormDurationMin();
        int heavyDurationMax = configManager.getHeavyStormDurationMax();

        if (currentDay >= heavyMin && currentDay <= heavyMax) {
            if (random.nextDouble() < heavyChance) { 
                int duration = random.nextInt(heavyDurationMax - heavyDurationMin + 1) + heavyDurationMin;
                startStorm(duration); 
                return;
            }
        }
        
        // รีเซ็ตตัวนับวันถ้าเกินรอบสูงสุดของโอกาสเกิดสภาพอากาศ (เพื่อให้วนรอบใหม่ 1-6)
        if (currentDay > heavyMax) {
            currentDay = 1; 
        }
    }

    private void startRain(int durationDays) {
        if (trackedWorld == null) return;
        trackedWorld.setStorm(true);
        trackedWorld.setThundering(false);
        trackedWorld.setWeatherDuration(durationDays * 24000); 
        isWeatherRunning = true;
        weatherDurationLeft = durationDays;
        Bukkit.broadcastMessage("§e[สภาพอากาศ] ฝนเริ่มตกปกติ คาดว่าจะนาน " + durationDays + " วัน.");
    }

    private void startStorm(int durationDays) {
        if (trackedWorld == null) return;
        trackedWorld.setStorm(true);
        trackedWorld.setThundering(true);
        trackedWorld.setWeatherDuration(durationDays * 24000); 
        isWeatherRunning = true;
        weatherDurationLeft = durationDays;
        Bukkit.broadcastMessage("§c[สภาพอากาศ] ฝนตกหนักและมีพายุ คาดว่าจะนาน " + durationDays + " วัน!");
    }
    
    private void stopWeather() {
        if (trackedWorld == null) return;
        trackedWorld.setStorm(false);
        trackedWorld.setThundering(false);
        isWeatherRunning = false;
        weatherDurationLeft = 0;
        Bukkit.broadcastMessage("§e[สภาพอากาศ] เมฆฝนเคลื่อนตัวผ่านไป ท้องฟ้าแจ่มใสแล้ว.");
    }
    
    private void changeSeason() {
        Season nextSeason = Season.getNextSeason(currentSeason);
        currentSeason = nextSeason;
        currentDay = 1; 
        
        Bukkit.broadcastMessage(String.format("§6[ปฏิทิน] ฤดูกาลเปลี่ยนเป็น: %s%s§6 แล้ว!", 
            currentSeason.getChatColor(), currentSeason.getThaiName()));
        
        // Assume SeasonProcessor exists and is a BukkitRunnable
        // new SeasonProcessor(this, currentSeason).runTaskAsynchronously(this);
    }

    // ====================================================================================
    // 2. ระบบอัปเดตสถานะผู้เล่น & Action Bar / Scoreboard
    // ====================================================================================
    
    private void startStatsUpdateLoop() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    updatePlayer(player);
                }
            }
        }.runTaskTimer(this, 0, configManager.getUpdateFrequencyTicks()); // ใช้ค่า Config
    }

    private void updatePlayer(Player player) {
        double temp = temperatureManager.getTemperature(player);
        double thirst = thirstManager.getThirst(player);
        
        // 1. Thirst Loss Calculation (ใช้ Config)
        double thirstLoss = configManager.getBaseThirstLoss();
        if (player.isSprinting()) {
            thirstLoss *= configManager.getSprintMultiplier();
        }

        // Apply Thirst Loss
        thirstManager.setThirst(player, thirst - thirstLoss);
        
        // 2. Temperature Logic (ตัวอย่างการลด/เพิ่มอุณหภูมิ)
        // คุณอาจจะต้องเขียน logic การปรับอุณหภูมิตามไบโอม/เสื้อผ้าเพิ่มเติม
        // ในตัวอย่างนี้ ลดอุณหภูมิพื้นฐาน
        double tempChange = -0.1; 
        if (trackedWorld != null && trackedWorld.hasStorm()) {
            tempChange += configManager.getTempModifierOnRain() / 10.0; // หารเพื่อลดผลกระทบ
        }
        temperatureManager.setTemperature(player, temp + tempChange);

        // 3. Apply Effects (ใช้ Config)
        double currentTemp = temperatureManager.getTemperature(player);
        double currentThirst = thirstManager.getThirst(player);
        double damage = configManager.getDamageAmount();

        if (currentTemp < configManager.getFreezingThreshold()) {
            player.setFreezeTicks(100);
            player.damage(damage); 
        }
        if (currentTemp > configManager.getHotThreshold()) {
            player.damage(damage); 
        }
        if (currentThirst <= configManager.getThirstDangerLevel()) {
            player.damage(damage); 
        }
        
        // 4. Display Status (ใช้ Config)
        // ในโค้ดตัวอย่างนี้ จะใช้ ActionBar ตามที่ตั้งค่าใน Config
        sendActionBar(player, currentTemp, currentThirst);
    }

    private void sendActionBar(Player player, double temp, double thirst) {
        ChatColor tempColor = getTemperatureColor(temp);
        String thirstBar = getThirstBar(thirst);
        String seasonDisplay = currentSeason.getChatColor() + currentSeason.getThaiName();
        String dayDisplay = String.valueOf(currentDay);

        String message = String.format("§f[ปฏิทิน: %s§f - วันที่ %s]   |   [อุณหภูมิ: %s%.1f°C§f]   |   [น้ำ: %s§f]",
            seasonDisplay, dayDisplay, 
            tempColor, temp, thirstBar);

        Component component = LegacyComponentSerializer.legacySection().deserialize(message);
        adventure.player(player).sendActionBar(component);
    }
    
    private ChatColor getTemperatureColor(double temp) {
        if (temp < configManager.getFreezingThreshold()) return ChatColor.AQUA;
        if (temp > configManager.getHotThreshold()) return ChatColor.RED;
        return ChatColor.YELLOW;
    }
    
    private String getThirstBar(double thirst) {
        int max = configManager.getMaxThirst();
        int bars = 10;
        int filledBars = (int) Math.ceil((double) thirst / max * bars);
        
        StringBuilder bar = new StringBuilder();
        
        ChatColor color;
        if (thirst <= configManager.getThirstDangerLevel()) {
            color = ChatColor.RED;
        } else if (thirst < max / 2) {
            color = ChatColor.YELLOW;
        } else {
            color = ChatColor.BLUE;
        }
        
        bar.append(color).append(ChatColor.BOLD);
        for (int i = 0; i < bars; i++) {
            if (i < filledBars) {
                bar.append("█");
            } else {
                bar.append("§7█");
            }
        }
        bar.append(ChatColor.RESET).append(" (").append(String.format("%.0f", thirst)).append(")");
        return bar.toString();
    }
}
