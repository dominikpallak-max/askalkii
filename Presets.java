package pl.fullbright;

import java.util.ArrayList;
import java.util.List;

public class Presets {
    public static final List<Config.Seen> ALL = new ArrayList<>();

    private static void add(String id, String name) {
        Config.Seen s = new Config.Seen();
        s.id = id;
        s.name = name;
        ALL.add(s);
    }

    static {
        add("minecraft:diamond", "Diament");
        add("minecraft:emerald", "Szmaragd");
        add("minecraft:netherite_ingot", "Sztabka netherytu");
        add("minecraft:gold_ingot", "Sztabka złota");
        add("minecraft:iron_ingot", "Sztabka żelaza");
        add("minecraft:ender_pearl", "Perła Endermana");
        add("minecraft:golden_apple", "Złote jabłko");
        add("minecraft:enchanted_golden_apple", "Zaklęte złote jabłko");
        add("minecraft:totem_of_undying", "Totem nieśmiertelności");
        add("minecraft:elytra", "Elytra");
    }
}
