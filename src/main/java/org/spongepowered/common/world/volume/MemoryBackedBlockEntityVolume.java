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

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.entity.BlockEntity;
import org.spongepowered.api.data.Key;
import org.spongepowered.api.data.value.Value;
import org.spongepowered.api.fluid.FluidState;
import org.spongepowered.api.world.volume.block.entity.MutableBlockEntityVolume;
import org.spongepowered.api.world.volume.stream.StreamOptions;
import org.spongepowered.api.world.volume.stream.VolumeStream;
import org.spongepowered.math.vector.Vector3i;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

public class MemoryBackedBlockEntityVolume extends MemoryBackedObjectVolume<BlockEntity> implements MutableBlockEntityVolume<MemoryBackedBlockEntityVolume> {

    public MemoryBackedBlockEntityVolume(Vector3i min, Vector3i max) {
        super(min, max);
    }

    @Override
    public VolumeStream getBlockEntityStream(Vector3i min, Vector3i max, StreamOptions options
    ) {
        return null;
    }

    @Override
    public Collection<? extends BlockEntity> getBlockEntities() {
        return null;
    }

    @Override
    public Optional<? extends BlockEntity> getBlockEntity(int x, int y, int z) {
        return Optional.empty();
    }

    @Nullable
    public TileEntity getTileEntity(final BlockPos pos) {
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

    @Override
    public void addBlockEntity(int x, int y, int z, BlockEntity blockEntity) {

    }

    @Override
    public void removeBlockEntity(int x, int y, int z) {

    }

    @Override
    public boolean setBlock(int x, int y, int z, BlockState block) {
        return false;
    }

    public void setBlock(BlockPos pos, net.minecraft.block.BlockState state) {

    }

    public net.minecraft.block.BlockState getBlock(BlockPos pos) {
        return null;
    }

    @Override
    public boolean removeBlock(int x, int y, int z) {
        return false;
    }

    @Override
    public VolumeStream<MemoryBackedBlockEntityVolume, BlockState> getBlockStateStream(Vector3i min, Vector3i max, StreamOptions options
    ) {
        return null;
    }
}
