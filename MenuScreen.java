package pl.fullbright;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
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
    static final int W = 380, H = 232;
    record Label(int x, int y, String t, int c) {}
    record Cell(int x, int y, ItemStack s) {}

    int tab = 0, px, py, gridPage = 0, listPage = 0, gridPages = 1, listPages = 1;
    String search = "", status = "", pageText = "";
    Config.Entry editing;
    Item selItem;
    String eMin = "0", eMax = "1m", eName = "";
    final List<Label> labels = new ArrayList<>();
    final List<Cell> cells = new ArrayList<>();
    final List<Cell> gridCells = new ArrayList<>();
    final List<ClickableWidget> gridWidgets = new ArrayList<>();
    List<Item> filtered = new ArrayList<>();
    ButtonWidget toggleBtn;

    public MenuScreen() { super(Text.literal("FullBright")); }

    @Override public boolean shouldPause() { return false; }
    @Override public void renderBackground(DrawContext c, int x, int y, float d) {}
    @Override public void removed() { Config.save(); }

    static ItemStack stackOf(String id) {
        Identifier i = Identifier.tryParse(id);
        return i == null ? ItemStack.EMPTY : new ItemStack(Registries.ITEM.get(i));
    }

    @Override
    protected void init() {
        px = (width - W) / 2;
        py = (height - H) / 2;
        labels.clear(); cells.clear(); gridCells.clear(); gridWidgets.clear(); pageText = "";
        String[] names = {"Lista", "Dodaj", "AutoRynek", "Config"};
        for (int i = 0; i < 4; i++) {
            final int t = i;
            addDrawableChild(FlatButton.btn(px + 10 + i * 90, py + 28, 86, 18, names[i], tab == i,
                    b -> { tab = t; editing = null; selItem = null; status = ""; clearAndInit(); }));
        }
        toggleBtn = FlatButton.btn(px + W - 122, py + 3, 116, 16, "", false, b -> {
            if (!Config.get().enabled) AutoBuyer.start(); else AutoBuyer.stop();
            refreshToggle();
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
        boolean on = Config.get().enabled;
        FlatButton.mark(toggleBtn, "AutoRynek: " + (on ? "ON" : "OFF"), on);
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

    void label(int x, int y, String t) { labels.add(new Label(x, y, t, 0xFF9AA4B2)); }

    // ---------------- LISTA ----------------
    void buildList() {
        List<Config.Entry> es = Config.get().entries;
        int per = 5;
        listPages = Math.max(1, (es.size() + per - 1) / per);
        listPage = Math.min(listPage, listPages - 1);
        if (es.isEmpty()) label(px + 14, py + 60, "Brak przedmiotów. Dodaj je w zakładce 'Dodaj'.");
        for (int r = 0; r < per; r++) {
            int idx = listPage * per + r;
            if (idx >= es.size()) break;
            final Config.Entry e = es.get(idx);
            int y = py + 54 + r * 25;
            ItemStack st = stackOf(e.id);
            cells.add(new Cell(px + 14, y + 2, st));
            labels.add(new Label(px + 38, y + 1, st.getName().getString()
                    + (e.nameContains.isEmpty() ? "" : " [" + e.nameContains + "]"), 0xFFFFFFFF));
            labels.add(new Label(px + 38, y + 12, "Min " + PriceParser.format(e.min)
                    + "  -  Max " + PriceParser.format(e.max), 0xFF9AA4B2));
            addDrawableChild(FlatButton.btn(px + W - 160, y + 3, 36, 16, e.enabled ? "ON" : "OFF", e.enabled,
                    b -> { e.enabled = !e.enabled; Config.save(); clearAndInit(); }));
            addDrawableChild(FlatButton.btn(px + W - 120, y + 3, 56, 16, "Edytuj", false, b -> {
                editing = e; eMin = PriceParser.format(e.min); eMax = PriceParser.format(e.max);
                eName = e.nameContains; clearAndInit();
            }));
            addDrawableChild(FlatButton.btn(px + W - 60, y + 3, 46, 16, "Usuń", false, b -> { es.remove(e); Config.save(); clearAndInit(); }));
        }
        addDrawableChild(FlatButton.btn(px + 14, py + H - 34, 40, 16, "<", false, b -> { if (listPage > 0) { listPage--; clearAndInit(); } }));
        addDrawableChild(FlatButton.btn(px + W - 54, py + H - 34, 40, 16, ">", false, b -> { if (listPage < listPages - 1) { listPage++; clearAndInit(); } }));
        pageText = (listPage + 1) + "/" + listPages;
    }

    // ---------------- DODAJ ----------------
    void buildAdd() {
        TextFieldWidget f = new TextFieldWidget(textRenderer, px + 14, py + 52, W - 28, 16, Text.empty());
        f.setMaxLength(40);
        f.setPlaceholder(Text.literal("Szukaj przedmiotu (np. diamond, pumpkin)..."));
        f.setText(search);
        f.setChangedListener(s -> { search = s; gridPage = 0; rebuildGrid(); });
        addDrawableChild(f);
        addDrawableChild(FlatButton.btn(px + 14, py + H - 34, 40, 16, "<", false, b -> { if (gridPage > 0) { gridPage--; rebuildGrid(); } }));
        addDrawableChild(FlatButton.btn(px + W - 54, py + H - 34, 40, 16, ">", false, b -> { if (gridPage < gridPages - 1) { gridPage++; rebuildGrid(); } }));
        rebuildGrid();
    }

    void rebuildGrid() {
        for (ClickableWidget w : gridWidgets) remove(w);
        gridWidgets.clear();
        gridCells.clear();
        String q = search.toLowerCase().trim();
        filtered = Registries.ITEM.stream()
                .filter(i -> i != Items.AIR)
                .filter(i -> q.isEmpty()
                        || Registries.ITEM.getId(i).getPath().contains(q)
                        || i.getName().getString().toLowerCase().contains(q))
                .toList();
        int cols = 14, rows = 5, per = cols * rows;
        gridPages = Math.max(1, (filtered.size() + per - 1) / per);
        gridPage = Math.min(gridPage, gridPages - 1);
        for (int k = 0; k < per; k++) {
            int idx = gridPage * per + k;
            if (idx >= filtered.size()) break;
            final Item it = filtered.get(idx);
            int x = px + 12 + (k % cols) * 25, y = py + 76 + (k / cols) * 23;
            ButtonWidget b = FlatButton.btn(x, y, 23, 22, "", false, bb -> {
                selItem = it; eMin = "0"; eMax = "1m"; eName = ""; clearAndInit();
            });
            addDrawableChild(b);
            gridWidgets.add(b);
            gridCells.add(new Cell(x + 3, y + 3, new ItemStack(it)));
        }
        pageText = (gridPage + 1) + "/" + gridPages + "  (" + filtered.size() + ")";
    }

    // ---------------- EDYTOR (Min value / Max value) ----------------
    void buildEditor() {
        String id = editing != null ? editing.id : Registries.ITEM.getId(selItem).toString();
        ItemStack st = stackOf(id);
        cells.add(new Cell(px + 14, py + 56, st));
        labels.add(new Label(px + 38, py + 53, st.getName().getString(), 0xFFFFFFFF));
        labels.add(new Label(px + 38, py + 64, id, 0xFF9AA4B2));
        label(px + 14, py + 88, "Min value (np. 100k)");
        field(px + 14, py + 100, 166, eMin, s -> eMin = s);
        label(px + 200, py + 88, "Max value (np. 1m)");
        field(px + 200, py + 100, 166, eMax, s -> eMax = s);
        label(px + 14, py + 128, "Nazwa zawiera (opcjonalnie, dla itemów serwerowych)");
        field(px + 14, py + 140, W - 28, eName, s -> eName = s);
        addDrawableChild(FlatButton.btn(px + 14, py + H - 52, 110, 18, editing != null ? "Zapisz" : "Dodaj", true, b -> {
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
        addDrawableChild(FlatButton.btn(px + 134, py + H - 52, 90, 18, "Anuluj", false, b -> { editing = null; selItem = null; status = ""; clearAndInit(); }));
    }

    // ---------------- USTAWIENIA AUTORYNKU ----------------
    void buildSettings() {
        Config c = Config.get();
        label(px + 14, py + 54, "Przewijanie strony (ms)");
        intField(px + 14, py + 66, 160, c.pageDelayMs, v -> c.pageDelayMs = v);
        label(px + 14, py + 90, "Kupowanie itemu (ms)");
        intField(px + 14, py + 102, 160, c.buyDelayMs, v -> c.buyDelayMs = v);
        addDrawableChild(FlatButton.btn(px + 14, py + 128, 160, 18, "Kierunek: " + dirName(c.direction), false, b -> {
            c.direction = switch (c.direction) { case "RIGHT" -> "LEFT"; case "LEFT" -> "BOUNCE"; default -> "RIGHT"; };
            b.setMessage(Text.literal("Kierunek: " + dirName(c.direction)));
        }));
        label(px + 200, py + 54, "Slot strzałka lewo");
        intField(px + 200, py + 66, 70, c.prevSlot, v -> c.prevSlot = v);
        label(px + 280, py + 54, "Slot strzałka prawo");
        intField(px + 280, py + 66, 70, c.nextSlot, v -> c.nextSlot = v);
        label(px + 200, py + 90, "Slot strony (1/81)");
        intField(px + 200, py + 102, 70, c.pageSlot, v -> c.pageSlot = v);
        label(px + 14, py + 156, "Tytuł rynku zawiera");
        field(px + 14, py + 168, 160, c.marketTitle, s -> c.marketTitle = s);
        label(px + 200, py + 156, "Słowo przed ceną w lore (puste=auto)");
        field(px + 200, py + 168, 166, c.priceKeyword, s -> c.priceKeyword = s);
    }

    static String dirName(String d) {
        return switch (d) { case "RIGHT" -> "Prawo"; case "LEFT" -> "Lewo"; default -> "Odbijanie"; };
    }

    // ---------------- CONFIG ----------------
    void buildConfig() {
        label(px + 14, py + 56, "Plik: " + Config.path().getFileName());
        addDrawableChild(FlatButton.btn(px + 14, py + 74, 160, 20, "Zapisz config", false, b -> { Config.save(); status = "Zapisano config"; }));
        addDrawableChild(FlatButton.btn(px + 14, py + 100, 160, 20, "Wczytaj config", false, b -> {
            status = Config.load() ? "Wczytano config" : "Błąd wczytywania";
            clearAndInit();
        }));
        addDrawableChild(FlatButton.btn(px + 14, py + 126, 160, 20, "Reset do domyślnych", false, b -> { Config.reset(); status = "Zresetowano"; clearAndInit(); }));
        label(px + 14, py + 156, "Ścieżka: " + Config.path());
    }

    @Override
    public void render(DrawContext c, int mx, int my, float d) {
        c.fill(0, 0, width, height, 0xB0000000);
        c.fill(px, py, px + W, py + H, 0xFF14161C);
        c.fill(px, py, px + W, py + 22, 0xFF1C2030);
        FlatButton.border(c, px, py, W, H, 0xFF3B82F6);
        c.drawTextWithShadow(textRenderer, "FullBright", px + 8, py + 7, 0xFF7DB3FF);
        super.render(c, mx, my, d);

        for (Label l : labels) c.drawTextWithShadow(textRenderer, l.t(), l.x(), l.y(), l.c());
        for (Cell k : cells) c.drawItem(k.s(), k.x(), k.y());
        for (Cell k : gridCells) c.drawItem(k.s(), k.x(), k.y());
        if (!pageText.isEmpty() && editing == null && selItem == null)
            c.drawCenteredTextWithShadow(textRenderer, pageText, px + W / 2, py + H - 30, 0xFFFFFFFF);
        if (!status.isEmpty())
            c.drawTextWithShadow(textRenderer, status, px + 14, py + H - 12, 0xFFFFD166);
        String as = "Status: " + AutoBuyer.status;
        c.drawTextWithShadow(textRenderer, as, px + W - 14 - textRenderer.getWidth(as), py + H - 12, 0xFF7DE29B);

        for (Cell k : gridCells) {
            if (mx >= k.x() - 3 && mx < k.x() + 20 && my >= k.y() - 3 && my < k.y() + 19)
                c.drawItemTooltip(textRenderer, k.s(), mx, my);
        }
        for (Cell k : cells) {
            if (mx >= k.x() - 2 && mx < k.x() + 18 && my >= k.y() - 2 && my < k.y() + 18)
                c.drawItemTooltip(textRenderer, k.s(), mx, my);
        }
    }
}
