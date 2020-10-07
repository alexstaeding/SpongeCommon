package org.spongepowered.common.util.raytrace;

import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.util.Tuple;
import org.spongepowered.api.util.blockray.RayTraceResult;
import org.spongepowered.api.world.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.common.util.VecHelper;
import org.spongepowered.math.vector.Vector3d;
import org.spongepowered.math.vector.Vector3i;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public final class SpongeEntityRayTrace extends AbstractSpongeRayTrace<@NonNull Entity> {

    private static final Predicate<Entity> DEFAULT_FILTER = entity -> true;

    public SpongeEntityRayTrace() {
        super(SpongeEntityRayTrace.DEFAULT_FILTER);
    }

    @Override
    @NonNull
    public Optional<RayTraceResult<@NonNull Entity>> execute() {
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
        final Vector3i steps = this.createSteps(direction);

        // The ray equation is, vec(u) + t vec(d). From a point (x, y), there is a t
        // that we need to traverse to get to a boundary. We work that out now...
        TData tData = this.createInitialTData(direction);
        Vector3d currentLocation = new Vector3d(this.start.getX(), this.start.getY(), this.start.getZ());

        while (tData.getTotalT() < length) {

            final AxisAlignedBB targetAABB = new AxisAlignedBB(currentBlock.getX(),
                    currentBlock.getY(), currentBlock.getZ(), currentBlock.getX() + 1, currentBlock.getY() + 1, currentBlock.getZ() + 1);
            final List<net.minecraft.entity.Entity> entityList =
                    ((World) serverWorld).getEntitiesInAABBexcluding(null, targetAABB, (Predicate) this.select);
            final List<net.minecraft.entity.Entity> failiures = this.continueWhile == null ? Collections.emptyList() :
                    ((World) serverWorld).getEntitiesInAABBexcluding(null, targetAABB, (Predicate) this.continueWhile.negate());

            final Vec3d vec3dstart = VecHelper.toVec3d(currentLocation);
            // As this iteration is for the CURRENT block location, we need to check where we are with the filter.
            if (this.continueWhileLocation != null && !this.continueWhileLocation.test(ServerLocation.of(serverWorld, currentBlock))) {
                return Optional.empty();
            }
            final Vec3d vec3dend;
            if (tData.getTotalTWithNextStep() > length) {
                vec3dend = VecHelper.toVec3d(this.end);
            } else {
                currentLocation = currentLocation.add(
                        direction.getX() * tData.getNextStep(),
                        direction.getY() * tData.getNextStep(),
                        direction.getZ() * tData.getNextStep()
                );
                vec3dend = VecHelper.toVec3d(currentLocation);
                currentBlock = this.getNextBlock(currentBlock, tData, steps);
                tData = this.advance(tData, direction);
            }

            double failureDistance = Double.MAX_VALUE;
            for (final net.minecraft.entity.Entity entity : failiures) {
                final Optional<Vec3d> vec3d = entity.getBoundingBox().rayTrace(vec3dstart, vec3dend);
                if (vec3d.isPresent()) {
                    final Vec3d hitPosition = vec3d.get();
                    final double sqdist = hitPosition.squareDistanceTo(vec3dstart);
                    if (sqdist < failureDistance) {
                        failureDistance = sqdist;
                    }
                }
            }

            Tuple<Double, RayTraceResult<@NonNull Entity>> returnedEntity = null;
            for (final net.minecraft.entity.Entity entity : entityList) {
                final Optional<Vec3d> vec3d = entity.getBoundingBox().rayTrace(vec3dstart, vec3dend);
                if (vec3d.isPresent()) {
                    final Vec3d hitPosition = vec3d.get();
                    final double sqdist = hitPosition.squareDistanceTo(vec3dstart);
                    if (sqdist < failureDistance && (returnedEntity == null || sqdist < returnedEntity.getFirst())) {
                        returnedEntity = Tuple.of(sqdist, new SpongeRayTraceResult<>((Entity) entity, VecHelper.toVector3d(hitPosition)));
                    }
                }
            }

            if (returnedEntity != null) {
                return Optional.of(returnedEntity.getSecond());
            } else if (!failiures.isEmpty()) {
                return Optional.empty();
            }
        }

        return Optional.empty();
    }

}
