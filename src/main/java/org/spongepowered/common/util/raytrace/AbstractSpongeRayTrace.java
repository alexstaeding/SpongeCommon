package org.spongepowered.common.util.raytrace;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.util.blockray.RayTrace;
import org.spongepowered.api.world.Locatable;
import org.spongepowered.api.world.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.math.vector.Vector3d;
import org.spongepowered.math.vector.Vector3i;

import java.util.function.Predicate;

public abstract class AbstractSpongeRayTrace<T extends Locatable> implements RayTrace<@NonNull T> {

    private final Predicate<T> defaultFilter;

    @Nullable Vector3d start;
    @Nullable Vector3d direction;
    @Nullable Vector3d end;
    @Nullable ResourceKey world;
    @Nullable Predicate<T> select;
    @Nullable Predicate<T> continueWhile = null;
    @Nullable Predicate<ServerLocation> continueWhileLocation = null;

    AbstractSpongeRayTrace(final Predicate<T> defaultFilter) {
        this.defaultFilter = defaultFilter;
        this.select = defaultFilter;
    }

    @Override
    @NonNull
    public final RayTrace<@NonNull T> world(@NonNull final ServerWorld serverWorld) {
        this.world = serverWorld.getKey();
        return this;
    }

    @Override
    @NonNull
    public final RayTrace<@NonNull T> sourcePosition(@NonNull final Entity sourceEntity) {
        this.start = sourceEntity.getPosition();
        return this;
    }

    @Override
    @NonNull
    public final RayTrace<@NonNull T> sourcePosition(@NonNull final Vector3d sourcePosition) {
        this.start = sourcePosition;
        return this;
    }

    @Override
    @NonNull
    public RayTrace<@NonNull T> continueWhile(@NonNull final Predicate<@NonNull T> continueWhile) {
        if (this.continueWhile == null) {
            this.continueWhile = continueWhile;
        } else {
            this.continueWhile = this.continueWhile.and(continueWhile);
        }
        return this;
    }

    @Override
    @NonNull
    public RayTrace<@NonNull T> continueWhileLocation(@NonNull final Predicate<ServerLocation> continueWhileLocation) {
        if (this.continueWhileLocation == null) {
            this.continueWhileLocation = continueWhileLocation;
        } else {
            this.continueWhileLocation = this.continueWhileLocation.and(continueWhileLocation);
        }
        return this;
    }

    @Override
    public RayTrace<@NonNull T> direction(@NonNull final Vector3d direction) {
        this.end = null;
        this.direction = direction.normalize();
        return this;
    }

    @Override
    @NonNull
    public final RayTrace<@NonNull T> continueUntil(@NonNull final Vector3d endPosition) {
        this.end = endPosition;
        this.direction = null;
        return this;
    }

    @Override
    @NonNull
    public final RayTrace<@NonNull T> select(@NonNull final Predicate<T> filter) {
        if (this.select == null) {
            this.select = filter;
        } else {
            this.select = this.select.or(filter);
        }
        return this;
    }

    final Vector3i getNextBlock(final Vector3i current, final TData data, final Vector3i steps) {
        return current.add(
                data.nextStepWillAdvanceX() ? steps.getX() : 0,
                data.nextStepWillAdvanceY() ? steps.getY() : 0,
                data.nextStepWillAdvanceZ() ? steps.getX() : 0
        );
    }

    final Vector3i createSteps(final Vector3d direction) {
        return new Vector3i(
                Math.signum(direction.getX()),
                Math.signum(direction.getY()),
                Math.signum(direction.getZ())
        );
    }

    @Override
    @NonNull
    public RayTrace<@NonNull T> reset() {
        this.select = this.defaultFilter;
        this.world = null;
        this.start = null;
        this.end = null;
        this.continueWhile = null;
        this.continueWhileLocation = null;
        return this;
    }

    final void setupEnd() {
        if (this.direction != null) {
            this.continueUntil(this.start.add(this.direction.mul(300)));
        }
    }

    final Vector3i initialBlock(final Vector3d direction) {
        return new Vector3i(
                this.start.getX() - (direction.getX() < 0 && this.start.getX() == 0 ? 1 : 0),
                this.start.getY() - (direction.getY() < 0 && this.start.getY() == 0 ? 1 : 0),
                this.start.getZ() - (direction.getZ() < 0 && this.start.getZ() == 0 ? 1 : 0)
        );
    }

    final TData createInitialTData(final Vector3d direction) {
        return new TData(
            0,
            this.getT(this.start.getX(), direction.getX(), this.end.getX()),
            this.getT(this.start.getY(), direction.getY(), this.end.getY()),
            this.getT(this.start.getZ(), direction.getZ(), this.end.getZ())
        );
    }

    final TData advance(final TData data, final Vector3d direction) {
        final double nextStep = data.getNextStep();
        return new TData(
                data.getTotalTWithNextStep(),
                data.nextStepWillAdvanceX() ? 1.0 / direction.getX() : data.gettToX() - nextStep,
                data.nextStepWillAdvanceY() ? 1.0 / direction.getY() : data.gettToY() - nextStep,
                data.nextStepWillAdvanceZ() ? 1.0 / direction.getZ() : data.gettToZ() - nextStep
        );
    }

    private double getT(final double start, final double direction, final double end) {
        if (direction > 0) {
            return (Math.min(end, Math.ceil(start)) - start) / direction;
        } else if (direction < 0) {
            return (start - Math.max(end, Math.floor(start))) / direction;
        } else {
            // Infinity - indicates we never reach a boundary.
            return Double.POSITIVE_INFINITY;
        }
    }

    static final class TData {

        private final double totalT;
        private final double tToX;
        private final double tToY;
        private final double tToZ;
        private final double nextStep;

        TData(final double totalT, final double tToX, final double tToY, final double tToZ) {
            this.totalT = totalT;
            this.tToX = tToX;
            this.tToY = tToY;
            this.tToZ = tToZ;
            this.nextStep = Math.min(tToX, Math.min(tToY, tToZ));
        }

        public double getTotalT() {
            return this.totalT;
        }

        public double gettToX() {
            return this.tToX;
        }

        public double gettToY() {
            return this.tToY;
        }

        public double gettToZ() {
            return this.tToZ;
        }

        public boolean nextStepWillAdvanceX() {
            return this.tToX >= this.nextStep;
        }

        public boolean nextStepWillAdvanceY() {
            return this.tToY >= this.nextStep;
        }

        public boolean nextStepWillAdvanceZ() {
            return this.tToZ >= this.nextStep;
        }

        public double getNextStep() {
            return this.nextStep;
        }

        public double getTotalTWithNextStep() {
            return this.nextStep + this.totalT;
        }
    }

}
