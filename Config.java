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

    public List<Entry> entries = new ArrayList<>();
    public boolean enabled = false;
    public int pageDelayMs = 250;     // czas przewijania strony
    public int buyDelayMs = 150;      // czas od kliknięcia itemu do kliknięcia ZAKUP
    public String direction = "BOUNCE"; // RIGHT, LEFT, BOUNCE
    public int prevSlot = 45;         // strzałka w lewo
    public int nextSlot = 53;         // strzałka w prawo
    public int pageSlot = 49;         // wskaźnik strony "1/81"
    public int firstItemSlot = 9;
    public int lastItemSlot = 44;
    public String marketTitle = "RYNEK";
    public String priceKeyword = "";  // puste = auto ($ / cena / price)

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
            for (Entry e : c.entries) if (e.nameContains == null) e.nameContains = "";
            if (c.direction == null) c.direction = "BOUNCE";
            if (c.marketTitle == null) c.marketTitle = "RYNEK";
            if (c.priceKeyword == null) c.priceKeyword = "";
            c.enabled = false;
            I = c;
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static void save() {
        try { Files.writeString(path(), G.toJson(I)); } catch (Exception ignored) {}
    }

    public static void reset() { I = new Config(); save(); }
}
