package pl.fullbright;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;

import java.util.Locale;

/** Dodaje Start / Stop / Skaner na górze okna rynku (/ah). */
public class MarketOverlay {
    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (!(screen instanceof GenericContainerScreen gcs)) return;
            String title = gcs.getTitle().getString().toLowerCase(Locale.ROOT);
            if (!title.contains(Config.get().marketTitle.toLowerCase(Locale.ROOT))) return;

            int bw = 64, gap = 4, x = (w - (bw * 3 + gap * 2)) / 2, y = 4;
            ButtonWidget start = FlatButton.btn(x, y, bw, 18, "Start", false, b -> AutoBuyer.start());
            ButtonWidget stop = FlatButton.btn(x + bw + gap, y, bw, 18, "Stop", false, b -> AutoBuyer.stop());
            ButtonWidget scan = FlatButton.btn(x + (bw + gap) * 2, y, bw, 18, "Skaner", false, b -> AutoBuyer.startScan());
            var btns = Screens.getButtons(screen);
            btns.add(start);
            btns.add(stop);
            btns.add(scan);

            ScreenEvents.beforeRender(screen).register((sc, ctx, mx, my, delta) -> {
                boolean on = Config.get().enabled;
                FlatButton.mark(start, "Start", on && !AutoBuyer.scanner);
                FlatButton.mark(scan, "Skaner", on && AutoBuyer.scanner);
                FlatButton.mark(stop, "Stop", !on);
            });

            ScreenEvents.afterRender(screen).register((sc, ctx, mx, my, delta) -> {
                var tr = MinecraftClient.getInstance().textRenderer;
                String l1 = "Status: " + AutoBuyer.status;
                ctx.drawCenteredTextWithShadow(tr, l1, w / 2, y + 24, 0xFF7DE29B);
                if (AutoBuyer.scanner && (AutoBuyer.scanPages > 0 || AutoBuyer.scanDone)) {
                    String l2 = "Na rynku: " + AutoBuyer.scanTotal + " szt  |  W configu: " + AutoBuyer.scanMatched
                            + " szt  |  W cenie (snajp): " + AutoBuyer.scanInRange + " szt  |  Strony: "
                            + AutoBuyer.scanPages + "/" + AutoBuyer.scanPagesTotal;
                    ctx.drawCenteredTextWithShadow(tr, l2, w / 2, y + 36, 0xFFFFD166);
                }
            });
        });
    }
}
