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
    public static String status = "Wyłączony";
    public static int bought = 0;

    // --- tryb skanera ---
    public static boolean scanner = false;
    public static int scanTotal = 0;     // sztuk na rynku (suma ze skanowanych stron)
    public static int scanMatched = 0;   // sztuk pasujących do configu (id/nazwa)
    public static int scanInRange = 0;   // sztuk pasujących do configu I w widełkach cen (snajp)
    public static int scanPages = 0;     // ile stron przeskanowano
    public static int scanPagesTotal = 0;
    public static boolean scanDone = false;
    private static final Map<Integer, int[]> visited = new HashMap<>();

    private static long nextAt = 0;
    private static int dir = 1;
    private static Config.Entry pending = null;
    private static long pendingAt = 0;
    private static int emptyScans = 0;
    private static final Pattern PAGE = Pattern.compile("(\\d+)\\s*/\\s*(\\d+)");

    // ---- sterowanie z przycisków ----
    public static void start() {
        scanner = false;
        pending = null;
        Config.get().enabled = true;
    }

    public static void stop() {
        Config.get().enabled = false;
        pending = null;
        status = "Zatrzymano";
    }

    public static void startScan() {
        scanner = true;
        scanDone = false;
        visited.clear();
        scanTotal = scanMatched = scanInRange = scanPages = scanPagesTotal = 0;
        pending = null;
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

    public static void tick(MinecraftClient mc) {
        Config c = Config.get();
        if (!c.enabled) { if (!scanner || !scanDone) status = "Wyłączony"; return; }
        if (mc.player == null || mc.interactionManager == null) return;
        if (!(mc.currentScreen instanceof GenericContainerScreen s)) { status = "Otwórz /ah"; return; }
        if (!s.getTitle().getString().toLowerCase(Locale.ROOT).contains(c.marketTitle.toLowerCase(Locale.ROOT))) {
            status = "To nie jest rynek"; return;
        }
        long now = System.currentTimeMillis();
        if (now < nextAt) return;

        GenericContainerScreenHandler h = s.getScreenHandler();
        int n = Math.min(h.getRows() * 9, h.slots.size());

        // 1) ekran potwierdzenia -> klik ZAKUP (tylko tryb kupowania)
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

        // numer strony
        int cur = -1, total = -1;
        if (c.pageSlot >= 0 && c.pageSlot < n) {
            ItemStack ps = h.getSlot(c.pageSlot).getStack();
            StringBuilder sb = new StringBuilder(PriceParser.clean(ps.getName().getString()));
            for (String l : lore(ps)) sb.append(' ').append(PriceParser.clean(l));
            Matcher m = PAGE.matcher(sb);
            if (m.find()) { cur = Integer.parseInt(m.group(1)); total = Integer.parseInt(m.group(2)); }
        }

        // 2) skan strony
        int first = Math.max(0, c.firstItemSlot), last = Math.min(c.lastItemSlot, n - 1);
        int nonEmpty = 0, pTotal = 0, pMatched = 0, pInRange = 0;
        for (int i = first; i <= last; i++) {
            ItemStack st = h.getSlot(i).getStack();
            if (st.isEmpty()) continue;
            nonEmpty++;
            pTotal += st.getCount();
            String id = Registries.ITEM.getId(st.getItem()).toString();
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

        if (nonEmpty == 0 && emptyScans++ < 6) { status = "Ładowanie..."; nextAt = now + c.pageDelayMs; return; }
        emptyScans = 0;

        // 2b) skaner: zapisz wyniki strony i sprawdź czy koniec
        if (scanner) {
            if (cur < 0 || total < 0) { // nie znamy stron -> jedna strona
                visited.put(1, new int[]{pTotal, pMatched, pInRange});
                recalc(1);
                finishScan();
                return;
            }
            visited.put(cur, new int[]{pTotal, pMatched, pInRange});
            recalc(total);
            if (visited.size() >= total) { finishScan(); return; }
        }

        // 3) przewijanie
        if (scanner) {
            // skaner: idź w prawo do końca, potem w lewo po brakujące strony
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
        int slot = dir > 0 ? c.nextSlot : c.prevSlot;
        if (slot >= 0 && slot < n) click(mc, h, slot);
        status = (scanner ? "Skanuję " : "Strona ") + (cur > 0 ? cur + "/" + total : "?") + (dir > 0 ? " ->" : " <-");
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
        status = "Skan zakończony";
    }
}
