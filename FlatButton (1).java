package pl.fullbright;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/** Pomocnik: zwykłe przyciski (builder) + własne malowanie "płaskiego" stylu. */
public class FlatButton {
    private static final Set<ButtonWidget> SEL = Collections.newSetFromMap(new WeakHashMap<>());

    public static ButtonWidget btn(int x, int y, int w, int h, String text, boolean selected,
                                   ButtonWidget.PressAction action) {
        ButtonWidget b = ButtonWidget.builder(Text.literal(text), action).dimensions(x, y, w, h).build();
        if (selected) SEL.add(b);
        return b;
    }

    public static void mark(ButtonWidget b, String text, boolean selected) {
        b.setMessage(Text.literal(text));
        if (selected) SEL.add(b); else SEL.remove(b);
    }

    public static void border(DrawContext c, int x, int y, int w, int h, int color) {
        c.fill(x, y, x + w, y + 1, color);
        c.fill(x, y + h - 1, x + w, y + h, color);
        c.fill(x, y, x + 1, y + h, color);
        c.fill(x + w - 1, y, x + w, y + h, color);
    }

    /** Maluje ładny przycisk dokładnie na miejscu zwykłego. */
    public static void paint(DrawContext c, ButtonWidget b, TextRenderer tr, int accent) {
        int x = b.getX(), y = b.getY(), w = b.getWidth(), h = b.getHeight();
        String t = b.getMessage().getString();
        boolean hov = b.isHovered(), sel = SEL.contains(b);
        int bg, fg = 0xFFE6E9F0, bd = hov ? 0xFF7A84A8 : 0xFF353B50;
        if (t.equals("ON") || t.endsWith(": ON")) { bg = hov ? 0xFF2FA866 : 0xFF23864F; fg = 0xFFFFFFFF; bd = 0xFF3DDC84; }
        else if (t.equals("OFF") || t.endsWith(": OFF")) { bg = hov ? 0xFF3B4153 : 0xFF2B3040; fg = 0xFF9AA4B2; }
        else if (t.equals("Usuń")) { bg = hov ? 0xFFB43A3A : 0xFF8E2B2B; fg = 0xFFFFFFFF; bd = 0xFFE05A5A; }
        else if (sel) { bg = accent; fg = 0xFF14161C; bd = 0xFFFFE08A; }
        else bg = hov ? 0xFF343B52 : 0xFF232838;
        c.fill(x, y, x + w, y + h, bg);
        border(c, x, y, w, h, bd);
        if (!t.isEmpty()) c.drawCenteredTextWithShadow(tr, t, x + w / 2, y + (h - 8) / 2, fg);
    }
}
