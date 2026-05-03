package gg.alexandre.zoom;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.MovementStates;
import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChain;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChains;
import com.hypixel.hytale.protocol.packets.inventory.SetActiveSlot;
import com.hypixel.hytale.protocol.packets.player.ClientMovement;
import com.hypixel.hytale.server.core.entity.entities.Player;
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
        if (packet instanceof ClientMovement movement && movement.movementStates != null) {
            onMovementStates(playerRef, movement.movementStates);
        }

        if (packet instanceof SyncInteractionChains syncInteractionChains) {
            onSyncInteractionChains(playerRef, syncInteractionChains);
        }
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

    private static void onSyncInteractionChains(@Nonnull PlayerRef playerRef,
                                                @Nonnull SyncInteractionChains syncInteractionChains) {
        SyncInteractionChain interactionChain = null;

        int activeSlot = 0;
        int targetSlot = 0;

        for (int i = 0; i < syncInteractionChains.updates.length; i++) {
            SyncInteractionChain chain = syncInteractionChains.updates[i];
            if (chain.interactionType == InteractionType.SwapFrom && chain.data != null) {
                interactionChain = chain;

                activeSlot = chain.activeHotbarSlot;
                targetSlot = chain.data.targetSlot;
                break;
            }
        }

        if (interactionChain == null) {
            return;
        }

        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) {
            return;
        }

        Store<EntityStore> store = ref.getStore();
        World world = store.getExternalData().getWorld();

        SyncInteractionChain finalInteractionChain = interactionChain;
        int finalTargetSlot = targetSlot;
        int finalActiveSlot = activeSlot;

        world.execute(() -> {
            Player player = store.getComponent(ref, Player.getComponentType());
            assert player != null;

            ZoomingTag tag = store.getComponent(ref, ZoomPlugin.ZOOMING_TAG_TYPE);
            if (tag == null) {
                return;
            }

            if (!tag.zooming) {
                tag.activeSlot = finalTargetSlot;
                return;
            }

            if (finalInteractionChain.initial && finalActiveSlot != finalTargetSlot) {
                if (isLeft(finalActiveSlot, finalTargetSlot)) {
                    tag.zoomLevel += FovPacketUtil.STEP;
                } else {
                    tag.zoomLevel -= FovPacketUtil.STEP;
                }

                tag.zoomLevel = Math.max(FovPacketUtil.MIN_FOV, Math.min(tag.zoomLevel, FovPacketUtil.MAX_FOV));
            }

            playerRef.getPacketHandler().writeNoCache(new SetActiveSlot(-1, tag.activeSlot));
        });
    }

    private static boolean isLeft(int oldIndex, int newIndex) {
        int size = 9;
        int rightDistance = (newIndex - oldIndex + size) % size;
        int leftDistance = (oldIndex - newIndex + size) % size;

        return rightDistance < leftDistance;
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
            tag.zoomLevel = 0.2;
        }
    }
}
