package org.spongepowered.common.util.raytrace;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.util.blockray.RayTrace;
import org.spongepowered.api.world.LocatableBlock;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.math.vector.Vector3d;
import org.spongepowered.math.vector.Vector3i;

import java.util.Optional;
import java.util.function.Predicate;

public class SpongeBlockRayTrace implements RayTrace<@NonNull LocatableBlock> {

    private static final Predicate<LocatableBlock> DEFAULT_FILTER = block -> {
        final BlockType type = block.getBlockState().getType();
        return type != BlockTypes.CAVE_AIR.get() && type != BlockTypes.VOID_AIR.get() && type != BlockTypes.AIR.get();
    };

    private Vector3d start;
    private Vector3d end;
    private ResourceKey world;
    private Predicate<LocatableBlock> filter = SpongeBlockRayTrace.DEFAULT_FILTER;

    @Override
    @NonNull
    public RayTrace<@NonNull LocatableBlock> world(@NonNull final ServerWorld serverWorld) {
        this.world = serverWorld.getKey();
        return this;
    }

    @Override
    @NonNull
    public RayTrace<@NonNull LocatableBlock> sourcePosition(@NonNull final Entity sourceEntity) {
        this.start = sourceEntity.getPosition();
        return this;
    }

    @Override
    @NonNull
    public RayTrace<@NonNull LocatableBlock> sourcePosition(@NonNull final Vector3d sourcePosition) {
        this.start = sourcePosition;
        return this;
    }

    @Override
    @NonNull
    public RayTrace<@NonNull LocatableBlock> endPosition(@NonNull final Vector3d endPosition) {
        this.end = endPosition;
        return this;
    }

    @Override
    @NonNull
    public RayTrace<@NonNull LocatableBlock> accept(@NonNull final Predicate<LocatableBlock> filter) {
        this.filter = filter;
        return this;
    }

    @Override
    @NonNull
    public Optional<LocatableBlock> execute() {
        // get the direction
        final Vector3d directionWithLength = this.end.sub(this.start);
        final double length = directionWithLength.length();
        final Vector3d direction = directionWithLength.normalize();
        if (direction.lengthSquared() == 0) {
            throw new IllegalStateException("The start and end must be two different vectors");
        }

        final ServerWorld serverWorld = Sponge.getServer().getWorldManager().getWorld(this.world)
                .orElseThrow(() -> new IllegalStateException("World with key " + this.world.getFormatted() + " is not loaded!"));

        Vector3i currentBlock = new Vector3i(
                this.start.getX() - (direction.getX() < 0 && this.start.getX() == 0 ? 1 : 0),
                this.start.getY() - (direction.getY() < 0 && this.start.getY() == 0 ? 1 : 0),
                this.start.getZ() - (direction.getZ() < 0 && this.start.getZ() == 0 ? 1 : 0)
            );

        {
            final LocatableBlock block = serverWorld.getLocation(currentBlock).asLocatableBlock();
            if (this.filter.test(block)) {
                return Optional.of(block);
            }
        }

        final Vector3i steps = new Vector3i(
                Math.signum(direction.getX()),
                Math.signum(direction.getY()),
                Math.signum(direction.getZ())
        );

        // The ray equation is, vec(u) + t vec(d). From a point (x, y), there is a t
        // that we need to traverse to get to a boundary. We work that out now...
        double tToX = this.getT(this.start.getX(), direction.getX(), this.end.getX());
        double tToY = this.getT(this.start.getY(), direction.getY(), this.end.getY());
        double tToZ = this.getT(this.start.getZ(), direction.getZ(), this.end.getZ());
        double totalTraversal = 0;

        double nextStep;
        while ((nextStep = Math.min(tToX, Math.min(tToY, tToZ))) + totalTraversal < length) {
            totalTraversal += nextStep;
            tToX -= nextStep;
            tToY -= nextStep;
            tToZ -= nextStep;
            if (tToX == 0) {
                tToX = 1.0 / direction.getX();
                currentBlock = currentBlock.add(steps.getX(), 0, 0);
            }

            if (tToY == 0) {
                tToY = 1.0 / direction.getY();
                currentBlock = currentBlock.add(0, steps.getY(), 0);
            }

            if (tToZ == 0) {
                tToZ = 1.0 / direction.getZ();
                currentBlock = currentBlock.add(0, 0, steps.getZ());
            }

            if (this.filter.test(serverWorld.getLocatableBlock(currentBlock))) {
                return Optional.of(serverWorld.getLocatableBlock(currentBlock));
            }
        }

        return Optional.empty();
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

    @Override
    @NonNull
    public RayTrace<@NonNull LocatableBlock> reset() {
        this.filter = SpongeBlockRayTrace.DEFAULT_FILTER;
        this.world = null;
        this.start = null;
        this.end = null;
        return this;
    }
}
