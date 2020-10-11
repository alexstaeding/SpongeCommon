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
import net.minecraft.world.biome.Biome;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.entity.BlockEntity;
import org.spongepowered.api.data.Key;
import org.spongepowered.api.data.value.Value;
import org.spongepowered.api.fluid.FluidState;
import org.spongepowered.api.world.biome.BiomeType;
import org.spongepowered.api.world.volume.biome.MutableBiomeVolume;
import org.spongepowered.api.world.volume.block.entity.MutableBlockEntityVolume;
import org.spongepowered.api.world.volume.stream.StreamOptions;
import org.spongepowered.api.world.volume.stream.VolumeStream;
import org.spongepowered.math.vector.Vector3i;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

public class MemoryBackedBiomeVolume extends MemoryBackedObjectVolume<BiomeType> implements MutableBiomeVolume<MemoryBackedBiomeVolume> {

    public MemoryBackedBiomeVolume(Vector3i min, Vector3i max) {
        super(min, max);
    }

    @Override
    public boolean setBiome(int x, int y, int z, BiomeType biome) {
        return false;
    }

    public boolean setBiome(BlockPos pos, Biome biome) {

    }

    @Override
    public VolumeStream<MemoryBackedBiomeVolume, BiomeType> getBiomeStream(Vector3i min, Vector3i max, StreamOptions options
    ) {
        return null;
    }

    @Override
    public BiomeType getBiome(int x, int y, int z) {
        return null;
    }

    public Biome getBiome(BlockPos pos) {
        return null;
    }
}
