package org.spongepowered.common.util.raytrace;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.api.util.blockray.RayTraceResult;
import org.spongepowered.api.world.Locatable;
import org.spongepowered.math.vector.Vector3d;

public class SpongeRayTraceResult<T extends Locatable> implements RayTraceResult<@NonNull T> {

    private final T selectedObject;
    private final Vector3d hitPosition;

    public SpongeRayTraceResult(final T selectedObject, final Vector3d hitPosition) {
        this.selectedObject = selectedObject;
        this.hitPosition = hitPosition;
    }

    @Override
    public T getSelectedObject() {
        return this.selectedObject;
    }

    @Override
    public Vector3d getHitPosition() {
        return this.hitPosition;
    }

}
