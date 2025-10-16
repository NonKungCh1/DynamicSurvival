// /src/main/java/com/nonkungch/dynamicsurvival/Season.java

package com.nonkungch.dynamicsurvival;

public enum Season {
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

    public Season next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public String getThaiName() {
        return thaiName;
    }

    public String getChatColor() {
        return chatColor;
    }

    public void processSeasonStart(DynamicSurvival plugin) {
        new SeasonProcessor(plugin, this).runTask(plugin);
    }
}
