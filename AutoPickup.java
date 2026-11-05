package com.askalki;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AutoPickup {
    private static final double RANGE = 8.0;
    private static final double PICK_DISTANCE = 0.6;

    // Czas pojawienia się itemu u klienta (klucz = UUID encji)
    private static final Map<UUID, Long> spawnTimes = new HashMap<>();
    private static boolean controlling = false;

    public static void init() {
        ClientEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof ItemEntity) {
                spawnTimes.put(entity.getUuid(), System.currentTimeMillis());
            }
        });
        ClientEntityEvents.ENTITY_UNLOAD.register((entity, world) -> spawnTimes.remove(entity.getUuid()));
        ClientTickEvents.END_CLIENT_TICK.register(AutoPickup::tick);
    }

    private static void tick(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            spawnTimes.clear();
            controlling = false;
            return;
        }

        if (!Settings.masterEnabled || !Settings.fastPickup) {
            release(client);
            return;
        }

        PlayerEntity p = client.player;
        ItemEntity target = null;
        double best = Double.MAX_VALUE;
        long now = System.currentTimeMillis();

        for (ItemEntity item : client.world.getEntitiesByClass(ItemEntity.class,
                p.getBoundingBox().expand(RANGE), e -> true)) {
            Long born = spawnTimes.get(item.getUuid());
            if (born == null) continue;

            long age = now - born;
            boolean ready = Settings.instantOnLand
                    ? (item.isOnGround() || age >= Settings.delayMs)
                    : age >= Settings.delayMs;
            if (!ready) continue;

            double d = p.distanceTo(item);
            if (d < best) {
                best = d;
                target = item;
            }
        }

        if (target == null || best < PICK_DISTANCE) {
            release(client);
            return;
        }

        // Patrz w stronę itemu
        double dx = target.getX() - p.getX();
        double dz = target.getZ() - p.getZ();
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        p.setYaw(yaw);
        p.setPitch(0);

        // Biegnij
        client.options.forwardKey.setPressed(true);
        client.options.sprintKey.setPressed(Settings.sprintToItem);
        controlling = true;
    }

    private static void release(MinecraftClient client) {
        if (controlling) {
            client.options.forwardKey.setPressed(false);
            client.options.sprintKey.setPressed(false);
            controlling = false;
        }
    }
}
