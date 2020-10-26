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
package org.spongepowered.common.bridge.data;

import com.google.common.collect.ImmutableList;
import com.google.common.reflect.TypeToken;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.data.DataHolder;
import org.spongepowered.api.data.DataManipulator;
import org.spongepowered.api.data.DataTransactionResult;
import org.spongepowered.api.data.Key;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataContentUpdater;
import org.spongepowered.api.data.persistence.DataStore;
import org.spongepowered.api.data.persistence.DataView;
import org.spongepowered.api.data.persistence.Queries;
import org.spongepowered.api.data.value.Value;
import org.spongepowered.common.data.SpongeDataManager;
import org.spongepowered.common.data.persistence.NbtTranslator;
import org.spongepowered.common.util.Constants;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public interface CustomDataHolderBridge {

    static void serializeCustomData(final DataCompoundHolder object) {
        CompoundNBT compound = object.data$getCompound();
        if (!(object instanceof CustomDataHolderBridge)) {
            return;
        }

        final DataManipulator.Mutable manipulator = ((CustomDataHolderBridge) object).bridge$getManipulator();
        final DataHolder dataHolder = (DataHolder) object;
        final TypeToken<? extends DataHolder> dataHolderType = TypeToken.of(dataHolder.getClass());

        final Set<DataStore> dataStores = manipulator.getKeys().stream()
                .map(key -> SpongeDataManager.getDatastoreRegistry().getDataStore(key, dataHolderType))
                .collect(Collectors.toSet());
        if (!dataStores.isEmpty()) {
            final DataContainer dataContainer = NbtTranslator.getInstance().translate(compound);
            for (DataStore dataStore : dataStores) {
                dataStore.serialize(manipulator, dataContainer);
            }
            final CompoundNBT serialized = NbtTranslator.getInstance().translate(dataContainer);
            compound.merge(serialized); // TODO does this work?
        }

        final List<DataView> failedData = ((CustomDataHolderBridge) object).bridge$getFailedData();
        if (!failedData.isEmpty()) {
            final ListNBT failedList = new ListNBT();
            for (final DataView failedDatum : failedData) {
                failedList.add(NbtTranslator.getInstance().translate(failedDatum));
            }
            compound.put(Constants.Sponge.FAILED_CUSTOM_DATA, failedList);
        } else {
            compound.remove(Constants.Sponge.FAILED_CUSTOM_DATA);
        }
    }

    static void deserializeCustomData(final DataCompoundHolder object) {
        final CompoundNBT compound = object.data$getCompound();
        if (compound == null) {
            return;
        }
        if (!(object instanceof CustomDataHolderBridge && object.data$getSpongeData().contains(Constants.Sponge.CUSTOM_MANIPULATOR_TAG_LIST, Constants.NBT.TAG_LIST))) {
            return;
        }
        final ListNBT list = object.data$getSpongeData().getList(Constants.Sponge.CUSTOM_MANIPULATOR_TAG_LIST, Constants.NBT.TAG_COMPOUND);
        if (list.isEmpty()) {
            return;
        }
        // There is some data present
        final DataContainer allData = NbtTranslator.getInstance().translate(compound);
        final DataView spongeData = allData.getView(Constants.Forge.ROOT).flatMap(forgeData -> forgeData.getView(Constants.Sponge.SPONGE_ROOT)).get();
        // Update data
        final List<DataView> updatedDataViews = spongeData.getViewList(Constants.Sponge.CUSTOM_MANIPULATOR_LIST).get()
                        .stream().map(CustomDataHolderBridge::updateDataViewForDataManipulator).collect(Collectors.toList());
        spongeData.set(Constants.Sponge.CUSTOM_MANIPULATOR_LIST, updatedDataViews);

        final TypeToken<? extends DataHolder> typeToken = TypeToken.of((Class<? extends DataHolder>)object.getClass());
        // Find DataStores
        final List<DataStore> dataStores = new ArrayList<>();
        final ImmutableList.Builder<DataView> failed = ImmutableList.builder();
        for (DataView dataView : updatedDataViews) {
            final Optional<DataStore> dataStore = dataView.getString(Constants.Sponge.DATA_ID)
                    .flatMap(id -> SpongeDataManager.getInstance().getRegistration(ResourceKey.resolve(id)))
                    .flatMap(r -> r.getDataStore(typeToken));
            if (dataStore.isPresent()) {
                dataStores.add(dataStore.get());
            } else {
                // If no datastore was found add this to failed data
                failed.add(dataView);
            }
        }


        for (DataStore dataStore : dataStores) {
            // Deserialize to Manipulator
            final DataManipulator.Mutable manipulator = dataStore.deserialize(allData);
            // Set data in CustomDataHolderBridge
            ((CustomDataHolderBridge) object).bridge$mergeDeserialized(manipulator);
        }

        ((CustomDataHolderBridge) object).bridge$addFailedData(failed.build());

        CustomDataHolderBridge.syncCustomToTag(object);
    }

    static void syncTagToCustom(Object dataHolder) {
        if (dataHolder instanceof DataCompoundHolder) {
            if (((DataCompoundHolder) dataHolder).data$hasSpongeData()) {
                final DataCompoundHolder compoundHolder = (DataCompoundHolder) dataHolder;
                CompoundNBT compound = compoundHolder.data$getCompound();
                if (compound == null) {
                    return;
                }
                CustomDataHolderBridge.deserializeCustomData((DataCompoundHolder) dataHolder);
            }
        }
    }

    static void syncCustomToTag(Object dataHolder) {
        if (dataHolder instanceof DataCompoundHolder) {
            final DataCompoundHolder compoundHolder = (DataCompoundHolder) dataHolder;
            CompoundNBT compound = compoundHolder.data$getCompound();
            if (compound == null) {
                compound = new CompoundNBT();
                compoundHolder.data$setCompound(compound);
            }
            CustomDataHolderBridge.serializeCustomData((DataCompoundHolder) dataHolder);
            compoundHolder.data$cleanEmptySpongeData();
        }
    }

    void bridge$mergeDeserialized(DataManipulator.Mutable manipulator);

    static DataView updateDataViewForDataManipulator(final DataView dataView) {
        final int version = dataView.getInt(Queries.CONTENT_VERSION).orElse(1);
        if (version != Constants.Sponge.CURRENT_CUSTOM_DATA) {
            final DataContentUpdater contentUpdater = SpongeDataManager.getInstance().getWrappedCustomContentUpdater(DataManipulator.Mutable.class, version, Constants.Sponge.CURRENT_CUSTOM_DATA)
                    .orElseThrow(() -> new IllegalArgumentException("Could not find a content updater for DataManipulator information with version: " + version));
            return contentUpdater.update(dataView);
        }
        return dataView;
    }

    void bridge$clearCustomData();

    default <E> Optional<E> bridge$getCustom(Key<? extends Value<E>> key) {
        return this.bridge$getManipulator().get(key);
    }

    default <E> DataTransactionResult bridge$offerCustom(Key<? extends Value<E>> key, E value) {
        final DataManipulator.Mutable manipulator = this.bridge$getManipulator();
        final Value.Immutable<E> immutableValue = manipulator.getValue(key).map(Value::asImmutable).orElse(null);
        final DataTransactionResult.Builder builder = DataTransactionResult.builder();
        if (immutableValue != null) {
            builder.replace(immutableValue);
        }
        manipulator.set(key, value);
        builder.success(manipulator.getValue(key).get().asImmutable());

        syncCustomToTag(this);

        return builder.result(DataTransactionResult.Type.SUCCESS).build();
    }

    default <E> DataTransactionResult bridge$removeCustom(Key<? extends Value<E>> key) {
        final Optional<? extends Value<E>> value = this.bridge$getManipulator().getValue(key);
        return value.map(Value::asImmutable).map(DataTransactionResult::successRemove)
                .orElseGet(DataTransactionResult::successNoData);
    }

    DataManipulator.Mutable bridge$getManipulator();

    default void bridge$addFailedData(ImmutableList<DataView> failedData) {
        this.bridge$getFailedData().addAll(failedData);
    }

    List<DataView> bridge$getFailedData();
}
