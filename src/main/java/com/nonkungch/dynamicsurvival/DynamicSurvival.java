package com.nonkungch.dynamicsurvival;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

// ************************ การนำเข้าใหม่สำหรับ Adventure API ************************
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.platform.bukkit.BukkitAudiences; // NEW: สำหรับจัดการ Action Bar
import net.kyori.adventure.audience.Audience;             // NEW: สำหรับดึงผู้เล่น
// ********************************************************************************

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

// ====================================================================================
// ENUMERATIONS
// ====================================================================================

enum Season {
    SPRING("ฤดูใบไม้ผลิ", 20, "§a§l"), 
    SUMMER("ฤดูร้อน", 25, "§6§l"),  
    AUTUMN("ฤดูใบไม้ร่วง", 15, "§c§l"), 
    WINTER("ฤดูหนาว", 30, "§b§l");  

    private final String thaiName;
    private final int durationDays; 
    private final String chatColor;

    Season(String thaiName, int durationDays, String chatColor) {
        this.thaiName = thaiName;
        this.durationDays = durationDays;
        this.chatColor = chatColor;
    }

    public Season next() {
        return values()[(ordinal() + 1) % values().length];
    }
    
    public String getThaiName() { return thaiName; }
    public int getDurationDays() { return durationDays; }
    public String getChatColor() { return chatColor; }
}

// ====================================================================================
// MAIN PLUGIN CLASS: DynamicSurvival
// ====================================================================================

public class DynamicSurvival extends JavaPlugin implements Listener { // แก้ไขชื่อคลาสแล้ว

    private Season currentSeason = Season.SPRING;
    private int currentDay = 1;
    private long lastDayTime = 0;
    private final Map<Player, PlayerStats> playerStats = new HashMap<>();
    private final Random random = new Random();
    private World trackedWorld; 
    private BukkitAudiences audiences; // NEW: ตัวแปรสำหรับ Adventure Audiences

    @Override
    public void onEnable() {
        getLogger().info("DynamicSurvival Plugin (v" + getDescription().getVersion() + ") กำลังทำงาน!");
        
        // ************************ NEW: เริ่มต้น Adventure Audiences ************************
        this.audiences = BukkitAudiences.create(this);
        // ***********************************************************************************
        
        if (!Bukkit.getWorlds().isEmpty()) {
            trackedWorld = Bukkit.getWorlds().get(0);
        } else {
            getLogger().warning("ไม่พบโลก! ระบบฤดูกาลอาจทำงานไม่ถูกต้อง");
        }
        
        Bukkit.getPluginManager().registerEvents(this, this);
        startSeasonAndWeatherLoop();
        startStatsUpdateLoop();
    }

    @Override
    public void onDisable() {
        getLogger().info("DynamicSurvival Plugin ถูกปิดการทำงาน!");
        Bukkit.getScheduler().cancelTasks(this);
        
        // ************************ NEW: ปิด Adventure Audiences ************************
        if (this.audiences != null) {
            this.audiences.close();
        }
        // ******************************************************************************
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        playerStats.putIfAbsent(player, new PlayerStats(20.0f, 100));
    }
    
    // ====================================================================================
    // 1. ระบบฤดูกาล & ปฏิทิน & สภาพอากาศ
    // ====================================================================================

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

