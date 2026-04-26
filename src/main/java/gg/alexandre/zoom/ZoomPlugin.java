package gg.alexandre.zoom;

import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.io.adapter.PacketFilter;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public class ZoomPlugin extends JavaPlugin {

    public static ComponentType<EntityStore, ZoomingTag> ZOOMING_TAG_TYPE;

    private PacketFilter movementFilter;

    public ZoomPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        ComponentRegistryProxy<EntityStore> entityStoreRegistry = getEntityStoreRegistry();

        ZOOMING_TAG_TYPE = entityStoreRegistry.registerComponent(ZoomingTag.class, () -> {
            throw new UnsupportedOperationException();
        });

        entityStoreRegistry.registerSystem(new ZoomSystem());
        getEventRegistry().register(PlayerConnectEvent.class, ZoomEvents::onPlayerConnect);
        movementFilter = PacketAdapters.registerInbound(ZoomEvents::onPacket);
    }

    @Override
    protected void shutdown() {
        if (movementFilter != null) {
            PacketAdapters.deregisterInbound(movementFilter);
            movementFilter = null;
        }
    }

}
