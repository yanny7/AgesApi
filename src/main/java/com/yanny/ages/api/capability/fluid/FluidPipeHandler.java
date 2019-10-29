package com.yanny.ages.api.capability.fluid;

import com.google.common.collect.Sets;
import com.mojang.datafixers.util.Pair;
import com.yanny.ages.api.Reference;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static com.yanny.ages.api.property.FacingProperty.*;

public class FluidPipeHandler extends WorldSavedData {
    private static final int PIPE_CAPACITY = 100;
    private static final Set<Direction> DIRECTIONS = Sets.newHashSet(Direction.values());

    private final Map<BlockPos, LazyOptional<FluidTank>> PIPES = new HashMap<>();
    private final Map<LazyOptional<FluidTank>, Set<BlockPos>> CAPABILITIES = new HashMap<>();

    private static final String SAVE_DATA_ID = Reference.MODID + "_fluid_pipe_handler";
    private static final HashMap<World, FluidPipeHandler> INSTANCE = new HashMap<>();

    private final World world;

    private FluidPipeHandler(@Nonnull World world) {
        super(SAVE_DATA_ID);
        this.world = world;
    }

    @Nonnull
    public static FluidPipeHandler getInstance(@Nonnull World world) {
        if (world.isRemote) {
            throw new IllegalStateException("FluidPipeHandler is only server-side");
        }

        FluidPipeHandler handler = INSTANCE.get(world);

        if (handler == null) {
            handler = new FluidPipeHandler(world);
            INSTANCE.put(world, handler);
        }

        if (world instanceof ServerWorld) {
            ServerWorld server = (ServerWorld) world;
            server.getSavedData().getOrCreate(() -> INSTANCE.get(world), SAVE_DATA_ID);
        }

        return handler;
    }

    public synchronized void register(@Nonnull BlockPos position) {
        if (position == BlockPos.ZERO || PIPES.containsKey(position)) {
            return;
        }

        registerPipe(position);
        markDirty();
    }

    public synchronized void remove(@Nonnull BlockPos position) {
        if (position == BlockPos.ZERO) {
            return;
        }

        removePipe(position);
        markDirty();
    }

    public synchronized void connectionChanged(@Nonnull BlockPos position1, @Nonnull BlockPos position2) {
        if (position1 == BlockPos.ZERO || position2 == BlockPos.ZERO) {
            return;
        }

        networkChanged(position1, position2);
        markDirty();
    }

    @Override
    public void read(@Nonnull CompoundNBT compoundNBT) {
        ListNBT capabilityList = compoundNBT.getList("capabilities", compoundNBT.getInt("listType"));

        PIPES.clear();
        CAPABILITIES.clear();

        capabilityList.forEach(tag -> {
            if (tag instanceof CompoundNBT) {
                CompoundNBT compound = (CompoundNBT) tag;
                int capacity = compound.getInt("capacity");
                FluidTank tank = new FluidTank(capacity);
                LazyOptional<FluidTank> capability = LazyOptional.of(() -> tank);
                Set<BlockPos> set = new HashSet<>();
                ListNBT pipeList = compound.getList("pipes", compound.getInt("listType"));

                tank.readFromNBT(compound);

                pipeList.forEach(t -> {
                    if (t instanceof CompoundNBT) {
                        CompoundNBT c = (CompoundNBT) t;
                        int x = c.getInt("x");
                        int y = c.getInt("y");
                        int z = c.getInt("z");
                        BlockPos pos = new BlockPos(x, y, z);
                        set.add(pos);
                        PIPES.put(pos, capability);
                    }
                });

                CAPABILITIES.put(capability, set);
            }
        });
    }

    @Nonnull
    @Override
    public CompoundNBT write(@Nonnull CompoundNBT compoundNBT) {
        ListNBT list = new ListNBT();
        CAPABILITIES.forEach((capability, posSet) -> {
            CompoundNBT compound = new CompoundNBT();
            ListNBT set = new ListNBT();

            capability.ifPresent(tank -> {
                compound.putInt("capacity", tank.getCapacity());
                tank.writeToNBT(compound);
            });

            posSet.forEach(pos -> {
                CompoundNBT c = new CompoundNBT();
                c.putInt("x", pos.getX());
                c.putInt("y", pos.getY());
                c.putInt("z", pos.getZ());
                set.add(c);
            });

            compound.put("pipes", set);
            compound.putInt("listType", set.getTagType());
            list.add(compound);
        });

        compoundNBT.put("capabilities", list);
        compoundNBT.putInt("listType", list.getTagType());
        return compoundNBT;
    }

    @Nonnull
    public <T> LazyOptional<T> getCapability(@Nonnull BlockPos position) {
        if (PIPES.containsKey(position)) {
            markDirty(); // most probably we does some changes in tank, mark for save
            return PIPES.get(position).cast();
        } else {
            return LazyOptional.empty();
        }
    }

