package com.xcompwiz.mystcraft.entity;

import com.xcompwiz.mystcraft.item.ItemLinking;
import com.xcompwiz.mystcraft.registry.MystEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 0.13.7.06-style physical Linking/Descriptive Book.
 *
 * <p>CP267 deliberately uses a plain Entity instead of PathfinderMob.  The old
 * book behaved like a damageable world object, but it never needed AI,
 * navigation, follow-range or any other Mob attribute.  Keeping those modern
 * Mob systems attached caused construction to fail before the entity could even
 * spawn.  Physics, damage, persistence and interaction are implemented here
 * directly instead.</p>
 */
public final class EntityLinkbook extends Entity {
    private static final EntityDataAccessor<ItemStack> BOOK =
            SynchedEntityData.defineId(EntityLinkbook.class, EntityDataSerializers.ITEM_STACK);

    private int decayTimer;
    private int wetTicks;
    private float bookHealth = 10.0F;

    public EntityLinkbook(EntityType<? extends EntityLinkbook> type, Level level) {
        super(type, level);
    }

    public EntityLinkbook(Level level, Entity location, ItemStack stack) {
        this(MystEntities.LINKBOOK.get(), level);
        setBook(stack.copyWithCount(1));
        setPos(location.getX(), location.getY(), location.getZ());
        setYRot(location.getYRot());
        setXRot(location.getXRot());
        setDeltaMovement(location.getDeltaMovement());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(BOOK, ItemStack.EMPTY);
    }

    public ItemStack getBook() {
        return entityData.get(BOOK);
    }

    public void setBook(ItemStack stack) {
        ItemStack stored = stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        entityData.set(BOOK, stored);
        if (!stored.isEmpty()) {
            float max = Math.max(1.0F, stored.getMaxDamage());
            bookHealth = Math.max(0.0F, max - stored.getDamageValue());
        }
    }

    private void syncDamageToBook() {
        ItemStack stack = getBook();
        if (stack.isEmpty() || !stack.isDamageableItem()) return;
        int damage = Math.max(0, Math.min(stack.getMaxDamage(), Math.round(stack.getMaxDamage() - bookHealth)));
        if (stack.getDamageValue() != damage) {
            ItemStack copy = stack.copy();
            copy.setDamageValue(damage);
            entityData.set(BOOK, copy);
        }
    }

    private boolean damageBook(DamageSource source, float amount) {
        if (isRemoved() || getBook().isEmpty() || amount <= 0.0F) return false;
        if (source.is(DamageTypes.IN_WALL)) return false;
        float adjusted = source.is(DamageTypeTags.IS_FIRE) ? amount * 2.0F : amount;
        bookHealth -= adjusted;
        syncDamageToBook();
        if (bookHealth <= 0.0F) discard();
        return true;
    }

    @Override
    public void tick() {
        super.tick();

        if (getBook().isEmpty()) {
            if (!level().isClientSide) discard();
            return;
        }

        // Small dropped-object physics without Mob navigation/attributes.
        Vec3 motion = getDeltaMovement();
        if (!isNoGravity()) motion = motion.add(0.0D, -0.04D, 0.0D);
        double impactY = motion.y;
        setDeltaMovement(motion);
        move(MoverType.SELF, motion);

        Vec3 moved = getDeltaMovement();
        if (onGround()) {
            // Preserve a little legacy item-like slide, and convert a hard landing
            // into real book damage.  This keeps "drop it from a height" dangerous.
            if (impactY < -0.65D && !level().isClientSide && level() instanceof ServerLevel serverLevel) {
                float damage = (float) Math.max(1.0D, Math.ceil((-impactY - 0.55D) * 6.0D));
                damageBook(serverLevel.damageSources().fall(), damage);
            }
            setDeltaMovement(moved.x * 0.70D, Math.abs(moved.y) < 0.08D ? 0.0D : moved.y * -0.35D, moved.z * 0.70D);
        } else {
            setDeltaMovement(moved.scale(0.98D));
        }

        if (level().isClientSide || !(level() instanceof ServerLevel serverLevel)) return;

        ++decayTimer;

        // Legacy books lasted roughly five seconds in water / exposed rain or snow.
        if (isInWaterOrRain()) {
            ++wetTicks;
            if (wetTicks % 10 == 0) damageBook(serverLevel.damageSources().drown(), 1.0F);
        } else {
            wetTicks = 0;
        }

        if (isInLava() && tickCount % 10 == 0) {
            damageBook(serverLevel.damageSources().lava(), 1.0F);
        } else if (isOnFire() && tickCount % 20 == 0) {
            damageBook(serverLevel.damageSources().inFire(), 1.0F);
        }

        // Original EntityLinkbook also very slowly weathered even when protected.
        if (decayTimer % 10000 == 0) {
            damageBook(serverLevel.damageSources().starve(), 1.0F);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide) return false;
        return damageBook(source, amount);
    }

    /**
     * Plain Entity defaults are not necessarily targetable by the player interaction ray.
     * Legacy EntityLinkbook inherited this behaviour from EntityLiving; after CP267 moved
     * the implementation to a lightweight Entity we must opt back into pickability so
     * right-click GUI interaction and sneak-empty-hand recovery can actually reach interact().
     */
    @Override
    public boolean isPickable() {
        return !isRemoved() && !getBook().isEmpty();
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (level().isClientSide) return InteractionResult.SUCCESS;

        if (player.isShiftKeyDown() && player.getItemInHand(hand).isEmpty()) {
            ItemStack stack = getBook().copy();
            if (!stack.isEmpty()) {
                if (player.getItemInHand(hand).isEmpty()) player.setItemInHand(hand, stack);
                else if (!player.getInventory().add(stack)) player.drop(stack, false);
                discard();
            }
            return InteractionResult.CONSUME;
        }

        if (player instanceof ServerPlayer serverPlayer && getBook().getItem() instanceof ItemLinking) {
            ItemLinking.openEntityBookMenu(serverPlayer, getId(), getBook());
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("DecayTimer", decayTimer);
        tag.putInt("WetTicks", wetTicks);
        tag.putFloat("BookHealth", bookHealth);
        ItemStack stack = getBook();
        if (!stack.isEmpty()) tag.put("Item", stack.saveOptional(level().registryAccess()));
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        decayTimer = tag.getInt("DecayTimer");
        wetTicks = tag.getInt("WetTicks");
        ItemStack stack = ItemStack.parseOptional(level().registryAccess(), tag.getCompound("Item"));
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemLinking)) {
            discard();
            return;
        }
        setBook(stack);
        if (tag.contains("BookHealth")) {
            bookHealth = Math.max(0.0F, tag.getFloat("BookHealth"));
            syncDamageToBook();
        }
    }
}
