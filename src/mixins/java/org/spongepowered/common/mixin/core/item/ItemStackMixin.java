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
package org.spongepowered.common.mixin.core.item;

import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.api.data.DataTransactionResult;
import org.spongepowered.api.data.Key;
import org.spongepowered.api.data.value.Value;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.common.bridge.data.CustomDataHolderBridge;
import org.spongepowered.common.bridge.data.DataCompoundHolder;
import org.spongepowered.common.bridge.world.WorldBridge;
import org.spongepowered.common.data.provider.nbt.NBTDataType;
import org.spongepowered.common.data.provider.nbt.NBTDataTypes;
import org.spongepowered.common.event.tracking.IPhaseState;
import org.spongepowered.common.event.tracking.PhaseContext;
import org.spongepowered.common.event.tracking.PhaseTracker;

import java.util.Optional;

import javax.annotation.Nullable;

@Mixin(net.minecraft.item.ItemStack.class)
public abstract class ItemStackMixin implements CustomDataHolderBridge, DataCompoundHolder {

    @Shadow public abstract void shadow$setTag(@Nullable CompoundNBT nbt);
    @Shadow @Nullable public abstract CompoundNBT shadow$getTag();

    @Shadow private CompoundNBT tag;

    @Shadow private boolean isEmpty;

    @Override
    public <E> DataTransactionResult bridge$offerCustom(final Key<@NonNull ? extends Value<E>> key, final E value) {
        if (this.isEmpty) {
            return DataTransactionResult.failNoData();
        }
        return CustomDataHolderBridge.super.bridge$offerCustom(key, value);
    }

    @Override
    public <E> Optional<E> bridge$getCustom(final Key<@NonNull ? extends Value<E>> key) {
        if (this.isEmpty) {
            return Optional.empty();
        }
        return CustomDataHolderBridge.super.bridge$getCustom(key);
    }

    @Override
    public <E> DataTransactionResult bridge$removeCustom(final Key<@NonNull ? extends Value<E>> key) {
        if (this.isEmpty) {
            return DataTransactionResult.failNoData();
        }
        return CustomDataHolderBridge.super.bridge$removeCustom(key);
    }

    @Override
    public CompoundNBT data$getCompound() {
        return this.shadow$getTag();
    }

    @Override
    public void data$setCompound(final CompoundNBT nbt) {
        this.shadow$setTag(nbt);
    }

    // Add our manipulators when creating copies from this ItemStack:
    @SuppressWarnings("ConstantConditions")
    @Inject(method = "copy", at = @At("RETURN"))
    private void impl$onCopy(final CallbackInfoReturnable<ItemStack> info) {
        ((CustomDataHolderBridge) (Object) info.getReturnValue()).bridge$mergeDeserialized(this.bridge$getManipulator());
    }

    @SuppressWarnings("ConstantConditions")
    @Inject(method = "split", at = @At("RETURN"))
    private void impl$onSplit(final int amount, final CallbackInfoReturnable<net.minecraft.item.ItemStack> info) {
        ((CustomDataHolderBridge) (Object) info.getReturnValue()).bridge$mergeDeserialized(this.bridge$getManipulator());
    }

    // Read custom data from nbt
    @Inject(method = "<init>(Lnet/minecraft/nbt/CompoundNBT;)V", at = @At("RETURN"))
    private void impl$onRead(final CompoundNBT compound, final CallbackInfo info) {
        if (!this.isEmpty) {
            CustomDataHolderBridge.syncCustomToTag(this);
        }
        // Prune empty stack tag compound if its empty to enable stacking.
        if (this.tag != null && this.tag.isEmpty()) {
            this.tag = null;
        }
    }

    @Redirect(method = "removeChildTag",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/nbt/CompoundNBT;remove(Ljava/lang/String;)V"))
    private void impl$nullStackCompoundIfEmptyAfterRemoval(final CompoundNBT compound, final String key) {
        compound.remove(key);
        if (compound.isEmpty()) {
            this.tag = null;
        }
    }

    @Inject(method = "setTag", at = @At("RETURN"))
    private void impl$onSet(final CompoundNBT compound, final CallbackInfo callbackInfo) {
        if (this.shadow$getTag() != compound) {
            this.bridge$clearCustomData();
        }
        CustomDataHolderBridge.syncTagToCustom(this);
    }


    @SuppressWarnings({"unchecked", "rawtypes"})
    @Inject(method = "onBlockDestroyed", at = @At("HEAD"))
    private void impl$capturePlayerUsingItemstack(final World worldIn, final BlockState blockIn, final BlockPos pos, final PlayerEntity playerIn,
        final CallbackInfo ci) {
        if (!((WorldBridge) worldIn).bridge$isFake()) {
            final PhaseContext<@NonNull ?> context = PhaseTracker.getInstance().getPhaseContext();
            final IPhaseState state = context.state;
            state.capturePlayerUsingStackToBreakBlock((org.spongepowered.api.item.inventory.ItemStack) this, (ServerPlayerEntity) playerIn, context);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Inject(method = "onBlockDestroyed", at = @At("RETURN"))
    private void impl$nullOutCapturedPlayer(final World worldIn, final BlockState blockIn, final BlockPos pos, final PlayerEntity playerIn,
        final CallbackInfo ci) {
        if (!((WorldBridge) worldIn).bridge$isFake()) {
            final PhaseContext<@NonNull ?> context = PhaseTracker.getInstance().getPhaseContext();
            final IPhaseState state = context.state;
            state.capturePlayerUsingStackToBreakBlock((org.spongepowered.api.item.inventory.ItemStack) this, null, context);
        }
    }

    @Override
    public NBTDataType data$getNbtDataType() {
        return NBTDataTypes.ITEMSTACK;
    }
}
