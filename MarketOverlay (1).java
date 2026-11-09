package pl.fullbright;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;

import java.util.Locale;

/** Start / Stop / Skanuj nad oknem rynku - tylko gdy "AutoRynek: ON" w menu. */
public class MarketOverlay {
    private static int seg(DrawContext c, TextRenderer tr, String s, int x, int y, int col) {
        c.drawTextWithShadow(tr, s, x, y, col);
        return x + tr.getWidth(s);
    }

    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (!Config.get().module) return;
            if (!(screen instanceof GenericContainerScreen gcs)) return;
            String title = gcs.getTitle().getString().toLowerCase(Locale.ROOT);
            if (!title.contains(Config.get().marketTitle.toLowerCase(Locale.ROOT))) return;

            int rows = gcs.getScreenHandler().getRows();
            int bh = 114 + rows * 18;
            int gx = (w - 176) / 2, gy = (h - bh) / 2;
            int bx = gx, by = Math.max(2, gy - 34);

            ButtonWidget start = FlatButton.btn(bx, by, 50, 18, "Start", false, b -> AutoBuyer.start());
            ButtonWidget stop = FlatButton.btn(bx + 54, by, 50, 18, "Stop", false, b -> AutoBuyer.stop());
            ButtonWidget scan = FlatButton.btn(bx + 108, by, 58, 18, "Skanuj", false, b -> AutoBuyer.startScan());
            var btns = Screens.getButtons(screen);
            btns.add(start);
            btns.add(stop);
            btns.add(scan);

            ScreenEvents.beforeRender(screen).register((sc, ctx, mx, my, delta) -> {
                boolean on = Config.get().enabled;
                start.active = !(on && !AutoBuyer.scanner);
                scan.active = !(on && AutoBuyer.scanner);
                stop.active = on;
            });

            ScreenEvents.afterRender(screen).register((sc, ctx, mx, my, delta) -> {
                TextRenderer tr = MinecraftClient.getInstance().textRenderer;
                int x = bx + 174, y = by + 5;
                boolean on = Config.get().enabled;
                int col = !on ? 0xFFFFFFFF : AutoBuyer.scanner ? 0xFF55FFFF : 0xFF55FF55;
                x = seg(ctx, tr, AutoBuyer.state(), x, y, col);
                x = seg(ctx, tr, " | ", x, y, 0xFF8B93A5);
                x = seg(ctx, tr, "Baza: ", x, y, 0xFFAAAAAA);
                x = seg(ctx, tr, String.valueOf(AutoBuyer.scanTotal), x, y, 0xFFFFFFFF);
                x = seg(ctx, tr, " | ", x, y, 0xFF8B93A5);
                x = seg(ctx, tr, "Snajper: ", x, y, 0xFFFFAA00);
                seg(ctx, tr, String.valueOf(AutoBuyer.scanner ? AutoBuyer.scanInRange : AutoBuyer.bought), x, y, 0xFFFFFFFF);
                if (!AutoBuyer.status.isEmpty())
                    ctx.drawTextWithShadow(tr, AutoBuyer.status, bx + 174, by + 17, 0xFF8B93A5);
            });
        });
    }
}
