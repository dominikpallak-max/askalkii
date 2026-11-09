package pl.fullbright;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Config {
    public static class Entry {
        public String id = "minecraft:diamond";
        public String nameContains = "";
        public long min = 0;
        public long max = 1_000_000;
        public boolean enabled = true;
    }

    public static class Seen {
        public String id;
        public String name;
    }

    public List<Entry> entries = new ArrayList<>();
    public List<Seen> catalog = new ArrayList<>();
    public int pageDelayMs = 100;
    public int buyDelayMs = 50;
    public String direction = "BOUNCE"; // RIGHT, LEFT, BOUNCE
    public String marketTitle = "RYNEK";
    public String priceKeyword = "";

    /** AutoRynek ON/OFF w menu (pokazuje przyciski w rynku). Nie zapisuje się do pliku. */
    public transient boolean module = false;
    /** Czy autorynek aktualnie pracuje (Start/Skanuj). */
    public transient boolean enabled = false;

    private static Config I = new Config();
    private static final Gson G = new GsonBuilder().setPrettyPrinting().create();

    public static Config get() { return I; }
    public static Path path() { return FabricLoader.getInstance().getConfigDir().resolve("fullbright.json"); }

    public static boolean load() {
        try {
            if (!Files.exists(path())) { save(); return true; }
            Config c = G.fromJson(Files.readString(path()), Config.class);
            if (c == null) c = new Config();
            if (c.entries == null) c.entries = new ArrayList<>();
            if (c.catalog == null) c.catalog = new ArrayList<>();
            for (Entry e : c.entries) if (e.nameContains == null) e.nameContains = "";
            if (c.direction == null) c.direction = "BOUNCE";
            if (c.marketTitle == null) c.marketTitle = "RYNEK";
            if (c.priceKeyword == null) c.priceKeyword = "";
            c.module = I.module;
            I = c;
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static void addSeen(String id, String name) {
        for (Seen x : I.catalog) if (x.id.equals(id) && x.name.equals(name)) return;
        if (I.catalog.size() >= 800) return;
        Seen x = new Seen();
        x.id = id; x.name = name;
        I.catalog.add(x);
    }

    public static void save() {
        try { Files.writeString(path(), G.toJson(I)); } catch (Exception ignored) {}
    }

    public static void reset() {
        boolean m = I.module;
        I = new Config();
        I.module = m;
        save();
    }
}
