package pl.fullbright;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class FullBrightMod implements ClientModInitializer {
    public static KeyBinding KEY;

    @Override
    public void onInitializeClient() {
        Config.load();
        MarketOverlay.register();
        KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.fullbright.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_J,
                KeyBinding.Category.create(Identifier.of("fullbright", "main"))));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (KEY.wasPressed()) {
                mc.setScreen(new MenuScreen());
            }
            AutoBuyer.tick(mc);
        });
    }
}
