package org.spongepowered.common.util.raytrace;

import com.google.common.base.Preconditions;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.util.blockray.RayTrace;
import org.spongepowered.api.util.blockray.RayTraceResult;
import org.spongepowered.api.world.LocatableBlock;
import org.spongepowered.api.world.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.math.vector.Vector3d;
import org.spongepowered.math.vector.Vector3i;

import java.util.Optional;
import java.util.function.Predicate;

public final class SpongeBlockRayTrace extends AbstractSpongeRayTrace<@NonNull LocatableBlock> {

    private static final Predicate<LocatableBlock> DEFAULT_FILTER = block -> {
        final BlockType type = block.getBlockState().getType();
        return type != BlockTypes.CAVE_AIR.get() && type != BlockTypes.VOID_AIR.get() && type != BlockTypes.AIR.get();
    };

    SpongeBlockRayTrace() {
        super(SpongeBlockRayTrace.DEFAULT_FILTER);
    }

    @Override
    @NonNull
    public final Optional<RayTraceResult<@NonNull LocatableBlock>> execute() {
        Preconditions.checkState(this.start != null, "start cannot be null");
        Preconditions.checkState(this.end != null || this.direction != null, "end cannot be null");
        Preconditions.checkState(this.world != null, "world cannot be null");
        Preconditions.checkState(this.select != null, "select filter cannot be null");

        this.setupEnd();

        // get the direction
        final Vector3d directionWithLength = this.end.sub(this.start);
        final double length = directionWithLength.length();
        final Vector3d direction = directionWithLength.normalize();
        if (direction.lengthSquared() == 0) {
            throw new IllegalStateException("The start and end must be two different vectors");
        }

        final ServerWorld serverWorld = Sponge.getServer().getWorldManager().getWorld(this.world)
                .orElseThrow(() -> new IllegalStateException("World with key " + this.world.getFormatted() + " is not loaded!"));

        Vector3i currentBlock = this.initialBlock(direction);
        final LocatableBlock initialBlock = serverWorld.getLocatableBlock(currentBlock);
        if (this.select.test(initialBlock)) {
            return Optional.of(new SpongeRayTraceResult<>(initialBlock, this.start));
        }

        final Vector3i steps = this.createSteps(direction);

        // The ray equation is, vec(u) + t vec(d). From a point (x, y), there is a t
        // that we need to traverse to get to a boundary. We work that out now...
        TData currentData = this.createInitialTData(direction);
        while (currentData.getTotalTWithNextStep() < length) {
            currentBlock = this.getNextBlock(currentBlock, currentData, steps);
            currentData = this.advance(currentData, direction);

            final LocatableBlock locatableBlock = serverWorld.getLocatableBlock(currentBlock);
            if (this.continueWhile != null && !this.continueWhile.test(locatableBlock)) {
                return Optional.empty();
            }
            if (this.select.test(locatableBlock)) {
                final Vector3d hitLocation = this.start.add(direction.mul(currentData.getTotalT()));
                return Optional.of(new SpongeRayTraceResult<>(locatableBlock, hitLocation));
            }
        }

        return Optional.empty();
    }

}
