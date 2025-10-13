package com.nonkungch.dynamicsurvival;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SeasonProcessor extends BukkitRunnable {

    private final DynamicSurvival plugin;
    private final Season newSeason;
    private final World world;
    
    // Material ของใบไม้ที่ใช้ในการประมวลผล
    private static final List<Material> LEAF_MATERIALS = Arrays.asList(
        Material.OAK_LEAVES, Material.SPRUCE_LEAVES, Material.BIRCH_LEAVES, 
        Material.JUNGLE_LEAVES, Material.ACACIA_LEAVES, Material.DARK_OAK_LEAVES
    );

    public SeasonProcessor(DynamicSurvival plugin, Season newSeason) {
        this.plugin = plugin;
        this.newSeason = newSeason;
        // ใช้โลกหลัก (โลกแรกที่โหลด)
        this.world = plugin.getServer().getWorlds().get(0); 
    }

    @Override
    public void run() {
        if (world == null) return;

        plugin.getLogger().info("กำลังเริ่มต้นประมวลผลการเปลี่ยนแปลงโลกสำหรับฤดูกาล: " + newSeason.toString());

        // กำหนดขอบเขตการทำงาน (สแกน 5 ชิ้นส่วนรอบ ๆ ผู้เล่นที่ออนไลน์)
        Set<org.bukkit.Chunk> chunksToProcess = new HashSet<>();
        world.getPlayers().forEach(p -> {
            org.bukkit.Chunk centerChunk = p.getLocation().getChunk();
            // สแกนชิ้นส่วนใกล้เคียง (3x3 area)
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    chunksToProcess.add(world.getChunkAt(centerChunk.getX() + dx, centerChunk.getZ() + dz));
                }
            }
        });

        // ถ้าไม่มีผู้เล่นออนไลน์ ให้สแกนชิ้นส่วนศูนย์กลางโลก 
        if (chunksToProcess.isEmpty()) {
             chunksToProcess.add(world.getChunkAt(0, 0));
        }

        // ประมวลผลแต่ละ Chunk (Note: This still runs on the main thread and can cause lag)
        for (org.bukkit.Chunk chunk : chunksToProcess) {
             processChunk(chunk);
        }
        
        plugin.getLogger().info("การประมวลผลฤดูกาลสำหรับ " + newSeason.toString() + " เสร็จสมบูรณ์แล้ว.");
    }

    private void processChunk(org.bukkit.Chunk chunk) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                // สแกนเฉพาะบล็อกที่มีความสูง 60 ขึ้นไป
                for (int y = world.getMaxHeight() - 1; y > 60; y--) {
                    Block block = chunk.getBlock(x, y, z);
                    
                    if (LEAF_MATERIALS.contains(block.getType())) {
                        handleLeaves(block);
                    } else {
                        handleBiomeEffects(block);
                    }
                }
            }
        }
    }
    
    private void handleLeaves(Block block) {
         if (newSeason == Season.AUTUMN) {
            // ฤดูใบไม้ร่วง: ใบไม้มีโอกาสร่วง (40% Chance ที่ใบไม้จะหายไป)
            if (plugin.random.nextDouble() < 0.4) { 
                block.setType(Material.AIR);
            }
        }
        // เราจะไม่จัดการการเติบโตของใบไม้ใน SPRING ตรงนี้ เพราะมันซับซ้อนมาก
        // จะใช้ผลทางอ้อมจากการละลายของหิมะแทน
    }
    
    private void handleBiomeEffects(Block block) {
        Biome biome = block.getBiome();
        boolean isColdBiome = biome.toString().contains("SNOW") || biome.toString().contains("TAIGA");
        
        if (block.getType() == Material.AIR) return;

        // บล็อกที่อยู่บนสุด (สำหรับหิมะ)
        if (newSeason == Season.WINTER) {
            // ฤดูหนาว: หิมะตกในทุกไบโอมที่ไม่ได้ร้อนจัด
            if (block.getY() > 60 && block.getRelative(0, 1, 0).getType() == Material.AIR) {
                // ต้องเป็นบล็อกที่เหมาะสมในการรองรับหิมะ
                if (block.getType().isSolid()) {
                    Block blockAbove = block.getRelative(0, 1, 0);
                    // ป้องกันการทับซ้อนกับบล็อกอื่นๆ เช่น ดอกไม้
                    if (blockAbove.getType() == Material.AIR) {
                         blockAbove.setType(Material.SNOW);
                    }
                }
            }
        } else if (newSeason == Season.SPRING || newSeason == Season.SUMMER) {
            // ฤดูใบไม้ผลิ/ร้อน: หิมะจะละลาย (เฉพาะ Snow Layers)
            Block blockAbove = block.getRelative(0, 1, 0);
            if (blockAbove.getType() == Material.SNOW) {
                // ละลายหิมะที่ไม่ใช่ไบโอมหนาวเย็นถาวร
                if (!isColdBiome) { 
                    blockAbove.setType(Material.AIR);
                }
            }
        }
    }
}
