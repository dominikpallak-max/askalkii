package pl.fullbright;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

public class MenuScreen extends Screen {
    static final int W = 440, H = 262;
    static final int BG = 0xFF0E1016, PANEL = 0xFF141720, HEAD = 0xFF1A1230, CARD = 0xFF1C2030;
    static final int ACCENT = 0xFFFFC83D, MUTED = 0xFF8B93A5, WHITE = 0xFFFFFFFF;

    record Label(int x, int y, String t, int c) {}
    record Cell(int x, int y, ItemStack s) {}
    record Opt(Item item, String name) {}

    int tab = 0, px, py, gridPage = 0, listPage = 0, gridPages = 1, listPages = 1, source = 0;
    String search = "", status = "", pageText = "";
    Config.Entry editing;
    Item selItem;
    String eMin = "0", eMax = "1m", eName = "";
    final List<Label> labels = new ArrayList<>();
    final List<Cell> cells = new ArrayList<>();
    final List<int[]> cards = new ArrayList<>();
    final List<Cell> gridCells = new ArrayList<>();
    final List<ClickableWidget> gridWidgets = new ArrayList<>();
    List<Opt> filtered = new ArrayList<>();
    ButtonWidget toggleBtn;

    public MenuScreen() { super(Text.literal("FullBright")); }

    @Override public boolean shouldPause() { return false; }
    @Override public void renderBackground(DrawContext c, int x, int y, float d) {}
    @Override public void removed() { Config.save(); }

