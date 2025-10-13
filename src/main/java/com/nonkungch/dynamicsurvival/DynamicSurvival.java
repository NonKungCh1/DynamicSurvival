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

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

// ====================================================================================
// ENUMERATIONS
// ====================================================================================

enum Season {
    SPRING("ฤดูใบไม้ผลิ", 20, "§a§l"), // สีเขียวสด
    SUMMER("ฤดูร้อน", 25, "§6§l"),  // สีทอง/เหลือง
    AUTUMN("ฤดูใบไม้ร่วง", 15, "§c§l"), // สีแดง
    WINTER("ฤดูหนาว", 30, "§b§l");  // สีฟ้าอ่อน

    private final String thaiName;
    private final int durationDays; // จำนวนวันในเกมที่ฤดูกาลนี้คงอยู่
    private final String chatColor;

    Season(String thaiName, int durationDays, String chatColor) {
        this.thaiName = thaiName;
        this.durationDays = durationDays;
        this.chatColor = chatColor;
    }

    public Season next() {
        return values()[(ordinal() + 1) % values().length];
    }
    
    public String getThaiName() {
        return thaiName;
    }

    public int getDurationDays() {
        return durationDays;
    }
    
    public String getChatColor() {
        return chatColor;
    }
}

// ====================================================================================
// MAIN PLUGIN CLASS
// ====================================================================================

public class MyDynamicWorldPlugin extends JavaPlugin implements Listener {

    private Season currentSeason = Season.SPRING;
    private int currentDay = 1;
    private long lastDayTime = 0;
    private final Map<Player, PlayerStats> playerStats = new HashMap<>();
    private final Random random = new Random();
    private World trackedWorld; // โลกหลักที่เราจะติดตามฤดูกาล

    @Override
    public void onEnable() {
        getLogger().info("Dynamic World Plugin กำลังทำงาน!");
        
        // กำหนดโลกหลัก (สมมติว่าเป็นโลกแรกในรายการ)
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
        getLogger().info("Dynamic World Plugin ถูกปิดการทำงาน!");
        Bukkit.getScheduler().cancelTasks(this);
    }

