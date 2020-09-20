package org.spongepowered.common.util.raytrace;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.util.blockray.RayTrace;
import org.spongepowered.api.world.LocatableBlock;

public final class SpongeRayTraceFactory implements RayTrace.Factory {

    @Override
    @NonNull
    public RayTrace<@NonNull Entity> entityRayTrace() {
        return null;
    }

    @Override
    @NonNull
    public RayTrace<@NonNull LocatableBlock> blockRayTrace() {
        return new SpongeBlockRayTrace();
    }

}