    static ItemStack stackOf(String id, String name) {
        Identifier i = Identifier.tryParse(id);
        if (i == null) return ItemStack.EMPTY;
        ItemStack st = new ItemStack(Registries.ITEM.get(i));
        if (name != null && !name.isEmpty()) st.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name));
        return st;
    }

    @Override
    protected void init() {
        px = (width - W) / 2;
        py = Math.max(2, (height - H) / 2);
        labels.clear(); cells.clear(); cards.clear(); gridCells.clear(); gridWidgets.clear(); pageText = "";
        String[] names = {"Lista", "Dodaj", "AutoRynek", "Config"};
        for (int i = 0; i < 4; i++) {
            final int t = i;
            addDrawableChild(FlatButton.btn(px + 12 + i * 104, py + 34, 100, 20, names[i], tab == i,
                    b -> { tab = t; editing = null; selItem = null; status = ""; clearAndInit(); }));
        }
        toggleBtn = FlatButton.btn(px + W - 150, py + 4, 138, 18, "", false, b -> {
            Config c = Config.get();
            c.module = !c.module;
            if (!c.module) AutoBuyer.stop();
            refreshToggle();
            status = c.module ? "Włączone - otwórz /ah i kliknij Start" : "AutoRynek wyłączony";
        });
        refreshToggle();
        addDrawableChild(toggleBtn);

        if (editing != null || selItem != null) { buildEditor(); return; }
        switch (tab) {
            case 0 -> buildList();
            case 1 -> buildAdd();
            case 2 -> buildSettings();
            default -> buildConfig();
        }
    }

    void refreshToggle() {
        FlatButton.mark(toggleBtn, "AutoRynek: " + (Config.get().module ? "ON" : "OFF"), false);
    }

    TextFieldWidget field(int x, int y, int w, String val, Consumer<String> onChange) {
        TextFieldWidget f = new TextFieldWidget(textRenderer, x, y, w, 16, Text.empty());
        f.setMaxLength(64);
        f.setText(val);
        f.setChangedListener(onChange);
        addDrawableChild(f);
        return f;
    }

    TextFieldWidget intField(int x, int y, int w, int val, IntConsumer set) {
        return field(x, y, w, String.valueOf(val), s -> {
            try { set.accept(Math.max(0, Integer.parseInt(s.trim()))); } catch (Exception ignored) {}
        });
    }

    void label(int x, int y, String t) { labels.add(new Label(x, y, t, MUTED)); }

    // ================= LISTA =================
    void buildList() {
        List<Config.Entry> es = Config.get().entries;
        int per = 5;
        listPages = Math.max(1, (es.size() + per - 1) / per);
        listPage = Math.min(listPage, listPages - 1);
        if (es.isEmpty()) {
            label(px + 20, py + 74, "Lista jest pusta.");
            label(px + 20, py + 88, "Wejdź w zakładkę 'Dodaj' i wybierz przedmioty do kupowania.");
        }
        for (int r = 0; r < per; r++) {
            int idx = listPage * per + r;
            if (idx >= es.size()) break;
            final Config.Entry e = es.get(idx);
            int y = py + 62 + r * 31;
            cards.add(new int[]{px + 12, y, W - 24, 28});
            String title = e.nameContains.isEmpty() ? stackOf(e.id, "").getName().getString() : e.nameContains;
            cells.add(new Cell(px + 20, y + 6, stackOf(e.id, e.nameContains)));
            labels.add(new Label(px + 44, y + 5, title, WHITE));
            labels.add(new Label(px + 44, y + 16, "Min " + PriceParser.format(e.min)
                    + "   Max " + PriceParser.format(e.max), 0xFF7DE29B));
            addDrawableChild(FlatButton.btn(px + W - 168, y + 6, 40, 16, e.enabled ? "ON" : "OFF", false,
                    b -> { e.enabled = !e.enabled; Config.save(); clearAndInit(); }));
            addDrawableChild(FlatButton.btn(px + W - 122, y + 6, 56, 16, "Edytuj", false, b -> {
                editing = e; eMin = PriceParser.format(e.min); eMax = PriceParser.format(e.max);
                eName = e.nameContains; clearAndInit();
            }));
            addDrawableChild(FlatButton.btn(px + W - 62, y + 6, 46, 16, "Usuń", false,
                    b -> { es.remove(e); Config.save(); clearAndInit(); }));
        }
        addDrawableChild(FlatButton.btn(px + 12, py + H - 36, 40, 16, "<", false,
                b -> { if (listPage > 0) { listPage--; clearAndInit(); } }));
        addDrawableChild(FlatButton.btn(px + W - 52, py + H - 36, 40, 16, ">", false,
                b -> { if (listPage < listPages - 1) { listPage++; clearAndInit(); } }));
        pageText = (listPage + 1) + "/" + listPages + "   (" + es.size() + " przedm.)";
    }

    // ================= DODAJ =================
    static final String[] SRC = {"Serwer", "Minecraft", "Rynek (skan)"};

    void buildAdd() {
        TextFieldWidget f = new TextFieldWidget(textRenderer, px + 14, py + 62, 270, 16, Text.empty());
        f.setMaxLength(40);
        f.setPlaceholder(Text.literal("Szukaj przedmiotu..."));
        f.setText(search);
        f.setChangedListener(s -> { search = s; gridPage = 0; rebuildGrid(); });
        addDrawableChild(f);
        addDrawableChild(FlatButton.btn(px + W - 146, py + 62, 132, 16, "Źródło: " + SRC[source], true,
                b -> { source = (source + 1) % 3; gridPage = 0; clearAndInit(); }));
        if (source == 2 && Config.get().catalog.isEmpty())
            label(px + 20, py + 100, "Brak itemów z rynku. Włącz AutoRynek, otwórz /ah i kliknij 'Skanuj'.");
        addDrawableChild(FlatButton.btn(px + 12, py + H - 36, 40, 16, "<", false,
                b -> { if (gridPage > 0) { gridPage--; rebuildGrid(); } }));
        addDrawableChild(FlatButton.btn(px + W - 52, py + H - 36, 40, 16, ">", false,
                b -> { if (gridPage < gridPages - 1) { gridPage++; rebuildGrid(); } }));
        rebuildGrid();
    }

    void rebuildGrid() {
        for (ClickableWidget w : gridWidgets) remove(w);
        gridWidgets.clear();
        gridCells.clear();
        String q = search.toLowerCase().trim();
        List<Opt> tmp = new ArrayList<>();
        if (source == 1) {
            for (Item i : Registries.ITEM) {
                if (i == Items.AIR) continue;
                if (q.isEmpty() || Registries.ITEM.getId(i).getPath().contains(q)
                        || i.getName().getString().toLowerCase().contains(q))
                    tmp.add(new Opt(i, ""));
            }
        } else {
            List<Config.Seen> src = source == 0 ? Presets.ALL : Config.get().catalog;
            for (Config.Seen sn : src) {
                Identifier idn = Identifier.tryParse(sn.id);
                if (idn == null) continue;
                Item it = Registries.ITEM.get(idn);
                if (it == Items.AIR) continue;
                if (q.isEmpty() || sn.name.toLowerCase().contains(q) || sn.id.contains(q))
                    tmp.add(new Opt(it, sn.name));
            }
        }
        filtered = tmp;
        int cols = 16, rows = 5, per = cols * rows;
        gridPages = Math.max(1, (filtered.size() + per - 1) / per);
        gridPage = Math.min(gridPage, gridPages - 1);
        for (int k = 0; k < per; k++) {
            int idx = gridPage * per + k;
            if (idx >= filtered.size()) break;
            final Opt o = filtered.get(idx);
            int x = px + 14 + (k % cols) * 25, y = py + 86 + (k / cols) * 23;
            ButtonWidget b = FlatButton.btn(x, y, 23, 22, "", false, bb -> {
                selItem = o.item(); eMin = "0"; eMax = "1m";
                eName = o.name().equals(o.item().getName().getString()) ? "" : o.name();
                clearAndInit();
            });
            addDrawableChild(b);
            gridWidgets.add(b);
            ItemStack gs = new ItemStack(o.item());
            if (!o.name().isEmpty()) gs.set(DataComponentTypes.CUSTOM_NAME, Text.literal(o.name()));
            gridCells.add(new Cell(x + 3, y + 3, gs));
        }
        pageText = (gridPage + 1) + "/" + gridPages + "   (" + filtered.size() + ")";
    }

    // ================= EDYTOR =================
    void buildEditor() {
        String id = editing != null ? editing.id : Registries.ITEM.getId(selItem).toString();
        ItemStack st = stackOf(id, eName);
        cards.add(new int[]{px + 12, py + 62, W - 24, 36});
        cells.add(new Cell(px + 22, py + 72, st));
        labels.add(new Label(px + 48, py + 68, eName.isEmpty() ? st.getName().getString() : eName, WHITE));
        labels.add(new Label(px + 48, py + 80, id, MUTED));
        label(px + 20, py + 110, "Min value (np. 100k)");
        field(px + 20, py + 122, 180, eMin, s -> eMin = s);
        label(px + 230, py + 110, "Max value (np. 1m)");
        field(px + 230, py + 122, 180, eMax, s -> eMax = s);
        label(px + 20, py + 150, "Nazwa zawiera (dla itemów serwerowych, opcjonalnie)");
        field(px + 20, py + 162, W - 40, eName, s -> eName = s);
        addDrawableChild(FlatButton.btn(px + 20, py + 192, 120, 20, editing != null ? "Zapisz" : "Dodaj", true, b -> {
            long mn = PriceParser.parseNumber(eMin), mx = PriceParser.parseNumber(eMax);
            if (mn < 0 || mx < 0) { status = "Błędna wartość min/max"; return; }
            if (mn > mx) { status = "Min nie może być większe niż Max"; return; }
            if (editing != null) {
                editing.min = mn; editing.max = mx; editing.nameContains = eName.trim();
            } else {
                Config.Entry e = new Config.Entry();
                e.id = id; e.min = mn; e.max = mx; e.nameContains = eName.trim();
                Config.get().entries.add(e);
            }
            Config.save();
            editing = null; selItem = null; tab = 0; status = "Zapisano";
            clearAndInit();
        }));
        addDrawableChild(FlatButton.btn(px + 150, py + 192, 100, 20, "Anuluj", false,
                b -> { editing = null; selItem = null; status = ""; clearAndInit(); }));
    }

    // ================= USTAWIENIA AUTORYNKU =================
    void buildSettings() {
        Config c = Config.get();
        cards.add(new int[]{px + 12, py + 62, W - 24, 150});
        label(px + 20, py + 70, "Przewijanie strony (ms)");
        intField(px + 20, py + 82, 180, c.pageDelayMs, v -> c.pageDelayMs = v);
        label(px + 20, py + 108, "Kupowanie itemu (ms)");
        intField(px + 20, py + 120, 180, c.buyDelayMs, v -> c.buyDelayMs = v);
        addDrawableChild(FlatButton.btn(px + 20, py + 148, 180, 18, "Kierunek: " + dirName(c.direction), false, b -> {
            c.direction = switch (c.direction) { case "RIGHT" -> "LEFT"; case "LEFT" -> "BOUNCE"; default -> "RIGHT"; };
            FlatButton.mark((ButtonWidget) b, "Kierunek: " + dirName(c.direction), false);
        }));
        label(px + 230, py + 70, "Tytuł rynku zawiera");
        field(px + 230, py + 82, 180, c.marketTitle, s -> c.marketTitle = s);
        label(px + 230, py + 108, "Słowo przed ceną (puste = auto)");
        field(px + 230, py + 120, 180, c.priceKeyword, s -> c.priceKeyword = s);
        label(px + 20, py + 178, "Strzałki i numer strony są wykrywane automatycznie.");
        label(px + 20, py + 190, "Gra liczy co 50 ms, więc mniej niż 50 ms nic nie da.");
    }

    static String dirName(String d) {
        return switch (d) { case "RIGHT" -> "Prawo"; case "LEFT" -> "Lewo"; default -> "Odbijanie"; };
    }

    // ================= CONFIG =================
    void buildConfig() {
        cards.add(new int[]{px + 12, py + 62, W - 24, 150});
        label(px + 20, py + 70, "Plik: config/" + Config.path().getFileName());
        addDrawableChild(FlatButton.btn(px + 20, py + 88, 180, 20, "Zapisz config", false,
                b -> { Config.save(); status = "Zapisano config"; }));
        addDrawableChild(FlatButton.btn(px + 20, py + 114, 180, 20, "Wczytaj config", false, b -> {
            status = Config.load() ? "Wczytano config" : "Błąd wczytywania";
            clearAndInit();
        }));
        addDrawableChild(FlatButton.btn(px + 20, py + 140, 180, 20, "Wyczyść katalog z rynku", false,
                b -> { Config.get().catalog.clear(); Config.save(); status = "Katalog wyczyszczony"; }));
        addDrawableChild(FlatButton.btn(px + 20, py + 166, 180, 20, "Reset do domyślnych", false,
                b -> { Config.reset(); status = "Zresetowano"; clearAndInit(); }));
        label(px + 230, py + 70, "Jak używać:");
        label(px + 230, py + 86, "1. Kliknij 'AutoRynek: ON'");
        label(px + 230, py + 98, "2. Dodaj itemy w 'Dodaj'");
        label(px + 230, py + 110, "3. Otwórz /ah");
        label(px + 230, py + 122, "4. Start = kupuje");
        label(px + 230, py + 134, "   Skanuj = liczy oferty");
    }

    // ================= RENDER =================
    @Override
    public void render(DrawContext c, int mx, int my, float d) {
        c.fill(0, 0, width, height, 0xC0000000);
        c.fill(px - 2, py - 2, px + W + 2, py + H + 2, 0xFF000000);
        c.fill(px, py, px + W, py + H, BG);
        c.fill(px, py, px + W, py + 27, HEAD);
        c.fill(px, py + 27, px + W, py + 29, ACCENT);
        c.fill(px + 8, py + 58, px + W - 8, py + H - 8, PANEL);
        FlatButton.border(c, px - 2, py - 2, W + 4, H + 4, 0xFF3A2A66);
        for (int[] r : cards) {
            c.fill(r[0], r[1], r[0] + r[2], r[1] + r[3], CARD);
            c.fill(r[0], r[1], r[0] + 2, r[1] + r[3], ACCENT);
        }
        c.drawTextWithShadow(textRenderer, "FullBright", px + 12, py + 10, ACCENT);
        c.drawTextWithShadow(textRenderer, "AutoRynek", px + 70, py + 10, MUTED);

        super.render(c, mx, my, d);

        for (Element e : children())
            if (e instanceof ButtonWidget b) FlatButton.paint(c, b, textRenderer, ACCENT);

        for (Label l : labels) c.drawTextWithShadow(textRenderer, l.t(), l.x(), l.y(), l.c());
        for (Cell k : cells) c.drawItem(k.s(), k.x(), k.y());
        for (Cell k : gridCells) c.drawItem(k.s(), k.x(), k.y());
        if (!pageText.isEmpty() && editing == null && selItem == null)
            c.drawCenteredTextWithShadow(textRenderer, pageText, px + W / 2, py + H - 32, WHITE);
        if (!status.isEmpty())
            c.drawTextWithShadow(textRenderer, status, px + 14, py + H - 4 - 8, 0xFFFFD166);
        String as = AutoBuyer.state() + (AutoBuyer.status.isEmpty() ? "" : " - " + AutoBuyer.status);
        c.drawTextWithShadow(textRenderer, as, px + W - 14 - textRenderer.getWidth(as), py + H - 12, 0xFF7DE29B);

        for (Cell k : gridCells)
            if (mx >= k.x() - 3 && mx < k.x() + 20 && my >= k.y() - 3 && my < k.y() + 19)
                c.drawItemTooltip(textRenderer, k.s(), mx, my);
        for (Cell k : cells)
            if (mx >= k.x() - 2 && mx < k.x() + 18 && my >= k.y() - 2 && my < k.y() + 18)
                c.drawItemTooltip(textRenderer, k.s(), mx, my);
    }
}
