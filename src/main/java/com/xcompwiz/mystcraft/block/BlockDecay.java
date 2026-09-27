package com.xcompwiz.mystcraft.block;

import com.mojang.serialization.MapCodec;
import com.xcompwiz.mystcraft.Mystcraft;
import com.xcompwiz.mystcraft.instability.LegacyInstabilityDecayRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.MapColor;

/** Single legacy blockdecay ID with the four active 0.13.7.06 colour states. */
public final class BlockDecay extends Block {
    public static final MapCodec<BlockDecay> CODEC = simpleCodec(BlockDecay::new);
    public static final EnumProperty<DecayType> DECAY = EnumProperty.create("decay", DecayType.class);
    public static final BooleanProperty MANUAL = BooleanProperty.create("manual");

    public enum DecayType implements StringRepresentable {
        RED("red"), BLUE("blue"), PURPLE("purple"), WHITE("white");
        private final String name;
        DecayType(String name) { this.name = name; }
        @Override public String getSerializedName() { return name; }
    }

    public BlockDecay() {
        this(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_RED)
                .strength(0.5F, 2.5F)
                .sound(SoundType.SAND)
                .noLootTable());
    }

    public BlockDecay(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(DECAY, DecayType.RED).setValue(MANUAL, false));
    }

    @Override protected MapCodec<BlockDecay> codec() { return CODEC; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DECAY, MANUAL);
    }


    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        super.entityInside(state, level, pos, entity);
        whiteContact(state, level, entity);
    }

    /**
     * Legacy 0.13.7.06 called the Decay handler both when an entity intersected the block and
     * when it walked over the top surface ({@code onEntityWalk}).  The first CP299 live bridge
     * only restored the intersection path, which made a player standing/walking on White Decay
     * incorrectly safe.  Modern {@code stepOn} is the matching surface-contact hook.
     */
    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        super.stepOn(level, pos, state, entity);
        whiteContact(state, level, entity);
    }

    private static void whiteContact(BlockState state, Level level, Entity entity) {
        if (state.getValue(DECAY) == DecayType.WHITE) {
            LegacyInstabilityDecayRuntime.whiteDecayContact(level, entity);
        }
    }
}
