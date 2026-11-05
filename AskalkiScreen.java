package com.askalki;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class AskalkiScreen extends Screen {

    public AskalkiScreen() {
        super(Text.literal("Askalki"));
    }

    @Override
    protected void init() {
        int cx = width / 2 - 100;
        int y = height / 2 - 80;

        ButtonWidget master = ButtonWidget.builder(label("Wszystko", Settings.masterEnabled), null)
            .dimensions(cx, y, 200, 20).build();
        master.setPressAction(b -> {
            Settings.masterEnabled = !Settings.masterEnabled;
            b.setMessage(label("Wszystko", Settings.masterEnabled));
        });
        addDrawableChild(master);

        ButtonWidget fast = ButtonWidget.builder(label("Szybkie podnoszenie", Settings.fastPickup), null)
            .dimensions(cx, y + 30, 200, 20).build();
        fast.setPressAction(b -> {
            Settings.fastPickup = !Settings.fastPickup;
            b.setMessage(label("Szybkie podnoszenie", Settings.fastPickup));
        });
        addDrawableChild(fast);

        addDrawableChild(new DelaySlider(cx, y + 60, 200, 20));

        ButtonWidget land = ButtonWidget.builder(label("Złap od razu po wylądowaniu", Settings.instantOnLand), null)
            .dimensions(cx, y + 90, 200, 20).build();
        land.setPressAction(b -> {
            Settings.instantOnLand = !Settings.instantOnLand;
            b.setMessage(label("Złap od razu po wylądowaniu", Settings.instantOnLand));
        });
        addDrawableChild(land);

        ButtonWidget sprint = ButtonWidget.builder(label("Biegnij do itemu (sprint)", Settings.sprintToItem), null)
            .dimensions(cx, y + 120, 200, 20).build();
        sprint.setPressAction(b -> {
            Settings.sprintToItem = !Settings.sprintToItem;
            b.setMessage(label("Biegnij do itemu (sprint)", Settings.sprintToItem));
        });
        addDrawableChild(sprint);
    }

    private static Text label(String name, boolean on) {
        return Text.literal(name + ": " + (on ? "ON" : "OFF"));
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, "Askalki", width / 2, height / 2 - 100, 0xFFFFFF);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
