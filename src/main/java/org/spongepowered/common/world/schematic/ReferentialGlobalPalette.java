package org.spongepowered.common.world.schematic;

import net.minecraft.util.ObjectIntIdentityMap;
import org.spongepowered.api.world.schematic.Palette;
import org.spongepowered.api.world.schematic.PaletteType;

import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class ReferentialGlobalPalette<T> implements Palette.Immutable<T> {
    private ObjectIntIdentityMap<T> registryReference;
    private final PaletteType<T> paletteType;


    @Override
    public PaletteType<T> getType() {
        return this.paletteType;
    }

    @Override
    public int getHighestId() {
        return this.registryReference.size() - 1;
    }

    @Override
    public Optional<T> get(int id) {
        return Optional.ofNullable(this.registryReference.getByValue(id));
    }

    @Override
    public Optional<Integer> get(T type) {
        return Optional.ofNullable(this.registryReference.get(type));
    }

    @Override
    public Stream<T> getEntries() {
        return StreamSupport.stream(this.registryReference.spliterator(), false);
    }

    @Override
    public Mutable<T> asMutable() {
        this.
        return new MutableBimapPalette<>(this.paletteType, );
    }
}
