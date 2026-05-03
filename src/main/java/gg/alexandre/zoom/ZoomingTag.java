package gg.alexandre.zoom;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import gg.alexandre.zoom.util.FovPacketUtil;

import javax.annotation.Nonnull;

public class ZoomingTag implements Component<EntityStore> {

    public FovPacketUtil fovUtil;
    public boolean zooming;
    public int activeSlot;
    public double zoomLevel = 0.2;

    public ZoomingTag(@Nonnull FovPacketUtil fovUtil) {
        this.fovUtil = fovUtil;
    }

    @Nonnull
    @Override
    public Component<EntityStore> clone() {
        ZoomingTag tag = new ZoomingTag(fovUtil);
        tag.zooming = zooming;
        tag.activeSlot = activeSlot;
        return tag;
    }

}