    private void registerPipe(@Nonnull BlockPos position) {
        List<LazyOptional<FluidTank>> around = findNetworksAround(position);

        if (around.isEmpty()) { // first pipe
            LazyOptional<FluidTank> capability = getNewCapability();
            PIPES.put(position, capability);
            CAPABILITIES.put(capability, Sets.newHashSet(position));
        } else if (around.size() == 1) { // only one network
            LazyOptional<FluidTank> capability = around.get(0);
            PIPES.put(position, capability);
            CAPABILITIES.get(capability).add(position);
            capability.ifPresent(tank -> tank.setCapacity(tank.getCapacity() + PIPE_CAPACITY));
        } else { // merging networks
            List<LazyOptional<FluidTank>> toRemove = new LinkedList<>();
            LazyOptional<FluidTank> capability = around.remove(0);

            capability.ifPresent(tank -> around.forEach(c -> {
                if (c == capability) { // same network
                    return;
                }

                c.ifPresent(t -> {
                    tank.setCapacity(tank.getCapacity() + t.getCapacity() + PIPE_CAPACITY);

                    if (!t.isEmpty()) {
                        tank.fill(t.getFluid(), IFluidHandler.FluidAction.EXECUTE);
                    }
                });

                Set<BlockPos> posSet = CAPABILITIES.get(c);
                CAPABILITIES.get(capability).addAll(posSet);
                posSet.forEach(p -> PIPES.replace(p, capability));
                toRemove.add(c);
            }));

            PIPES.put(position, capability);
            CAPABILITIES.get(capability).add(position);
            toRemove.forEach(CAPABILITIES::remove);
        }
    }

    private void removePipe(@Nonnull BlockPos position) {
        Set<BlockPos> around = findPipesAround(position);

        if (around.size() == 0) { // last pipe
            LazyOptional<FluidTank> capability = PIPES.remove(position);
            CAPABILITIES.remove(capability);
        } else if (around.size() == 1) { // only one network
            LazyOptional<FluidTank> capability = PIPES.remove(position);
            Set<BlockPos> set = CAPABILITIES.get(capability);
            capability.ifPresent(tank -> {
                if (!tank.isEmpty()) {
                    tank.drain(tank.getFluidAmount() / set.size(), IFluidHandler.FluidAction.EXECUTE); // remove part of stored fluid
                }

                tank.setCapacity(tank.getCapacity() - PIPE_CAPACITY);
            });
            set.remove(position);
        } else {
            LazyOptional<FluidTank> capability = PIPES.remove(position);
            Set<BlockPos> set = CAPABILITIES.remove(capability);
            capability.ifPresent(tank -> {
                if (!tank.isEmpty()) {
                    tank.drain(tank.getFluidAmount() / set.size(), IFluidHandler.FluidAction.EXECUTE); // remove part of stored fluid
                }

                tank.setCapacity(tank.getCapacity() - PIPE_CAPACITY);
            });

            recalculateNetwork(capability, around);
        }
    }

    private void recalculateNetwork(LazyOptional<FluidTank> capability, Set<BlockPos> around) {
        Map<BlockPos, Set<BlockPos>> map = new HashMap<>();
        AtomicInteger oldCapacity = new AtomicInteger();
        AtomicReference<FluidStack> oldFluid = new AtomicReference<>();
        int oldPipesCount;

        capability.ifPresent(tank -> {
            oldCapacity.set(tank.getCapacity());
            oldFluid.set(tank.getFluid().copy());
        });

        oldPipesCount = oldCapacity.get() / PIPE_CAPACITY;
        around.forEach(posAround -> {
            Set<BlockPos> mapSet = new HashSet<>();
            map.put(posAround, mapSet);
            searchBranch(posAround, mapSet);
        });

        // get first entry as original network
        Map.Entry<BlockPos, Set<BlockPos>> entry = map.entrySet().iterator().next();
        int pipesCount = entry.getValue().size();
        map.remove(entry.getKey());
        CAPABILITIES.put(capability, entry.getValue());
        capability.ifPresent(tank -> {
            tank.setCapacity(pipesCount * PIPE_CAPACITY);

            if (!oldFluid.get().isEmpty()) {
                tank.getFluid().setAmount(oldFluid.get().getAmount() / oldPipesCount * pipesCount);
            }
        });

        map.forEach((pos, set) -> {
            if (set.equals(entry.getValue())) { // same network
                return;
            }

            LazyOptional<FluidTank> newCapability = getNewCapability();
            CAPABILITIES.put(newCapability, set); // add new network
            set.forEach(p -> PIPES.replace(p, newCapability)); // replace capabilities
            int pCount = set.size();
            newCapability.ifPresent(tank -> {
                tank.setCapacity(pCount * PIPE_CAPACITY);

                if (!oldFluid.get().isEmpty()) {
                    tank.setFluid(oldFluid.get());
                    tank.getFluid().setAmount(oldFluid.get().getAmount() / oldPipesCount * pCount);
                }
            });
        });
    }

