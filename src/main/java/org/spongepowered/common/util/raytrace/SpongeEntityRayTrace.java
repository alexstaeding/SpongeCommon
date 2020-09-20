package org.spongepowered.common.util.raytrace;

import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.util.Tuple;
import org.spongepowered.api.util.blockray.RayTrace;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.common.util.VecHelper;
import org.spongepowered.math.vector.Vector3d;
import org.spongepowered.math.vector.Vector3i;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class SpongeEntityRayTrace implements RayTrace<@NonNull Entity> {

    private static final Predicate<Entity> DEFAULT_FILTER = entity -> true;

    private Vector3d start;
    private Vector3d end;
    private ResourceKey world;
    private Predicate<Entity> filter = SpongeEntityRayTrace.DEFAULT_FILTER;

    @Override
    @NonNull
    public RayTrace<@NonNull Entity> world(@NonNull final ServerWorld serverWorld) {
        this.world = serverWorld.getKey();
        return this;
    }

    @Override
    @NonNull
    public RayTrace<@NonNull Entity> sourcePosition(@NonNull final Entity sourceEntity) {
        this.start = sourceEntity.getPosition();
        return this;
    }

    @Override
    @NonNull
    public RayTrace<@NonNull Entity> sourcePosition(@NonNull final Vector3d sourcePosition) {
        this.start = sourcePosition;
        return this;
    }

    @Override
    @NonNull
    public RayTrace<@NonNull Entity> endPosition(@NonNull final Vector3d endPosition) {
        this.end = endPosition;
        return this;
    }

    @Override
    @NonNull
    public RayTrace<@NonNull Entity> accept(@NonNull final Predicate<Entity> filter) {
        this.filter = filter;
        return this;
    }

    @Override
    @NonNull
    public Optional<Entity> execute() {
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
        Vector3d currentLocation = new Vector3d(this.start.getX(), this.start.getY(), this.start.getZ());

        double nextStep;
        while (totalTraversal < length) {

            final AxisAlignedBB targetAABB = new AxisAlignedBB(currentBlock.getX(),
                    currentBlock.getY(), currentBlock.getZ(), currentBlock.getX() + 1, currentBlock.getY() + 1, currentBlock.getZ() + 1);
            final List<net.minecraft.entity.Entity> entityList =
                    ((World) serverWorld).getEntitiesInAABBexcluding(null, targetAABB, (Predicate) this.filter);

            final Vec3d vec3dstart = VecHelper.toVec3d(currentLocation);
            nextStep = Math.min(tToX, Math.min(tToY, tToZ));
            totalTraversal += nextStep;
            final Vec3d vec3dend;
            if (totalTraversal > length) {
                vec3dend = VecHelper.toVec3d(this.end);
            } else {
                tToX -= nextStep;
                tToY -= nextStep;
                tToZ -= nextStep;
                currentLocation = currentLocation.add(
                        direction.getX() * nextStep,
                        direction.getY() * nextStep,
                        direction.getZ() * nextStep
                );
                vec3dend = VecHelper.toVec3d(currentLocation);

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

            }

            Tuple<Double, net.minecraft.entity.Entity> returnedEntity = null;
            for (final net.minecraft.entity.Entity entity : entityList) {
                final Optional<Vec3d> vec3d = entity.getBoundingBox().rayTrace(vec3dstart, vec3dend);
                if (vec3d.isPresent()) {
                    final double sqdist = vec3d.get().squareDistanceTo(vec3dstart);
                    if (returnedEntity == null || sqdist < returnedEntity.getFirst()) {
                        returnedEntity = Tuple.of(sqdist, entity);
                    }
                }
            }

            if (returnedEntity != null) {
                return Optional.of((Entity) returnedEntity.getSecond());
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
    public RayTrace<@NonNull Entity> reset() {
        this.filter = SpongeEntityRayTrace.DEFAULT_FILTER;
        this.world = null;
        this.start = null;
        this.end = null;
        return this;
    }
}