                if (currentTime % 12000 == 0 && random.nextDouble() < 0.2) { 
                    applyRandomWeather();
                }
            }
        }.runTaskTimer(this, 20L, 20L);
    }
    
    private void onNewDay() {
        currentDay++;
        
        if (currentDay > currentSeason.getDurationDays()) {
            changeSeason();
        } else {
            Bukkit.broadcastMessage(String.format("§e[ปฏิทิน] วันที่ %d ใน %s%s§e (%d วันที่เหลือ)", 
                currentDay, currentSeason.getChatColor(), currentSeason.getThaiName(), 
                currentSeason.getDurationDays() - currentDay));
        }
    }

    private void changeSeason() {
        currentSeason = currentSeason.next();
        currentDay = 1;

        String msg = String.format("§l§e--- %sการเปลี่ยนฤดูกาลครั้งใหญ่!%s ---", ChatColor.GOLD, ChatColor.RESET);
        String seasonMsg = String.format("%s! ฤดูกาลใหม่คือ: %s%s", ChatColor.YELLOW, currentSeason.getChatColor(), currentSeason.getThaiName());
        
        Bukkit.broadcastMessage(msg);
        Bukkit.broadcastMessage(seasonMsg);

        Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage("§7(โปรดทราบว่าสีใบไม้เปลี่ยนไปแล้วตามฤดูกาล!)"));
    }
    
    private void applyRandomWeather() {
        if (trackedWorld == null) return;
        
        double chance = random.nextDouble();
        
        if (chance < 0.1) {
            trackedWorld.setStorm(false);
            trackedWorld.setThundering(true); 
            Bukkit.broadcastMessage("§4[สภาพอากาศ] เกิดพายุฝนฟ้าคะนอง! อากาศจะเย็นลงอย่างรวดเร็ว.");
        } else if (chance < 0.3) {
            trackedWorld.setStorm(true);
            trackedWorld.setThundering(false); 
            Bukkit.broadcastMessage("§b[สภาพอากาศ] มีฝนตก/หิมะตก อุณหภูมิจะลดลง.");
        } else {
            trackedWorld.setStorm(false);
            trackedWorld.setThundering(false); 
            Bukkit.broadcastMessage("§e[สภาพอากาศ] ท้องฟ้าแจ่มใส อากาศดี.");
        }
    }

    // ====================================================================================
    // 2. ระบบอุณหภูมิ & หลอดน้ำ (แสดงผลใน Action Bar)
    // ====================================================================================

    private void startStatsUpdateLoop() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    PlayerStats stats = playerStats.getOrDefault(player, new PlayerStats(20.0f, 100));
                    
                    float newTemp = calculateTemperature(player);
                    stats.setTemperature(newTemp);
                    
                    updateThirst(player, stats, newTemp);
                    
                    sendActionBarUpdate(player, stats);
                    
                    applyTemperatureEffects(player, newTemp);
                }
            }
        }.runTaskTimer(this, 40L, 40L);
    }
    
    private float calculateTemperature(Player player) {
        float baseTemp = 25.0f;
        switch (currentSeason) {
            case SPRING: baseTemp = 20.0f; break;
            case SUMMER: baseTemp = 30.0f; break;
            case AUTUMN: baseTemp = 15.0f; break;
            case WINTER: baseTemp = 0.0f; break;
        }

        float temp = baseTemp;
        
        long time = player.getWorld().getTime(); 
        if (time < 12000) { 
            temp += (12000 - time) / 12000.0f * 5.0f; 
        } else { 
            temp -= (time - 12000) / 12000.0f * 5.0f; 
        }

        if (player.getWorld().hasStorm()) {
            temp -= 3.0f; 
        }
        if (player.getWorld().isThundering()) {
            temp -= 5.0f; 
        }
        
        String biome = player.getLocation().getBlock().getBiome().toString();
        if (biome.contains("DESERT")) temp += 5.0f;
        if (biome.contains("SNOW") || biome.contains("TAIGA")) temp -= 5.0f;

        if (player.getLocation().getBlock().getRelative(0, -1, 0).getType() == Material.FIRE || 
            player.getLocation().getBlock().getRelative(0, -1, 0).getType() == Material.LAVA) {
            temp += 5.0f;
        }

        return temp;
    }
    
    private void updateThirst(Player player, PlayerStats stats, float currentTemp) {
        int thirst = stats.getThirst();
        
        int thirstLoss = 1;
        if (currentTemp > 30) thirstLoss = 3;
        if (player.isSprinting()) thirstLoss += 1; 
        
        stats.setThirst(Math.max(0, thirst - thirstLoss));
    }
    
    private void applyTemperatureEffects(Player player, float temp) {
        if (temp < -5.0f) {
            player.damage(1.0);
            player.sendTitle("", "§bคุณกำลังจะแข็งตาย! (-" + (int)Math.abs(temp) + "°C)", 10, 20, 10);
        } else if (temp < 5.0f) {
            player.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS, 40, 0));
        } else if (temp > 40.0f) {
            player.damage(1.0);
            player.sendTitle("", "§4คุณเป็นโรคลมแดด! (+" + (int)temp + "°C)", 10, 20, 10);
        } else if (temp > 35.0f) {
            player.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.WEAKNESS, 40, 0));
        }

        if (playerStats.get(player).getThirst() <= 10) {
            player.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.NAUSEA, 40, 0));
        }
    }

    private void sendActionBarUpdate(Player player, PlayerStats stats) {
        ChatColor tempColor;
        if (stats.getTemperature() < 5.0f) {
            tempColor = ChatColor.BLUE; 
        } else if (stats.getTemperature() > 35.0f) {
            tempColor = ChatColor.RED; 
        } else if (stats.getTemperature() > 25.0f) {
            tempColor = ChatColor.YELLOW; 
        } else {
            tempColor = ChatColor.GREEN; 
        }

        int thirstLevel = stats.getThirst();
        String thirstBar = "";
        int fullBlocks = thirstLevel / 10;
        int emptyBlocks = 10 - fullBlocks;
        
        thirstBar += ChatColor.AQUA + "💧".repeat(fullBlocks);
        thirstBar += ChatColor.DARK_GRAY + "💧".repeat(emptyBlocks);

        String message = String.format("§f[ปฏิทิน: %s%s§f - วันที่ %d]   |   [อุณหภูมิ: %s%.1f°C§f]   |   [น้ำ: %s§f]",
            currentSeason.getChatColor(), currentSeason.getThaiName(), currentDay, 
            tempColor, stats.getTemperature(), thirstBar);

        // ใช้ BukkitAudiences ในการส่ง Action Bar (แก้ไขแล้ว)
        Component component = LegacyComponentSerializer.legacySection().deserialize(message);
        Audience playerAudience = this.audiences.player(player);
        playerAudience.sendActionBar(component);
    }

    // ====================================================================================
    // PLAYER STATS CLASS
    // ====================================================================================

    private static class PlayerStats {
        private float temperature;
        private int thirst; // 0-100

        public PlayerStats(float temp, int thirst) {
            this.temperature = temp;
            this.thirst = thirst;
        }
        
        public float getTemperature() { return temperature; }
        public void setTemperature(float temperature) { this.temperature = temperature; }
        public int getThirst() { return thirst; }
        public void setThirst(int thirst) { this.thirst = thirst; }
    }
}
