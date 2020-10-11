/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.common.world.volume;

import net.minecraft.util.math.BlockPos;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.data.Key;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.value.Value;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.fluid.FluidState;
import org.spongepowered.api.util.AABB;
import org.spongepowered.api.world.volume.entity.MutableEntityVolume;
import org.spongepowered.api.world.volume.stream.StreamOptions;
import org.spongepowered.api.world.volume.stream.VolumeStream;
import org.spongepowered.math.vector.Vector3d;
import org.spongepowered.math.vector.Vector3i;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

public class MemoryBackedEntityVolume extends MemoryBackedObjectVolume<Entity> implements MutableEntityVolume<MemoryBackedEntityVolume> {

    public MemoryBackedEntityVolume(Vector3i min, Vector3i max) {
        super(min, max);
    }

    public void addEntity(net.minecraft.entity.Entity entity) {

    }

    @Override
    public <E extends Entity> E createEntity(EntityType<E> type, Vector3d position) throws IllegalArgumentException, IllegalStateException {
        return null;
    }

    @Override
    public <E extends Entity> E createEntityNaturally(EntityType<E> type, Vector3d position
    ) throws IllegalArgumentException, IllegalStateException {
        return null;
    }

    @Override
    public Optional<Entity> createEntity(DataContainer entityContainer) {
        return Optional.empty();
    }

    @Override
    public Optional<Entity> createEntity(DataContainer entityContainer, Vector3d position) {
        return Optional.empty();
    }

    @Override
    public boolean spawnEntity(Entity entity) {
        return false;
    }

    @Override
    public Collection<Entity> spawnEntities(Iterable<? extends Entity> entities) {
        return null;
    }

    @Override
    public boolean setBlock(int x, int y, int z, BlockState block) {
        return false;
    }

    @Override
    public boolean removeBlock(int x, int y, int z) {
        return false;
    }

    @Override
    public VolumeStream<MemoryBackedEntityVolume, BlockState> getBlockStateStream(Vector3i min, Vector3i max, StreamOptions options
    ) {
        return null;
    }

    @Override
    public BlockState getBlock(int x, int y, int z) {
        return null;
    }

    @Override
    public FluidState getFluid(int x, int y, int z) {
        return null;
    }

    @Override
    public int getHighestYAt(int x, int z) {
        return 0;
    }

    @Override
    public VolumeStream<MemoryBackedEntityVolume, Entity> getEntityStream(Vector3i min, Vector3i max, StreamOptions options
    ) {
        return null;
    }

    @Override
    public Collection<? extends Player> getPlayers() {
        return null;
    }

    @Override
    public Optional<Entity> getEntity(UUID uuid) {
        return Optional.empty();
    }

    public @Nullable net.minecraft.entity.Entity getEntity(BlockPos pos) {
        return null;
    }

    @Override
    public <T extends Entity> Collection<? extends T> getEntities(Class<? extends T> entityClass, AABB box,
        @Nullable Predicate<? super T> predicate
    ) {
        return null;
    }

    @Override
    public Collection<? extends Entity> getEntities(AABB box, Predicate<? super Entity> filter) {
        return null;
    }

    @Override
    public <E> Optional<E> get(int x, int y, int z, Key<? extends Value<E>> key) {
        return Optional.empty();
    }

    @Override
    public <E, V extends Value<E>> Optional<V> getValue(int x, int y, int z, Key<V> key) {
        return Optional.empty();
    }

    @Override
    public boolean supports(int x, int y, int z, Key<?> key) {
        return false;
    }

    @Override
    public Set<Key<?>> getKeys(int x, int y, int z) {
        return null;
    }

    @Override
    public Set<Value.Immutable<?>> getValues(int x, int y, int z) {
        return null;
    }
}
