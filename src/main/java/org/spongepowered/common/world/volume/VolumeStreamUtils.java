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

import net.minecraft.block.BlockState;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.IChunk;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.world.volume.game.ReadableRegion;
import org.spongepowered.api.world.volume.stream.StreamOptions;
import org.spongepowered.api.world.volume.stream.VolumeElement;
import org.spongepowered.api.world.volume.stream.VolumeStream;
import org.spongepowered.common.util.VecHelper;
import org.spongepowered.math.vector.Vector3i;

import java.lang.ref.WeakReference;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public final class VolumeStreamUtils {

    private VolumeStreamUtils() {}

    /**
     * Creates a {@link Supplier Supplier&lt;T&gt;} that weakly references the
     * provided {@code object} such that the object will not be referenced otherwise
     * within the Supplier itself, allowing it to be freely garbage collected, in the
     * circumstance the reference to the supplier is retained for an extended time.
     *
     * @param object The object being referenced
     * @param name The name of the object, for error printing
     * @param <T> The type of object requested
     * @return The supplier
     */
    public static <T> Supplier<T> createWeaklyReferencedSupplier(final T object, final String name) {
        final WeakReference<T> weakReference = new WeakReference<>(object);
        return () -> {
            final @Nullable T weaklyReferenced = weakReference.get();
            return Objects.requireNonNull(weaklyReferenced, () -> String.format("%s de-referenced!", name));
        };
    }

    public static <R extends ReadableRegion<R>> BiFunction<R, ChunkPos, @Nullable Chunk> getChunkAccessorByStatus(
        final IWorldReader worldReader,
        final boolean shouldGenerate
    ) {
        return (world, chunkPos) -> {
            final ChunkStatus chunkStatus = shouldGenerate
                ? ChunkStatus.EMPTY
                : ChunkStatus.FULL;
            final @Nullable IChunk ichunk = worldReader.getChunk(chunkPos.x, chunkPos.z, chunkStatus, shouldGenerate);
            if (shouldGenerate) {
                Objects.requireNonNull(ichunk, "Chunk was expected to load fully and generate, but somehow got a null chunk!");
            }
            return (Chunk) ichunk;
        };
    }

    /**
     *
     * @return
     */
    public static Function<Chunk, Stream<Map.Entry<BlockPos, Biome>>> getBiomesForChunkByPos() {
        return VolumeStreamUtils.getElementByPosition(VolumeStreamUtils.chunkSectionBiomeGetter());
    }

    public static Function<Chunk, Stream<Map.Entry<BlockPos, BlockState>>> getBlockStatesForSections() {
        return VolumeStreamUtils.getElementByPosition(VolumeStreamUtils.chunkSectionBlockStateGetter());
    }

    private interface TriFunction<A, B, C, Out> {
        Out apply(A a, B b, C c);
    }

    private static TriFunction<Chunk, ChunkSection, BlockPos, Biome> chunkSectionBiomeGetter() {
        return ((chunk, chunkSection, pos) -> chunk.getBiomes() == null
            ? chunk.getWorld().getNoiseBiomeRaw(pos.getX(), pos.getY(), pos.getZ())
            : chunk.getBiomes().getNoiseBiome(pos.getX(), pos.getY(), pos.getZ()));
    }

    private static TriFunction<Chunk, ChunkSection, BlockPos, BlockState> chunkSectionBlockStateGetter() {
        return ((chunk, chunkSection, pos) -> chunkSection.getBlockState(
            pos.getX() - (chunk.getPos().x << 4),
            pos.getY() - chunkSection.getYLocation(),
            pos.getZ() - (chunk.getPos().z << 4)));
    }

    private static <T> Function<Chunk, Stream<Map.Entry<BlockPos, T>>> getElementByPosition(TriFunction<Chunk, ChunkSection, BlockPos, T> elementAccessor) {
        return chunk -> {
            final ChunkPos pos = chunk.getPos();
            return Arrays.stream(chunk.getSections()).flatMap(
                chunkSection -> IntStream.range(0, 16)
                    .mapToObj(z -> IntStream.range(0, 16)
                        .mapToObj(x -> IntStream.range(0, 16)
                            .mapToObj(y ->
                                {
                                    final BlockPos blockPos = new BlockPos(x + pos.x << 4, y + chunkSection.getYLocation(), z + pos.z << 4);
                                    return new AbstractMap.SimpleEntry<>(blockPos, elementAccessor.apply(chunk, chunkSection, blockPos));
                                }
                            )))
                    .flatMap(Function.identity())
                    .flatMap(Function.identity())
            );
        };
    }

    @SuppressWarnings({"unchecked"})
    public static <R extends ReadableRegion<R>, API, MC, Section, KeyReference> VolumeStream<R, API> generateStream(
        final Vector3i min,
        final Vector3i max,
        final StreamOptions options,
        final R ref,
        final BiConsumer<KeyReference, MC> identityFunction,
        final BiFunction<R, ChunkPos, Section> chunkAccessor,
        final BiFunction<BlockPos, MC, KeyReference> entityToKey,
        final Function<Section, Stream<Map.Entry<BlockPos, MC>>> entityAccessor,
        final BiFunction<KeyReference, R, Tuple<BlockPos, MC>> filteredPositionEntityAccessor
    ) {
        final Supplier<R> worldSupplier = VolumeStreamUtils.createWeaklyReferencedSupplier(ref, "World");
        final BlockPos chunkMin = new BlockPos(min.getX() >> 4, 0, min.getZ() >> 4);
        final BlockPos chunkMax = new BlockPos(max.getX() >> 4, 0, max.getZ() >> 4);

        // Generate the chunk position stream to iterate on, whether they're accessed immediately
        // or lazily is up to the stream options.
        final Stream<ChunkPos> chunkPosStream = IntStream.range(chunkMin.getX(), chunkMax.getX() + 1)
            .mapToObj(x -> IntStream.range(chunkMin.getZ(), chunkMax.getZ() + 1).mapToObj(z -> new ChunkPos(x, z)))
            .flatMap(Function.identity());

        // This effectively creates a weakly referenced object supplier casting the MC variant to the API variant
        // without consideration, assuming the MC variant is always mixed in to implement the API variant.
        // Then constructs the VolumeElement
        final Function<Tuple<BlockPos, MC>, VolumeElement<R, API>> elementGenerator = (tuple) -> {
            final Supplier<API> blockEntitySupplier = VolumeStreamUtils.createWeaklyReferencedSupplier((API) tuple.getB(), "Element");
            final Vector3i blockEntityPos = VecHelper.toVector3i(tuple.getA());
            return VolumeElement.of(worldSupplier, blockEntitySupplier, blockEntityPos);
        };
        // Fairly trivial, but just acts as a filter and provides the set of filtered references back to the `poses`
        // passed in. This effectively builds the set of key references by their key, usually passing the entity
        // to the identity function whether the entity is to be "cloned" or merely retained by key. This is useful
        // compared to a traditional filter operation since the identity function renders the entity completely
        // separated from the volume target in the event of transformational operations being run on the VolumeStream
        // itself.
        final BiConsumer<Map.Entry<BlockPos, MC>, Set<KeyReference>> entryConsumer = (entry, poses) -> {
            final BlockPos pos = entry.getKey();
            final Vector3i v = VecHelper.toVector3i(pos);
            if (v.compareTo(min) >= 0 && v.compareTo(max) <= 0) {
                final KeyReference keyRef = entityToKey.apply(pos, entry.getValue());
                poses.add(keyRef);
                identityFunction.accept(keyRef, entry.getValue());
            }
        };
        // The stream of filtered key references, whether they're BlockPos or UUID,
        // depending on how the stream is being constructed, (immediate loading or not)
        // the positions can be dynamically generated by a stream, or can be pre-calculated
        // and offered as a pre-initialized collection of keys.
        final Stream<KeyReference> filteredPosStream;
        if (options.loadingStyle().immediateLoading()) {
            final Set<KeyReference> availableTileEntityPositions = new LinkedHashSet<>();
            chunkPosStream
                .map(pos -> chunkAccessor.apply(ref, pos))
                .map(entityAccessor)
                .forEach((map) -> map.forEach(entry -> entryConsumer.accept(entry, availableTileEntityPositions)));
            filteredPosStream = availableTileEntityPositions.stream();
        } else {
            // This is where the entirety of stream lazy evaluation occurs:
            // Since we're operating on the chunk positions, we generate the Stream of keys
            // for each position, which in turn generate their filtered lists on demand.
            filteredPosStream = chunkPosStream
                .flatMap(chunkPos -> {
                    final Set<KeyReference> blockEntityPoses = new LinkedHashSet<>();
                    entityAccessor.apply(chunkAccessor.apply(ref, chunkPos))
                        .forEach(entry -> entryConsumer.accept(entry, blockEntityPoses));
                    return blockEntityPoses.stream();
                });
        }
        // And finally, the complete stream turning objects into VolumeElements.
        final Stream<VolumeElement<R, API>> volumeStreamBacker = filteredPosStream
            .map(pos -> filteredPositionEntityAccessor.apply(pos, ref))
            .filter(tuple -> Objects.nonNull(tuple.getB()))
            .map(elementGenerator);
        return new SpongeVolumeStream<>(volumeStreamBacker, worldSupplier);
    }

}
