package gg.alexandre.zoom;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public class ZoomSystem extends EntityTickingSystem<EntityStore> {

    private Query<EntityStore> query;

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        if (query == null) {
            query = Query.and(ZoomPlugin.ZOOMING_TAG_TYPE);
        }

        return query;
    }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> chunk,
                     @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        ZoomingTag tag = chunk.getComponent(index, ZoomPlugin.ZOOMING_TAG_TYPE);
        assert tag != null;

        Ref<EntityStore> ref = chunk.getReferenceTo(index);
        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());

        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        assert playerRef != null;

        if (!tag.zooming || transform == null) {
            return;
        }

        tag.fovUtil.apply(playerRef.getPacketHandler(), store.getExternalData().getWorld(), transform.getPosition());
    }

}
