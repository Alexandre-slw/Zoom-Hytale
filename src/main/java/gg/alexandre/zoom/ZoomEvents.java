package gg.alexandre.zoom;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.MovementStates;
import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.protocol.packets.player.ClientMovement;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import gg.alexandre.zoom.util.FovPacketUtil;

import javax.annotation.Nonnull;

public class ZoomEvents {

    public static void onPlayerConnect(@Nonnull PlayerConnectEvent event) {
        World world = event.getWorld();
        if (world == null) {
            return;
        }

        PlayerRef playerRef = event.getPlayerRef();

        FovPacketUtil fovUtil = new FovPacketUtil();
        fovUtil.setup(playerRef.getPacketHandler());

        event.getHolder().putComponent(ZoomPlugin.ZOOMING_TAG_TYPE, new ZoomingTag(fovUtil));
    }

    public static void onPacket(@Nonnull PlayerRef playerRef, @Nonnull Packet packet) {
        if (!(packet instanceof ClientMovement movement) || movement.movementStates == null) {
            return;
        }

        onMovementStates(playerRef, movement.movementStates);
    }

    private static void onMovementStates(@Nonnull PlayerRef playerRef, @Nonnull MovementStates movementStates) {
        // Ignore when in fluid because it never resets walking
        if (movementStates.walking && (movementStates.inFluid || movementStates.swimming)) {
            return;
        }

        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) {
            return;
        }

        Store<EntityStore> store = ref.getStore();
        World world = store.getExternalData().getWorld();
        world.execute(() -> updateZooming(ref, movementStates.walking));
    }

    private static void updateZooming(@Nonnull Ref<EntityStore> ref, boolean walking) {
        if (!ref.isValid()) {
            return;
        }

        Store<EntityStore> store = ref.getStore();
        ZoomingTag tag = store.getComponent(ref, ZoomPlugin.ZOOMING_TAG_TYPE);
        if (tag == null) {
            return;
        }

        tag.zooming = walking;

        if (!walking && tag.fovUtil != null) {
            PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
            assert playerRef != null;
            tag.fovUtil.clear(playerRef.getPacketHandler(), store.getExternalData().getWorld());
        }
    }
}