    private void networkChanged(BlockPos position1, BlockPos position2) {
        LazyOptional<FluidTank> capability1 = PIPES.get(position1);
        LazyOptional<FluidTank> capability2 = PIPES.get(position2);
        Set<BlockPos> mapSet1 = new HashSet<>();
        Set<BlockPos> mapSet2 = new HashSet<>();

        if (capability1 == null || capability2 == null) {
            return;
        }

        searchBranch(position1, mapSet1);
        searchBranch(position2, mapSet2);

        if (mapSet1.equals(mapSet2) && capability1 != capability2) { // merge networks
            Set<BlockPos> set2 = CAPABILITIES.remove(capability2);
            capability1.ifPresent(tank1 -> capability2.ifPresent(tank2 -> {
                tank1.setCapacity(tank1.getCapacity() + tank2.getCapacity());

                if (!(tank1.getFluid().isEmpty() && tank2.getFluid().isEmpty())) {
                    if (tank1.getFluid().isEmpty() && !tank2.getFluid().isEmpty()) {
                        tank1.setFluid(tank2.getFluid());
                    } else if (!tank1.getFluid().isEmpty() && !tank2.getFluid().isEmpty()) {
                        tank1.getFluid().setAmount(tank1.getFluidAmount() + tank2.getFluidAmount());
                    }
                }
            }));
            CAPABILITIES.get(capability1).addAll(set2);
            set2.forEach(pos -> PIPES.replace(pos, capability1));
        } else if (!mapSet1.equals(mapSet2) && capability1 == capability2) {
            LazyOptional<FluidTank> tmpCapability = getNewCapability();
            AtomicReference<FluidStack> oldFluid = new AtomicReference<>();
            AtomicInteger oldCapacity = new AtomicInteger();

            capability1.ifPresent(tank1 -> {
                oldCapacity.set(tank1.getCapacity());
                oldFluid.set(tank1.getFluid());

                tank1.setCapacity(mapSet1.size() * PIPE_CAPACITY);

                if (!tank1.getFluid().isEmpty()) {
                    tank1.getFluid().setAmount(oldFluid.get().getAmount() / oldCapacity.get() * (mapSet1.size() * PIPE_CAPACITY));
                }
            });
            tmpCapability.ifPresent(tank2 -> {
                tank2.setCapacity(mapSet2.size() * PIPE_CAPACITY);

                if (!oldFluid.get().isEmpty()) {
                    tank2.getFluid().setAmount(oldFluid.get().getAmount() / oldCapacity.get() * (mapSet2.size() * PIPE_CAPACITY));
                }
            });

            CAPABILITIES.get(capability1).clear();
            CAPABILITIES.get(capability1).addAll(mapSet1);
            CAPABILITIES.put(tmpCapability, mapSet2);

            mapSet2.forEach(pos -> PIPES.replace(pos, tmpCapability));
        }
    }

    private void searchBranch(BlockPos pos, Set<BlockPos> map) {
        map.add(pos);
        findUniquePipesAround(pos, map).forEach(posAround -> searchBranch(posAround, map));
    }

    @Nonnull
    private LazyOptional<FluidTank> getNewCapability() {
        return LazyOptional.of(() -> new FluidTank(PIPE_CAPACITY)).cast();
    }

    @Nonnull
    private List<LazyOptional<FluidTank>> findNetworksAround(@Nonnull BlockPos pos) {
        return DIRECTIONS.stream().map(pos::offset).filter(PIPES::containsKey).map(PIPES::get).distinct().collect(Collectors.toList());
    }

    @Nonnull
    private Set<BlockPos> findPipesAround(@Nonnull BlockPos pos) {
        return DIRECTIONS.stream().map(pos::offset).filter(PIPES::containsKey).collect(Collectors.toSet());
    }

    private Set<BlockPos> findUniquePipesAround(@Nonnull BlockPos pos, @Nonnull Set<BlockPos> map) {
        return DIRECTIONS.stream().map(dir -> new Pair<>(dir, pos.offset(dir))).filter(pair -> PIPES.containsKey(pair.getSecond()))
                .filter(pair -> !map.contains(pair.getSecond())).filter(pair -> {
                    BlockState blockState = world.getBlockState(pos);
                    int state = blockState.get(FACING_TO_PROPERTY_MAP.get(pair.getFirst()));
                    int facing = world.getBlockState(pair.getSecond()).get(FACING_TO_PROPERTY_MAP.get(pair.getFirst().getOpposite()));

                    return ((state == TRUE) || (state == FORCE_TRUE)) && ((facing == TRUE) || (facing == FORCE_TRUE));
                }).map(Pair::getSecond).collect(Collectors.toSet());
    }
}
