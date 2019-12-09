package com.yanny.ages.api.capability.fluid;

import com.google.common.collect.Sets;
import com.mojang.datafixers.util.Pair;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
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

public class GenericFluidEntityHandler extends WorldSavedData {

    private final Map<BlockPos, LazyOptional<FluidTank>> ENTITIES = new HashMap<>();
    private final Map<LazyOptional<FluidTank>, Set<BlockPos>> CAPABILITIES = new HashMap<>();

    private final World world;
    private final Setup setup;

    public GenericFluidEntityHandler(@Nonnull World world, @Nonnull final Setup setup) {
        super(setup.networkId);
        this.setup = setup;
        this.world = world;
    }

    public synchronized void register(@Nonnull BlockPos position) {
        if (position == BlockPos.ZERO || ENTITIES.containsKey(position)) {
            return;
        }

        registerEntity(position);
        markDirty();
    }

    public synchronized void remove(@Nonnull BlockPos position) {
        if (position == BlockPos.ZERO) {
            return;
        }

        removeEntity(position);
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
        int version = compoundNBT.getInt("version");

        ENTITIES.clear();
        CAPABILITIES.clear();

        if (setup.version != version) {
            return; // dismiss old setup
        }

        capabilityList.forEach(tag -> {
            if (tag instanceof CompoundNBT) {
                CompoundNBT compound = (CompoundNBT) tag;
                int capacity = compound.getInt("capacity");
                FluidTank tank = new FluidTank(capacity);
                LazyOptional<FluidTank> capability = LazyOptional.of(() -> tank);
                Set<BlockPos> set = new HashSet<>();
                ListNBT entitiesList = compound.getList("entities", compound.getInt("listType"));

                tank.readFromNBT(compound);

                entitiesList.forEach(t -> {
                    if (t instanceof CompoundNBT) {
                        CompoundNBT c = (CompoundNBT) t;
                        int x = c.getInt("x");
                        int y = c.getInt("y");
                        int z = c.getInt("z");
                        BlockPos pos = new BlockPos(x, y, z);
                        set.add(pos);
                        ENTITIES.put(pos, capability);
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

            compound.put("entities", set);
            compound.putInt("listType", set.getTagType());
            list.add(compound);
        });

        compoundNBT.putInt("version", setup.version);
        compoundNBT.put("capabilities", list);
        compoundNBT.putInt("listType", list.getTagType());
        return compoundNBT;
    }

    @Nonnull
    public <T> LazyOptional<T> getCapability(@Nonnull BlockPos position) {
        if (ENTITIES.containsKey(position)) {
            markDirty(); // most probably we does some changes in tank, mark for save
            return ENTITIES.get(position).cast();
        } else {
            return LazyOptional.empty();
        }
    }

    private void registerEntity(@Nonnull BlockPos position) {
        List<LazyOptional<FluidTank>> around = findNetworksAround(position);

        if (around.isEmpty()) { // first entity
            LazyOptional<FluidTank> capability = getNewCapability();
            ENTITIES.put(position, capability);
            CAPABILITIES.put(capability, Sets.newHashSet(position));
        } else if (around.size() == 1) { // only one network
            LazyOptional<FluidTank> capability = around.get(0);
            ENTITIES.put(position, capability);
            CAPABILITIES.get(capability).add(position);
            capability.ifPresent(tank -> tank.setCapacity(tank.getCapacity() + setup.entityCapacity));
        } else { // merging networks
            List<LazyOptional<FluidTank>> toRemove = new LinkedList<>();
            LazyOptional<FluidTank> capability = around.remove(0);

            capability.ifPresent(tank -> around.forEach(c -> {
                if (c == capability) { // same network
                    return;
                }

                c.ifPresent(t -> {
                    tank.setCapacity(tank.getCapacity() + t.getCapacity() + setup.entityCapacity);

                    if (!t.isEmpty()) {
                        tank.fill(t.getFluid(), IFluidHandler.FluidAction.EXECUTE);
                    }
                });

                Set<BlockPos> posSet = CAPABILITIES.get(c);
                CAPABILITIES.get(capability).addAll(posSet);
                posSet.forEach(p -> ENTITIES.replace(p, capability));
                toRemove.add(c);
            }));

            ENTITIES.put(position, capability);
            CAPABILITIES.get(capability).add(position);
            toRemove.forEach(CAPABILITIES::remove);
        }
    }

    private void removeEntity(@Nonnull BlockPos position) {
        Set<BlockPos> around = findEntitiesAround(position);

        if (around.size() == 0) { // last entity
            LazyOptional<FluidTank> capability = ENTITIES.remove(position);
            CAPABILITIES.remove(capability);
        } else if (around.size() == 1) { // only one network
            LazyOptional<FluidTank> capability = ENTITIES.remove(position);
            Set<BlockPos> set = CAPABILITIES.get(capability);
            capability.ifPresent(tank -> {
                if (!tank.isEmpty()) {
                    tank.drain(tank.getFluidAmount() / set.size(), IFluidHandler.FluidAction.EXECUTE); // remove part of stored fluid
                }

                tank.setCapacity(tank.getCapacity() - setup.entityCapacity);
            });
            set.remove(position);
        } else {
            LazyOptional<FluidTank> capability = ENTITIES.remove(position);
            Set<BlockPos> set = CAPABILITIES.remove(capability);
            capability.ifPresent(tank -> {
                if (!tank.isEmpty()) {
                    tank.drain(tank.getFluidAmount() / set.size(), IFluidHandler.FluidAction.EXECUTE); // remove part of stored fluid
                }

                tank.setCapacity(tank.getCapacity() - setup.entityCapacity);
            });

            recalculateNetwork(capability, around);
        }
    }

    private void recalculateNetwork(LazyOptional<FluidTank> capability, Set<BlockPos> around) {
        Map<BlockPos, Set<BlockPos>> map = new HashMap<>();
        AtomicInteger oldCapacity = new AtomicInteger();
        AtomicReference<FluidStack> oldFluid = new AtomicReference<>();
        int oldEntitiesCount;

        capability.ifPresent(tank -> {
            oldCapacity.set(tank.getCapacity());
            oldFluid.set(tank.getFluid().copy());
        });

        oldEntitiesCount = oldCapacity.get() / setup.entityCapacity;
        around.forEach(posAround -> {
            Set<BlockPos> mapSet = new HashSet<>();
            map.put(posAround, mapSet);
            searchBranch(posAround, mapSet);
        });

        // get first entry as original network
        Map.Entry<BlockPos, Set<BlockPos>> entry = map.entrySet().iterator().next();
        int entitiesCount = entry.getValue().size();
        map.remove(entry.getKey());
        CAPABILITIES.put(capability, entry.getValue());
        capability.ifPresent(tank -> {
            tank.setCapacity(entitiesCount * setup.entityCapacity);

            if (!oldFluid.get().isEmpty()) {
                tank.getFluid().setAmount(oldFluid.get().getAmount() / oldEntitiesCount * entitiesCount);
            }
        });

        map.forEach((pos, set) -> {
            if (set.equals(entry.getValue())) { // same network
                return;
            }

            LazyOptional<FluidTank> newCapability = getNewCapability();
            CAPABILITIES.put(newCapability, set); // add new network
            set.forEach(p -> ENTITIES.replace(p, newCapability)); // replace capabilities
            int pCount = set.size();
            newCapability.ifPresent(tank -> {
                tank.setCapacity(pCount * setup.entityCapacity);

                if (!oldFluid.get().isEmpty()) {
                    tank.setFluid(oldFluid.get());
                    tank.getFluid().setAmount(oldFluid.get().getAmount() / oldEntitiesCount * pCount);
                }
            });
        });
    }

    private void networkChanged(BlockPos position1, BlockPos position2) {
        LazyOptional<FluidTank> capability1 = ENTITIES.get(position1);
        LazyOptional<FluidTank> capability2 = ENTITIES.get(position2);
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
            set2.forEach(pos -> ENTITIES.replace(pos, capability1));
        } else if (!mapSet1.equals(mapSet2) && capability1 == capability2) {
            LazyOptional<FluidTank> tmpCapability = getNewCapability();
            AtomicReference<FluidStack> oldFluid = new AtomicReference<>();
            AtomicInteger oldCapacity = new AtomicInteger();

            capability1.ifPresent(tank1 -> {
                oldCapacity.set(tank1.getCapacity());
                oldFluid.set(tank1.getFluid());

                tank1.setCapacity(mapSet1.size() * setup.entityCapacity);

                if (!tank1.getFluid().isEmpty()) {
                    tank1.getFluid().setAmount(oldFluid.get().getAmount() / oldCapacity.get() * (mapSet1.size() * setup.entityCapacity));
                }
            });
            tmpCapability.ifPresent(tank2 -> {
                tank2.setCapacity(mapSet2.size() * setup.entityCapacity);

                if (!oldFluid.get().isEmpty()) {
                    tank2.getFluid().setAmount(oldFluid.get().getAmount() / oldCapacity.get() * (mapSet2.size() * setup.entityCapacity));
                }
            });

            CAPABILITIES.get(capability1).clear();
            CAPABILITIES.get(capability1).addAll(mapSet1);
            CAPABILITIES.put(tmpCapability, mapSet2);

            mapSet2.forEach(pos -> ENTITIES.replace(pos, tmpCapability));
        }
    }

    private void searchBranch(BlockPos pos, Set<BlockPos> map) {
        map.add(pos);
        findUniqueEntitiesAround(pos, map).forEach(posAround -> searchBranch(posAround, map));
    }

    @Nonnull
    private LazyOptional<FluidTank> getNewCapability() {
        return LazyOptional.of(() -> new FluidTank(setup.entityCapacity)).cast();
    }

    @Nonnull
    private List<LazyOptional<FluidTank>> findNetworksAround(@Nonnull BlockPos pos) {
        return setup.directions.stream().map(pos::offset).filter(ENTITIES::containsKey).map(ENTITIES::get).distinct().collect(Collectors.toList());
    }

    @Nonnull
    private Set<BlockPos> findEntitiesAround(@Nonnull BlockPos pos) {
        return setup.directions.stream().map(pos::offset).filter(ENTITIES::containsKey).collect(Collectors.toSet());
    }

    private Set<BlockPos> findUniqueEntitiesAround(@Nonnull BlockPos pos, @Nonnull Set<BlockPos> map) {
        return setup.directions.stream()
                .map(dir -> new Pair<>(dir, pos.offset(dir)))
                .filter(pair -> ENTITIES.containsKey(pair.getSecond()))
                .filter(pair -> !map.contains(pair.getSecond()))
                .filter(pair -> setup.checkConnection(world, pos, pair.getFirst(), pair.getSecond()))
                .map(Pair::getSecond).collect(Collectors.toSet());
    }

    public abstract static class Setup {
        private final int entityCapacity;
        private final String networkId;
        private final Set<Direction> directions;
        private final int version;

        public Setup(int capacity, String id, Set<Direction> directions, int version) {
            this.entityCapacity = capacity;
            this.networkId = id;
            this.directions = directions;
            this.version = version;
        }

        public abstract boolean checkConnection(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull Direction posFacing, @Nonnull BlockPos pos2);
    }
}
