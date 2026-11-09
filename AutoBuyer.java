package pl.fullbright;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AutoBuyer {
    public static String status = "";
    public static int bought = 0;

    public static boolean scanner = false;
    public static int scanTotal = 0;     // "Baza": sztuk na rynku
    public static int scanMatched = 0;   // sztuk z configu
    public static int scanInRange = 0;   // "Snajper": z configu i w widełkach cen
    public static int scanPages = 0;
    public static int scanPagesTotal = 0;
    public static boolean scanDone = false;
    private static final Map<Integer, int[]> visited = new HashMap<>();

    private static long nextAt = 0;
    private static int dir = 1;
    private static Config.Entry pending = null;
    private static long pendingAt = 0;
    private static int emptyScans = 0;
    private static final Pattern PAGE = Pattern.compile("(\\d+)\\s*/\\s*(\\d+)");

    public static String state() {
        Config c = Config.get();
        if (!c.enabled) return scanner && scanDone ? "Skan zakończony" : "Zatrzymany";
        return scanner ? "Skanuję" : "Działa";
    }

    public static void start() {
        scanner = false; pending = null; status = "";
        Config.get().enabled = true;
    }

    public static void stop() {
        Config.get().enabled = false; pending = null; status = "";
    }

    public static void startScan() {
        scanner = true; scanDone = false; visited.clear();
        scanTotal = scanMatched = scanInRange = scanPages = scanPagesTotal = 0;
        pending = null; status = "";
        Config.get().enabled = true;
    }

    static List<String> lore(ItemStack st) {
        List<String> out = new ArrayList<>();
        LoreComponent lc = st.get(DataComponentTypes.LORE);
        if (lc != null) for (Text t : lc.lines()) out.add(t.getString());
        return out;
    }

    private static void click(MinecraftClient mc, GenericContainerScreenHandler h, int slot) {
        mc.interactionManager.clickSlot(h.syncId, slot, 0, SlotActionType.PICKUP, mc.player);
    }

    private static boolean matches(Config.Entry e, String id, String lowName) {
        if (!e.enabled || !e.id.equals(id)) return false;
        return e.nameContains.isBlank() || lowName.contains(e.nameContains.toLowerCase(Locale.ROOT));
    }

    /** Zwraca {aktualna, wszystkie, 1 jeśli wskaźnik jest w dolnym rzędzie} albo null. */
    private static int[] pageInfo(GenericContainerScreen s, GenericContainerScreenHandler h, int navStart, int n) {
        Matcher m = PAGE.matcher(PriceParser.clean(s.getTitle().getString()));
        if (m.find()) return new int[]{Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), 0};
        for (int i = navStart; i < n; i++) {
            ItemStack st = h.getSlot(i).getStack();
            if (st.isEmpty()) continue;
            StringBuilder sb = new StringBuilder(PriceParser.clean(st.getName().getString()));
            for (String l : lore(st)) sb.append(' ').append(PriceParser.clean(l));
            Matcher mm = PAGE.matcher(sb);
            if (mm.find()) return new int[]{Integer.parseInt(mm.group(1)), Integer.parseInt(mm.group(2)), 1};
        }
        return null;
    }

    public static void tick(MinecraftClient mc) {
        Config c = Config.get();
        if (!c.enabled) return;
        if (mc.player == null || mc.interactionManager == null) return;
        if (!(mc.currentScreen instanceof GenericContainerScreen s)) { status = "Otwórz /ah"; return; }
        if (!s.getTitle().getString().toLowerCase(Locale.ROOT).contains(c.marketTitle.toLowerCase(Locale.ROOT))) {
            status = "To nie jest rynek"; return;
        }
        long now = System.currentTimeMillis();
        if (now < nextAt) return;

        GenericContainerScreenHandler h = s.getScreenHandler();
        int rows = h.getRows();
        int n = Math.min(rows * 9, h.slots.size());
        int navStart = (rows - 1) * 9;

        // 1) ekran potwierdzenia -> klik ZAKUP
        if (!scanner) {
            for (int i = 0; i < n; i++) {
                ItemStack st = h.getSlot(i).getStack();
                if (st.isEmpty()) continue;
                String nm = PriceParser.clean(st.getName().getString()).toLowerCase(Locale.ROOT);
                if (nm.contains("zakup") || nm.contains("potwierd")) {
                    if (pending != null && now - pendingAt < 5000) {
                        click(mc, h, i);
                        bought++;
                        status = "Kupiono: " + pending.id + " (" + bought + ")";
                        pending = null;
                        nextAt = now + c.pageDelayMs;
                    } else {
                        status = "Ręczne potwierdzenie - pomijam";
                        nextAt = now + 500;
                    }
                    return;
                }
            }
            if (pending != null && now - pendingAt > 3000) pending = null;
        }

        if (rows < 5) { status = "Czekam na rynek..."; nextAt = now + 100; return; }

        // 2) strona + sloty (wykrywane automatycznie)
        int[] pg = pageInfo(s, h, navStart, n);
        int cur = pg == null ? -1 : pg[0], total = pg == null ? -1 : pg[1];
        boolean hasNav = pg != null && pg[2] == 1;
        int first = hasNav ? 9 : 0, last = navStart - 1;

        int prevSlot = navStart, nextSlot = n - 1;
        for (int i = navStart; i < n; i++) {
            ItemStack st = h.getSlot(i).getStack();
            if (st.isEmpty()) continue;
            String nm = PriceParser.clean(st.getName().getString()).toLowerCase(Locale.ROOT);
            if (nm.contains("nast") || nm.contains("next") || nm.contains("dalej") || nm.contains("→")) nextSlot = i;
            else if (nm.contains("poprz") || nm.contains("prev") || nm.contains("wstecz") || nm.contains("←")) prevSlot = i;
        }

        // 3) skan strony
        int nonEmpty = 0, pTotal = 0, pMatched = 0, pInRange = 0;
        for (int i = first; i <= last; i++) {
            ItemStack st = h.getSlot(i).getStack();
            if (st.isEmpty()) continue;
            nonEmpty++;
            pTotal += st.getCount();
            String id = Registries.ITEM.getId(st.getItem()).toString();
            if (scanner) Config.addSeen(id, PriceParser.clean(st.getName().getString()));
            String name = PriceParser.clean(st.getName().getString()).toLowerCase(Locale.ROOT);
            for (Config.Entry e : c.entries) {
                if (!matches(e, id, name)) continue;
                long price = PriceParser.priceFromLore(lore(st), c.priceKeyword);
                boolean inRange = price >= 0 && price >= e.min && price <= e.max;
                if (scanner) {
                    pMatched += st.getCount();
                    if (inRange) pInRange += st.getCount();
                    break;
                }
                if (inRange) {
                    click(mc, h, i);
                    pending = e;
                    pendingAt = now;
                    nextAt = now + c.buyDelayMs;
                    status = "Kupuję " + id + " za " + PriceParser.format(price);
                    return;
                }
            }
        }

        if (nonEmpty == 0 && emptyScans++ < 6) { status = "Ładowanie..."; nextAt = now + Math.max(100, c.pageDelayMs); return; }
        emptyScans = 0;

        if (scanner) {
            if (cur < 0 || total < 0) {
                visited.put(1, new int[]{pTotal, pMatched, pInRange});
                recalc(1);
                finishScan();
                return;
            }
            visited.put(cur, new int[]{pTotal, pMatched, pInRange});
            recalc(total);
            if (visited.size() >= total) { finishScan(); return; }
        }

        // 4) przewijanie
        if (scanner) {
            if (total > 0 && cur >= total) dir = -1;
            else if (cur == 1) dir = 1;
            else if (cur > 0 && visited.containsKey(cur + 1) && !visited.containsKey(cur - 1)) dir = -1;
            else if (cur > 0 && !visited.containsKey(cur + 1)) dir = 1;
        } else {
            switch (c.direction) {
                case "RIGHT" -> dir = 1;
                case "LEFT" -> dir = -1;
                default -> {
                    if (total > 0 && cur >= total) dir = -1;
                    else if (cur == 1) dir = 1;
                }
            }
        }
        click(mc, h, dir > 0 ? nextSlot : prevSlot);
        status = "Strona " + (cur > 0 ? cur + "/" + total : "?") + (dir > 0 ? " ->" : " <-");
        nextAt = now + c.pageDelayMs;
    }

    private static void recalc(int totalPages) {
        scanTotal = scanMatched = scanInRange = 0;
        for (int[] v : visited.values()) { scanTotal += v[0]; scanMatched += v[1]; scanInRange += v[2]; }
        scanPages = visited.size();
        scanPagesTotal = totalPages;
    }

    private static void finishScan() {
        scanDone = true;
        Config.get().enabled = false;
        Config.save();
        status = "Skan zakończony";
    }
}