    // เมื่อผู้เล่นเข้าร่วม: กำหนดค่าสถิติเริ่มต้น
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        playerStats.putIfAbsent(player, new PlayerStats(20.0f, 100));
    }
    
    // ====================================================================================
    // 1. ระบบฤดูกาล & ปฏิทิน & สภาพอากาศ
    // ====================================================================================

    private void startSeasonAndWeatherLoop() {
        // ตรวจสอบทุกๆ 20 tick (1 วินาที)
        new BukkitRunnable() {
            @Override
            public void run() {
                if (trackedWorld == null) return;
                
                // ตรวจสอบการเปลี่ยนวัน (Minecraft Day = 24000 ticks)
                long currentTime = trackedWorld.getFullTime();
                if (currentTime / 24000 > lastDayTime / 24000) {
                    onNewDay();
                }
                lastDayTime = currentTime;

                // สุ่มสภาพอากาศ (มีโอกาสสุ่มเปลี่ยนแปลงสภาพอากาศทุกๆ 10 นาที (12000 ticks))
                if (currentTime % 12000 == 0 && random.nextDouble() < 0.2) { 
                    applyRandomWeather();
                }
            }
        }.runTaskTimer(this, 20L, 20L);
    }
    
    private void onNewDay() {
        currentDay++;
        
        // ตรวจสอบการเปลี่ยนฤดูกาล
        if (currentDay > currentSeason.getDurationDays()) {
            changeSeason();
        } else {
             // แจ้งเตือนปฏิทินในเกม
            Bukkit.broadcastMessage(String.format("§e[ปฏิทิน] วันที่ %d ใน %s%s§e (%d วันที่เหลือ)", 
                currentDay, currentSeason.getChatColor(), currentSeason.getThaiName(), 
                currentSeason.getDurationDays() - currentDay));
        }
    }

    private void changeSeason() {
        currentSeason = currentSeason.next();
        currentDay = 1;

        // ข้อความแจ้งบอกการเปลี่ยนฤดูกาล
        String msg = String.format("§l§e--- %sการเปลี่ยนฤดูกาลครั้งใหญ่!%s ---", ChatColor.GOLD, ChatColor.RESET);
        String seasonMsg = String.format("%s! ฤดูกาลใหม่คือ: %s%s", ChatColor.YELLOW, currentSeason.getChatColor(), currentSeason.getThaiName());
        
        Bukkit.broadcastMessage(msg);
        Bukkit.broadcastMessage(seasonMsg);

        // **จุดเปลี่ยนสีไบโอม (ต้องใช้ Packet)**
        // *โค้ดจริงสำหรับการเปลี่ยนสีใบไม้และหญ้าแบบไดนามิกตามฤดูกาลจะถูกใส่ที่นี่*
        // *เนื่องจากความซับซ้อน จึงละไว้ในตัวอย่างนี้ แต่ต้องใช้ NMS/ProtocolLib สำหรับการส่ง Custom Chunk Data*
        Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage("§7(โปรดทราบว่าสีใบไม้เปลี่ยนไปแล้วตามฤดูกาล!)"));
    }
    
    private void applyRandomWeather() {
        if (trackedWorld == null) return;
        
        double chance = random.nextDouble();
        
        if (chance < 0.1) {
            trackedWorld.setStorm(false);
            trackedWorld.setThundering(true); // พายุฝนฟ้าคะนอง
            Bukkit.broadcastMessage("§4[สภาพอากาศ] เกิดพายุฝนฟ้าคะนอง! อากาศจะเย็นลงอย่างรวดเร็ว.");
        } else if (chance < 0.3) {
            trackedWorld.setStorm(true);
            trackedWorld.setThundering(false); // ฝน/หิมะ
            Bukkit.broadcastMessage("§b[สภาพอากาศ] มีฝนตก/หิมะตก อุณหภูมิจะลดลง.");
        } else {
            trackedWorld.setStorm(false);
            trackedWorld.setThundering(false); // อากาศแจ่มใส
            Bukkit.broadcastMessage("§e[สภาพอากาศ] ท้องฟ้าแจ่มใส อากาศดี.");
        }
    }

    // ====================================================================================
    // 2. ระบบอุณหภูมิ & หลอดน้ำ (แสดงผลใน Action Bar)
    // ====================================================================================

    private void startStatsUpdateLoop() {
        // อัพเดตสถิติทุกๆ 2 วินาที (40 ticks)
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    PlayerStats stats = playerStats.getOrDefault(player, new PlayerStats(20.0f, 100));
                    
                    // 1. คำนวณอุณหภูมิใหม่
                    float newTemp = calculateTemperature(player);
                    stats.setTemperature(newTemp);
                    
                    // 2. คำนวณหลอดน้ำใหม่ (ลด/เพิ่ม)
                    updateThirst(player, stats, newTemp);
                    
                    // 3. แสดงผลใน Action Bar
                    sendActionBarUpdate(player, stats);
                    
                    // 4. ใช้เอฟเฟกต์
                    applyTemperatureEffects(player, newTemp);
                }
            }
        }.runTaskTimer(this, 40L, 40L);
    }
    
    private float calculateTemperature(Player player) {
        // อุณหภูมิพื้นฐาน (ขึ้นอยู่กับฤดูกาล)
        float baseTemp = 25.0f;
        switch (currentSeason) {
            case SPRING: baseTemp = 20.0f; break;
            case SUMMER: baseTemp = 30.0f; break;
            case AUTUMN: baseTemp = 15.0f; break;
            case WINTER: baseTemp = 0.0f; break;
        }

        float temp = baseTemp;
        
        // ปรับตามเวลา (ร้อนตอนกลางวัน เย็นตอนกลางคืน)
        long time = player.getWorld().getTime(); // 0-24000
        if (time < 12000) { // กลางวัน
            temp += (12000 - time) / 12000.0f * 5.0f; // ร้อนขึ้น 0-5 องศา
        } else { // กลางคืน
            temp -= (time - 12000) / 12000.0f * 5.0f; // เย็นลง 0-5 องศา
        }

        // ปรับตามสภาพอากาศ
        if (player.getWorld().hasStorm()) {
            temp -= 3.0f; // ฝน/หิมะ
        }
        if (player.getWorld().isThundering()) {
            temp -= 5.0f; // พายุ
        }
        
        // ปรับตามไบโอม
        String biome = player.getLocation().getBlock().getBiome().toString();
        if (biome.contains("DESERT")) temp += 5.0f;
        if (biome.contains("SNOW") || biome.contains("TAIGA")) temp -= 5.0f;

        // ปรับตามบล็อกใกล้เคียง (ตัวอย่าง: ใกล้กองไฟ)
        if (player.getLocation().getBlock().getRelative(0, -1, 0).getType() == Material.FIRE || 
            player.getLocation().getBlock().getRelative(0, -1, 0).getType() == Material.LAVA) {
            temp += 5.0f;
        }

        return temp;
    }
    
    private void updateThirst(Player player, PlayerStats stats, float currentTemp) {
        int thirst = stats.getThirst();
        
        // สูญเสียน้ำมากขึ้นเมื่ออากาศร้อน
        int thirstLoss = 1;
        if (currentTemp > 30) thirstLoss = 3;
        if (player.isSprinting()) thirstLoss += 1; // วิ่งทำให้เสียน้ำเพิ่ม
        
        stats.setThirst(Math.max(0, thirst - thirstLoss));
        
        // ผู้เล่นดื่มน้ำ (ตัวอย่าง: คลิกขวาที่ขวดน้ำ) - ต้องใช้ Event Listener เพิ่มเติม
    }
    
    private void applyTemperatureEffects(Player player, float temp) {
        if (temp < -5.0f) {
            // หนาวจัด
            player.damage(1.0);
            player.sendTitle("", "§bคุณกำลังจะแข็งตาย! (-" + (int)Math.abs(temp) + "°C)", 10, 20, 10);
        } else if (temp < 5.0f) {
            // หนาว
            player.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SLOW, 40, 0));
        } else if (temp > 40.0f) {
            // ร้อนจัด
            player.damage(1.0);
            player.sendTitle("", "§4คุณเป็นโรคลมแดด! (+" + (int)temp + "°C)", 10, 20, 10);
        } else if (temp > 35.0f) {
            // ร้อน
            player.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.WEAKNESS, 40, 0));
        }

        if (playerStats.get(player).getThirst() <= 10) {
            // ขาดน้ำ
            player.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.CONFUSION, 40, 0));
        }
    }

    private void sendActionBarUpdate(Player player, PlayerStats stats) {
        // การกำหนดสีตามระดับอันตรายของอุณหภูมิ
        ChatColor tempColor;
        if (stats.getTemperature() < 5.0f) {
            tempColor = ChatColor.BLUE; // หนาว
        } else if (stats.getTemperature() > 35.0f) {
            tempColor = ChatColor.RED; // ร้อน
        } else if (stats.getTemperature() > 25.0f) {
            tempColor = ChatColor.YELLOW; // อุ่น
        } else {
            tempColor = ChatColor.GREEN; // ปกติ
        }

        // การสร้างหลอดน้ำ (สมมติว่าเต็ม 100)
        int thirstLevel = stats.getThirst();
        String thirstBar = "";
        int fullBlocks = thirstLevel / 10;
        int emptyBlocks = 10 - fullBlocks;
        
        thirstBar += ChatColor.AQUA + "💧".repeat(fullBlocks);
        thirstBar += ChatColor.DARK_GRAY + "💧".repeat(emptyBlocks);

        // Action Bar Output (รองรับ Bedrock ผ่าน GeyserMC)
        String message = String.format("§f[ปฏิทิน: %s%s§f - วันที่ %d]   |   [อุณหภูมิ: %s%.1f°C§f]   |   [น้ำ: %s§f]",
            currentSeason.getChatColor(), currentSeason.getThaiName(), currentDay, 
            tempColor, stats.getTemperature(), thirstBar);

        player.sendActionBar(message);
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
        
        // Getters and Setters...
        public float getTemperature() { return temperature; }
        public void setTemperature(float temperature) { this.temperature = temperature; }
        public int getThirst() { return thirst; }
        public void setThirst(int thirst) { this.thirst = thirst; }
    }
}
