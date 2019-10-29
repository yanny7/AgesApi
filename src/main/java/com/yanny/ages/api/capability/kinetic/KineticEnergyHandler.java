package com.yanny.ages.api.capability.kinetic;

import com.mojang.datafixers.util.Pair;
import com.yanny.ages.api.Reference;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.WorldSavedData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class KineticEnergyHandler extends WorldSavedData {
    private static final Logger LOGGER = LogManager.getLogger(KineticEnergyHandler.class);

    private final HashSet<BlockPos> REGISTRY_GENERATOR = new HashSet<>();
    private final HashSet<BlockPos> REGISTRY_SPINDLE = new HashSet<>();
    private final HashMap<BlockPos, BlockPos> CACHE_GENERATOR_FOR_SPINDLE = new HashMap<>();
    private final HashMap<BlockPos, HashSet<BlockPos>> CACHE_SPINDLES_FOR_GENERATOR = new HashMap<>();

    private final List<BlockPos> tmpList = new ArrayList<>();
    private final World world;

    private static final String SAVE_DATA_ID = Reference.MODID + "_kinetic_energy_handler";
    private static final HashMap<World, KineticEnergyHandler> INSTANCE = new HashMap<>();

    private KineticEnergyHandler(@Nonnull World world) {
        super(SAVE_DATA_ID);
        this.world = world;
    }

    public static KineticEnergyHandler getInstance(@Nonnull World world) {
        if (world.isRemote) {
            throw new IllegalStateException("KineticEnergyHandler is only server-side");
        }

        KineticEnergyHandler handler = INSTANCE.get(world);

        if (handler == null) {
            handler = new KineticEnergyHandler(world);
            INSTANCE.put(world, handler);
        }

        if (world instanceof ServerWorld) {
            ServerWorld server = (ServerWorld) world;
            server.getSavedData().getOrCreate(() -> INSTANCE.get(world), SAVE_DATA_ID);
        }

        return handler;
    }

    public synchronized void register(@Nonnull BlockPos position, @Nonnull IKinetic entity) {
        if (position == BlockPos.ZERO) {
            return;
        }

        if (entity instanceof IKineticGenerator) {
            if (!REGISTRY_GENERATOR.contains(position)) {
                registerGenerator(position, (IKineticGenerator) entity);
            }
        }
        if (entity instanceof IKineticSpindle) {
            if (!REGISTRY_SPINDLE.contains(position)) {
                registerSpindle(position, (IKineticSpindle) entity);
            }
        }

        markDirty();
    }

    public synchronized void remove(@Nonnull BlockPos position) {
        boolean hasGenerator = REGISTRY_GENERATOR.remove(position);
        boolean hasSpindle = REGISTRY_SPINDLE.remove(position);

        if (hasSpindle) {
            removeSpindle(position);
        }
        if (hasGenerator) {
            removeGenerator(position);
        }

        markDirty();
    }

    public synchronized void notifyEnergyChanged(@Nonnull BlockPos generatorPosition) {
        HashSet<BlockPos> spindles = CACHE_SPINDLES_FOR_GENERATOR.get(generatorPosition);
        IKineticGenerator generator = getTileEntity(REGISTRY_GENERATOR, generatorPosition);

        if (generator == null) {
            LOGGER.error("notifyEnergyChanged: TileEntity at {} in world {} does not exists!", generatorPosition, world);
            return;
        }

        spindles.forEach(blockPos -> {
            IKineticSpindle spindle = getTileEntity(REGISTRY_SPINDLE, blockPos);

            if (spindle == null) {
                LOGGER.error("notifyEnergyChanged: TileEntity at {} in world {} does not exists!", blockPos, world);
                return;
            }

            spindle.onEnergyChanged(generator);
        });
    }

    @Override
    public void read(@Nonnull CompoundNBT tag) {
        CompoundNBT genTag = (CompoundNBT) tag.get("generators");
        CompoundNBT spiTag = (CompoundNBT) tag.get("spindles");
        CompoundNBT cacheGenTag = (CompoundNBT) tag.get("cacheGenerator");
        CompoundNBT cacheSpiTag = (CompoundNBT) tag.get("cacheSpindle");

        if (genTag == null || spiTag == null || cacheSpiTag == null || cacheGenTag == null) {
            return;
        }

        REGISTRY_GENERATOR.clear();
        REGISTRY_SPINDLE.clear();
        CACHE_SPINDLES_FOR_GENERATOR.clear();
        CACHE_GENERATOR_FOR_SPINDLE.clear();

        readMap(genTag, REGISTRY_GENERATOR);
        readMap(spiTag, REGISTRY_SPINDLE);
        readMapMap(cacheGenTag, CACHE_SPINDLES_FOR_GENERATOR);
        readMap(cacheSpiTag, CACHE_GENERATOR_FOR_SPINDLE);
    }

    @Nonnull
    @Override
    public CompoundNBT write(@Nonnull CompoundNBT compound) {
        CompoundNBT genTag = new CompoundNBT();
        CompoundNBT spiTag = new CompoundNBT();
        CompoundNBT cacheGenTag = new CompoundNBT();
        CompoundNBT cacheSpiTag = new CompoundNBT();

        writeMap(genTag, REGISTRY_GENERATOR);
        writeMap(spiTag, REGISTRY_SPINDLE);
        writeMapMap(cacheGenTag, CACHE_SPINDLES_FOR_GENERATOR);
        writeMap(cacheSpiTag, CACHE_GENERATOR_FOR_SPINDLE);

        compound.put("generators", genTag);
        compound.put("spindles", spiTag);
        compound.put("cacheGenerator", cacheGenTag);
        compound.put("cacheSpindle", cacheSpiTag);
        return compound;
    }

    //////////////////////////////////////////////////////////////////////
    // PRIVATE METHODS
    //////////////////////////////////////////////////////////////////////

    private void registerGenerator(@Nonnull BlockPos generatorPosition, @Nonnull IKineticGenerator generator) {
        Direction direction = generator.getDirection();

        if (direction == null) {
            LogManager.getLogger().error("NULL direction");
            return;
        }

        REGISTRY_GENERATOR.add(generatorPosition);
        CACHE_SPINDLES_FOR_GENERATOR.put(generatorPosition, new HashSet<>());

        cacheSpindlesForGenerator(generatorPosition, generator, direction);
        cacheSpindlesForGenerator(generatorPosition, generator, direction.getOpposite());
    }

    private void cacheSpindlesForGenerator(@Nonnull BlockPos generatorPosition, @Nonnull IKineticGenerator generator, @Nonnull Direction direction) {
        for (int i = 0; i < 8; i++) {
            BlockPos spindlePosition = generatorPosition.offset(direction, i + 1);
            IKineticSpindle spindle = getTileEntity(REGISTRY_SPINDLE, spindlePosition);

            if (spindle != null && hasSameDirection(spindle, direction)) {
                cacheSpindle(generatorPosition, spindlePosition, generator, spindle);
            } else {
                break;
            }
        }
    }

    private void cacheSpindle(@Nonnull BlockPos generatorPosition, @Nonnull BlockPos spindlePosition,
                              @Nonnull IKineticGenerator generator, @Nonnull IKineticSpindle spindle) {
        if (CACHE_GENERATOR_FOR_SPINDLE.containsKey(spindlePosition)) {
            return; // spindle was already cached for different generator
        }

        HashSet<BlockPos> spindles = CACHE_SPINDLES_FOR_GENERATOR.get(generatorPosition);
        spindles.add(spindlePosition);
        CACHE_GENERATOR_FOR_SPINDLE.put(spindlePosition, generatorPosition);
        spindle.onConnectedToGenerator(generator);
    }

    private void registerSpindle(@Nonnull BlockPos spindlePosition, @Nonnull IKineticSpindle spindle) {
        Direction direction = spindle.getDirection();

        if (direction == null) {
            LogManager.getLogger().error("NULL direction");
            return;
        }

        REGISTRY_SPINDLE.add(spindlePosition);
        cacheGeneratorsForSpindle(spindlePosition, spindle, direction);
    }

    private void cacheGeneratorsForSpindle(@Nonnull BlockPos spindlePosition, @Nonnull IKineticSpindle spindle, @Nonnull Direction direction) {
        Pair<IKineticGenerator, BlockPos> generator = cacheGeneratorsInDirection(spindlePosition, direction);

        if (generator == null) {
            generator = cacheGeneratorsInDirection(spindlePosition, direction.getOpposite());
        }

        if (generator != null) {
            cacheSpindle(generator.getSecond(), spindlePosition, generator.getFirst(), spindle);
        }
    }

    private Pair<IKineticGenerator, BlockPos> cacheGeneratorsInDirection(@Nonnull BlockPos position, @Nonnull Direction direction) {
        int distToGenerator = 0;

        for (int i = 0; i < 8; i++) {
            BlockPos generatorPosition = position.offset(direction, i + 1);
            IKineticGenerator generator = getTileEntity(REGISTRY_GENERATOR, generatorPosition); //TODO dont get tileentity, but store directions in hashmap

            if (generator != null && hasSameDirection(generator, direction)) {
                // continue to opposite direction, if we just merged two spindles
                if (8 - distToGenerator - 1 > 0) {
                    for (int j = 0; j < 8 - distToGenerator - 1; j++) {
                        BlockPos spindlePosition = position.offset(direction, -1 - j);
                        IKineticSpindle spindle = getTileEntity(REGISTRY_SPINDLE, spindlePosition);

                        if (spindle != null) {
                            cacheSpindle(generatorPosition, spindlePosition, generator, spindle);
                        } else {
                            break;
                        }
                    }
                }

                return new Pair<>(generator, generatorPosition);
            } else {
                IKineticSpindle spindle = getTileEntity(REGISTRY_SPINDLE, generatorPosition);
                if (spindle == null || !hasSameDirection(spindle, direction)) {
                    break;
                }
            }

            distToGenerator++;
        }

        return null;
    }

    private void removeSpindle(@Nonnull BlockPos spindlePosition) {
        uncacheSpindle(spindlePosition);
        Direction.Plane.HORIZONTAL.forEach(direction -> uncacheSpindleInDirection(spindlePosition, direction));
    }

    private void uncacheSpindle(@Nonnull BlockPos spindlePosition) {
        BlockPos generatorPosition = CACHE_GENERATOR_FOR_SPINDLE.remove(spindlePosition);

        if (generatorPosition != null) {
            HashSet<BlockPos> spindles = CACHE_SPINDLES_FOR_GENERATOR.get(generatorPosition);
            spindles.remove(spindlePosition);

            IKineticSpindle spindle = getTileEntity(REGISTRY_SPINDLE, spindlePosition);

            if (spindle != null) { // can be null, because we already deregistered spindle in remove function
                spindle.onDisconnectFromGenerator();
            }
        }
    }

    private void uncacheSpindleInDirection(@Nonnull BlockPos position, @Nonnull Direction direction) {
        int distToGenerator = 0;
        boolean hasGenerator = false;
        tmpList.clear();

        for (int i = 0; i < 16; i++) { // find connected generator
            BlockPos tmpPos = position.offset(direction, i + 1);
            IKineticGenerator generator = getTileEntity(REGISTRY_GENERATOR, tmpPos);
            IKineticSpindle spindle = getTileEntity(REGISTRY_SPINDLE, tmpPos);

            if (generator != null && hasSameDirection(generator, direction)) {
                hasGenerator = true;
                break;
            }

            if (spindle == null || !hasSameDirection(spindle, direction)) {
                break;
            }

            tmpList.add(tmpPos);
            distToGenerator++;
        }

        if (hasGenerator && (distToGenerator - 8 > 0)) {
            for (int i = 0; i < distToGenerator - 8; i++) {
                uncacheSpindle(position.offset(direction, i + 1));
            }
        }

        if (!hasGenerator && !tmpList.isEmpty()) {
            tmpList.forEach(this::uncacheSpindle);
        }
    }

    private void removeGenerator(@Nonnull BlockPos generatorPosition) {
        HashSet<BlockPos> spindles = CACHE_SPINDLES_FOR_GENERATOR.remove(generatorPosition);

        spindles.forEach(spindlePosition -> {
            IKineticSpindle spindle = getTileEntity(REGISTRY_SPINDLE, spindlePosition);
            CACHE_GENERATOR_FOR_SPINDLE.remove(spindlePosition);

            if (spindle == null) {
                LogManager.getLogger().error("removeGenerator: TileEntity at {} in world {} does not exists!", spindlePosition, world);
                return;
            }

            spindle.onDisconnectFromGenerator();
        });
    }

    @Nullable
    private <T extends IKinetic> T getTileEntity(@Nonnull HashSet<BlockPos> map, @Nonnull BlockPos position) {
        T kinetic = null;

        if (map.contains(position)) {
            //noinspection unchecked
            kinetic = (T) world.getTileEntity(position);

            if (kinetic == null) {
                LogManager.getLogger().error("getTileEntity: TileEntity at {} in world {} does not exists!", position, world);
            }
        }

        return kinetic;
    }

    private static void writeMap(@Nonnull CompoundNBT tag, @Nonnull HashSet<BlockPos> map) {
        AtomicInteger index = new AtomicInteger();
        map.forEach(blockPos -> {
            CompoundNBT tmp = new CompoundNBT();
            tmp.putInt("x", blockPos.getX());
            tmp.putInt("y", blockPos.getY());
            tmp.putInt("z", blockPos.getZ());
            tag.put("pos" + index.getAndIncrement(), tmp);
        });
    }

    private static void writeMap(@Nonnull CompoundNBT tag, @Nonnull HashMap<BlockPos, BlockPos> map) {
        AtomicInteger index = new AtomicInteger();
        map.forEach((pos1, pos2) -> {
            CompoundNBT tmp = new CompoundNBT();
            tmp.putInt("x1", pos1.getX());
            tmp.putInt("y1", pos1.getY());
            tmp.putInt("z1", pos1.getZ());
            tmp.putInt("x2", pos2.getX());
            tmp.putInt("y2", pos2.getY());
            tmp.putInt("z2", pos2.getZ());
            tag.put("pos" + index.getAndIncrement(), tmp);
        });
    }

    private static void writeMapMap(@Nonnull CompoundNBT tag, @Nonnull HashMap<BlockPos, HashSet<BlockPos>> map) {
        AtomicInteger index = new AtomicInteger();
        map.forEach((position, spindles) -> {
            CompoundNBT tmp = new CompoundNBT();
            CompoundNBT tmpMap = new CompoundNBT();

            tmp.putInt("x", position.getX());
            tmp.putInt("y", position.getY());
            tmp.putInt("z", position.getZ());

            writeMap(tmpMap, spindles);
            tmp.put("map", tmpMap);

            tag.put("pos" + index.getAndIncrement(), tmp);
        });
    }

    private static void readMap(@Nonnull CompoundNBT tag, @Nonnull HashSet<BlockPos> map) {
        int index = 0;
        CompoundNBT data;

        while ((data = (CompoundNBT) tag.get("pos" + index++)) != null) {
            int x = data.getInt("x");
            int y = data.getInt("y");
            int z = data.getInt("z");
            map.add(new BlockPos(x, y, z));
        }
    }

    private static void readMap(@Nonnull CompoundNBT tag, @Nonnull HashMap<BlockPos, BlockPos> map) {
        int index = 0;
        CompoundNBT data;

        while ((data = (CompoundNBT) tag.get("pos" + index++)) != null) {
            int x1 = data.getInt("x1");
            int y1 = data.getInt("y1");
            int z1 = data.getInt("z1");
            int x2 = data.getInt("x2");
            int y2 = data.getInt("y2");
            int z2 = data.getInt("z2");
            map.put(new BlockPos(x1, y1, z1), new BlockPos(x2, y2, z2));
        }
    }

    private static void readMapMap(@Nonnull CompoundNBT tag, @Nonnull HashMap<BlockPos, HashSet<BlockPos>> map) {
        int index = 0;
        CompoundNBT data;

        while ((data = (CompoundNBT) tag.get("pos" + index++)) != null) {
            int x = data.getInt("x");
            int y = data.getInt("y");
            int z = data.getInt("z");
            HashSet<BlockPos> spindles = new HashSet<>();

            readMap(data.getCompound("map"), spindles);

            map.put(new BlockPos(x, y, z), spindles);
        }
    }

    private static boolean hasSameDirection(@Nonnull IKineticGenerator generator, @Nonnull Direction direction) {
        if (generator.getDirection() == null) {
            return false;
        }
        return generator.getDirection() == direction || generator.getDirection().getOpposite() == direction;
    }

    private static boolean hasSameDirection(@Nonnull IKineticSpindle generator, @Nonnull Direction direction) {
        if (generator.getDirection() == null) {
            return false;
        }
        return generator.getDirection() == direction || generator.getDirection().getOpposite() == direction;
    }
}
