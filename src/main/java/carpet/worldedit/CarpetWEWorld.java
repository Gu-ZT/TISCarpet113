/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package carpet.worldedit;

import carpet.worldedit.internal.CarpetWEWorldNativeAccess;
import carpet.worldedit.internal.NBTConverter;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Sets;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseItem;
import com.sk89q.worldedit.blocks.BaseItemStack;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.internal.Constants;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.util.SideEffect;
import com.sk89q.worldedit.util.SideEffectSet;
import com.sk89q.worldedit.util.TreeGenerator.TreeType;
import com.sk89q.worldedit.world.AbstractWorld;
import com.sk89q.worldedit.world.RegenOptions;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.item.ItemTypes;
import com.sk89q.worldedit.world.weather.WeatherType;
import com.sk89q.worldedit.world.weather.WeatherTypes;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.chunk.storage.AnvilSaveHandler;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraft.world.gen.feature.*;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.WorldInfo;

import javax.annotation.Nullable;
import java.io.File;
import java.lang.ref.WeakReference;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An adapter to Minecraft worlds for WorldEdit.
 */
public class CarpetWEWorld extends AbstractWorld {

    private static final Random random = new Random();
    private final WeakReference<World> worldRef;
    private final CarpetWEWorldNativeAccess worldNativeAccess;

    private static final IBlockState JUNGLE_LOG = Blocks.JUNGLE_LOG.getDefaultState();
    private static final IBlockState JUNGLE_LEAF = Blocks.JUNGLE_LEAVES.getDefaultState().with(BlockLeaves.PERSISTENT, Boolean.TRUE);
    private static final IBlockState JUNGLE_SHRUB = Blocks.OAK_LEAVES.getDefaultState().with(BlockLeaves.PERSISTENT, Boolean.TRUE);

    /**
     * Construct a new world.
     *
     * @param world the world
     */
    CarpetWEWorld(World world) {
        checkNotNull(world);
        this.worldRef = new WeakReference<>(world);
        this.worldNativeAccess = new CarpetWEWorldNativeAccess(worldRef);
    }

    /**
     * Get the underlying handle to the world.
     *
     * @return the world
     * @throws RuntimeException thrown if a reference to the world was lost (i.e. world was unloaded)
     */
    public World getWorld() {
        World world = worldRef.get();
        if (world != null) {
            return world;
        } else {
            throw new RuntimeException("The reference to the world was lost (i.e. the world may have been unloaded)");
        }
    }

    @Override
    public String getName() {
        return this.getWorld().getWorldInfo().getWorldName();
    }

    @Override
    public String getId() {
        return getName() + "_" + getWorld().getDimension().getType().getSuffix();
    }

    @Override
    public Path getStoragePath() {
        final World world = getWorld();
        MinecraftServer server = world.getServer();
        ISaveHandler isavehandler = server.getActiveAnvilConverter().getSaveLoader(server.getFolderName(), server);
        return isavehandler.getWorldDirectory().toPath();
    }

    @Override
    public <B extends BlockStateHolder<B>> boolean setBlock(BlockVector3 position, B block, SideEffectSet sideEffects) throws WorldEditException {
        clearContainerBlockContents(position);
        return worldNativeAccess.setBlock(position, block, sideEffects);
    }

    @Override
    public Set<SideEffect> applySideEffects(BlockVector3 position, BlockState previousType, SideEffectSet sideEffectSet) {
        worldNativeAccess.applySideEffects(position, previousType, sideEffectSet);
        return Sets.intersection(CarpetWorldEdit.inst.getPlatform().getSupportedSideEffects(), sideEffectSet.getSideEffectsToApply());
    }

    @Override
    public int getBlockLightLevel(BlockVector3 position) {
        checkNotNull(position);
        return getWorld().getLight(CarpetWEAdapter.toBlockPos(position));
    }

    @Override
    public boolean clearContainerBlockContents(BlockVector3 position) {
        checkNotNull(position);
        if (!getBlock(position).getBlockType().getMaterial().hasContainer()) {
            return false;
        }

        TileEntity tile = getWorld().getTileEntity(CarpetWEAdapter.toBlockPos(position));
        if ((tile instanceof IInventory)) {
            ((IInventory) tile).clear();
            return true;
        }
        return false;
    }

    @Override
    public boolean fullySupports3DBiomes() {
        return false;
    }

    @Override
    public BiomeType getBiome(BlockVector3 position) {
        checkNotNull(position);
        Chunk chunk = getWorld().getChunk(position.getX() >> 4, position.getZ() >> 4);
        return getBiomeInChunk(position, chunk);
    }

    @Override
    public boolean setBiome(BlockVector3 position, BiomeType biome) {
        checkNotNull(position);
        checkNotNull(biome);

        Chunk chunk = getWorld().getChunk(position.getBlockX() >> 4, position.getBlockZ() >> 4);
        if (chunk.isLoaded()) {
            int i = position.getX() & 15;
            int j = position.getZ() & 15;
            chunk.getBiomes()[j << 4 | i] = CarpetWEAdapter.adapt(biome);
            chunk.markDirty();
            return true;
        }

        return false;
    }

    private static final LoadingCache<WorldServer, WorldEditFakePlayer> fakePlayers
            = CacheBuilder.newBuilder().weakKeys().softValues().build(CacheLoader.from(WorldEditFakePlayer::new));

    @Override
    public boolean useItem(BlockVector3 position, BaseItem item, Direction face) {
        ItemStack stack = CarpetWEAdapter.adapt(new BaseItemStack(item.getType(), item.getNbtData(), 1));
        WorldServer world = (WorldServer) getWorld();
        final WorldEditFakePlayer fakePlayer;
        try {
            fakePlayer = fakePlayers.get(world);
        } catch (ExecutionException ignored) {
            return false;
        }
        fakePlayer.setHeldItem(EnumHand.MAIN_HAND, stack);
        fakePlayer.setLocationAndAngles(position.getBlockX(), position.getBlockY(), position.getBlockZ(),
                (float) face.toVector().toYaw(), (float) face.toVector().toPitch());
        final BlockPos blockPos = CarpetWEAdapter.toBlockPos(position);
        final EnumFacing enumFacing = CarpetWEAdapter.adapt(face);
        ItemUseContext itemUseContext = new ItemUseContext(fakePlayer, stack, blockPos, enumFacing, blockPos.getX(), blockPos.getY(), blockPos.getZ());
        EnumActionResult used = stack.onItemUse(itemUseContext);
        if (used != EnumActionResult.SUCCESS) {
            // try activating the block
            if (getWorld().getBlockState(blockPos).onBlockActivated(world, blockPos, fakePlayer, EnumHand.MAIN_HAND,
                    enumFacing, blockPos.getX(), blockPos.getY(), blockPos.getZ())) {
                used = EnumActionResult.SUCCESS;
            } else {
                used = stack.getItem().onItemRightClick(world, fakePlayer, EnumHand.MAIN_HAND).getType();
            }
        }
        return used == EnumActionResult.SUCCESS;
    }

    @Override
    public void dropItem(Vector3 position, BaseItemStack item) {
        checkNotNull(position);
        checkNotNull(item);

        if (item.getType() == ItemTypes.AIR) {
            return;
        }

        EntityItem entity = new EntityItem(getWorld(), position.getX(), position.getY(), position.getZ(), CarpetWEAdapter.adapt(item));
        entity.setPickupDelay(10);
        getWorld().spawnEntity(entity);
    }

    @Override
    public void simulateBlockMine(BlockVector3 position) {
        BlockPos pos = CarpetWEAdapter.toBlockPos(position);
        getWorld().destroyBlock(pos, true);
    }

    @Override
    public boolean canPlaceAt(BlockVector3 position, BlockState blockState) {
        return CarpetWEAdapter.adapt(blockState).isValidPosition(getWorld(), CarpetWEAdapter.toBlockPos(position));
    }

    private BiomeType getBiomeInChunk(BlockVector3 pos, Chunk chunk) {
        return CarpetWEAdapter.adapt(chunk.getBiome(CarpetWEAdapter.toBlockPos(pos)));
    }

    @Override
    public boolean regenerate(Region region, Extent extent, RegenOptions options) {
        // Don't even try to regen if it's going to fail.
        IChunkProvider provider = getWorld().getChunkProvider();
        if (!(provider instanceof ChunkProviderServer)) {
            return false;
        }

        File saveFolder = com.google.common.io.Files.createTempDir();
        // register this just in case something goes wrong
        // normally it should be deleted at the end of this method
        saveFolder.deleteOnExit();
        try {
            WorldServer originalWorld = (WorldServer) getWorld();

            MinecraftServer server = originalWorld.getServer();
            AnvilSaveHandler saveHandler = new AnvilSaveHandler(saveFolder, originalWorld.getSaveHandler().getWorldDirectory().getName(), server, server.getDataFixer());
            World freshWorld = new WorldServer(server, saveHandler, originalWorld.getSavedDataStorage(), originalWorld.getWorldInfo(), originalWorld.dimension.getType(), originalWorld.profiler).init(false);

            // Pre-gen all the chunks
            // We need to also pull one more chunk in every direction
            CuboidRegion expandedPreGen = new CuboidRegion(region.getMinimumPoint().subtract(16, 0, 16), region.getMaximumPoint().add(16, 0, 16));
            for (BlockVector2 chunk : expandedPreGen.getChunks()) {
                freshWorld.getChunk(chunk.getBlockX(), chunk.getBlockZ());
            }

            for (BlockVector3 vec : region) {
                BlockPos pos = CarpetWEAdapter.toBlockPos(vec);
                Chunk chunk = freshWorld.getChunk(pos);
                BlockStateHolder<?> state = CarpetWEAdapter.adapt(chunk.getBlockState(pos));
                TileEntity blockEntity = chunk.getTileEntity(pos);
                if (blockEntity != null) {
                    net.minecraft.nbt.NBTTagCompound tag = new net.minecraft.nbt.NBTTagCompound();
                    blockEntity.write(tag);
                    state = state.toBaseBlock(NBTConverter.fromNative(tag));
                }
                extent.setBlock(vec, state.toBaseBlock());

                if (options.shouldRegenBiomes()) {
                    BiomeType biome = getBiomeInChunk(vec, chunk);
                    extent.setBiome(vec, biome);
                }
            }
        } catch (WorldEditException e) {
            throw new RuntimeException(e);
        } finally {
            saveFolder.delete();
        }

        return true;
    }

    @Nullable
    private static Feature<NoFeatureConfig> createTreeFeatureGenerator(TreeType type) {
        switch (type) {
            case TREE: return new TreeFeature(true);
            case BIG_TREE: return new BigTreeFeature(true);
            case PINE:
            case REDWOOD: return new PointyTaigaTreeFeature();
            case TALL_REDWOOD: return new TallTaigaTreeFeature(true);
            case BIRCH: return new BirchTreeFeature(true, false);
            case JUNGLE: return new MegaJungleFeature(true, 10, 20, JUNGLE_LOG, JUNGLE_LEAF);
            case SMALL_JUNGLE: return new JungleTreeFeature(true, 4 + random.nextInt(7), JUNGLE_LOG, JUNGLE_LEAF, false);
            case SHORT_JUNGLE: return new JungleTreeFeature(true, 4 + random.nextInt(7), JUNGLE_LOG, JUNGLE_LEAF, true);
            case JUNGLE_BUSH: return new ShrubFeature(JUNGLE_LOG, JUNGLE_SHRUB);
            case RED_MUSHROOM: return new BigBrownMushroomFeature();
            case BROWN_MUSHROOM: return new BigRedMushroomFeature();
            case SWAMP: return new SwampTreeFeature();
            case ACACIA: return new SavannaTreeFeature(true);
            case DARK_OAK: return new CanopyTreeFeature(true);
            case MEGA_REDWOOD: return new MegaPineTree(false, random.nextBoolean());
            case TALL_BIRCH: return new BirchTreeFeature(true, true);
            case RANDOM: return createTreeFeatureGenerator(TreeType.values()[ThreadLocalRandom.current().nextInt(TreeType.values().length)]);
            case RANDOM_REDWOOD:
            default:
                return null;
        }
    }

    @Override
    public boolean generateTree(TreeType type, EditSession editSession, BlockVector3 position) {
        Feature<NoFeatureConfig> generator = createTreeFeatureGenerator(type);
        return generator != null && generator.place(getWorld(), getWorld().getChunkProvider().getChunkGenerator(), random, CarpetWEAdapter.toBlockPos(position), new NoFeatureConfig());
    }

    @Override
    public void checkLoadedChunk(BlockVector3 pt) {
        getWorld().getChunk(CarpetWEAdapter.toBlockPos(pt));
    }

    @Override
    public void fixAfterFastMode(Iterable<BlockVector2> chunks) {
        fixLighting(chunks);
    }

    @Override
    public void fixLighting(Iterable<BlockVector2> chunks) {
        World world = getWorld();
        for (BlockVector2 chunk : chunks) {
            world.getChunk(chunk.getBlockX(), chunk.getBlockZ()).resetRelightChecks$worldEdit();
        }
    }

    @Override
    public boolean playEffect(Vector3 position, int type, int data) {
        getWorld().playEvent(type, CarpetWEAdapter.toBlockPos(position.toBlockPoint()), data);
        return true;
    }

    @Override
    public WeatherType getWeather() {
        WorldInfo info = getWorld().getWorldInfo();
        if (info.isThundering()) {
            return WeatherTypes.THUNDER_STORM;
        }
        if (info.isRaining()) {
            return WeatherTypes.RAIN;
        }
        return WeatherTypes.CLEAR;
    }

    @Override
    public long getRemainingWeatherDuration() {
        WorldInfo info = getWorld().getWorldInfo();
        if (info.isThundering()) {
            return info.getThunderTime();
        }
        if (info.isRaining()) {
            return info.getRainTime();
        }
        return info.getClearWeatherTime();
    }

    @Override
    public void setWeather(WeatherType weatherType) {
        setWeather(weatherType, 0);
    }

    @Override
    public void setWeather(WeatherType weatherType, long duration) {
        WorldInfo info = getWorld().getWorldInfo();
        if (weatherType == WeatherTypes.THUNDER_STORM) {
            info.setClearWeatherTime(0);
            info.setThundering(true);
            info.setThunderTime((int) duration);
        } else if (weatherType == WeatherTypes.RAIN) {
            info.setClearWeatherTime(0);
            info.setRaining(true);
            info.setRainTime((int) duration);
        } else if (weatherType == WeatherTypes.CLEAR) {
            info.setRaining(false);
            info.setThundering(false);
            info.setClearWeatherTime((int) duration);
        }
    }

    @Override
    public int getMaxY() {
        return getWorld().getHeight() - 1;
    }

    @Override
    public BlockVector3 getSpawnPosition() {
        return CarpetWEAdapter.adapt(getWorld().getSpawnPoint());
    }

    @Override
    public BlockState getBlock(BlockVector3 position) {
        net.minecraft.block.state.IBlockState mcState = getWorld()
                .getChunk(position.getBlockX() >> 4, position.getBlockZ() >> 4)
                .getBlockState(CarpetWEAdapter.toBlockPos(position));

        return CarpetWEAdapter.adapt(mcState);
    }

    @Override
    public BaseBlock getFullBlock(BlockVector3 position) {
        BlockPos pos = new BlockPos(position.getBlockX(), position.getBlockY(), position.getBlockZ());
        // Avoid creation by using the CHECK mode -- if it's needed, it'll be re-created anyways
        TileEntity tile = getWorld().getChunk(pos).getTileEntity(pos, Chunk.EnumCreateEntityType.CHECK);

        if (tile != null) {
            NBTTagCompound nbt = new NBTTagCompound();
            tile.write(nbt);
            return getBlock(position).toBaseBlock(NBTConverter.fromNative(nbt));
        } else {
            return getBlock(position).toBaseBlock();
        }
    }

    @Override
    public int hashCode() {
        return getWorld().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if ((o instanceof CarpetWEWorld)) {
            CarpetWEWorld other = ((CarpetWEWorld) o);
            World otherWorld = other.worldRef.get();
            World thisWorld = worldRef.get();
            return otherWorld != null && otherWorld.equals(thisWorld);
        } else if (o instanceof com.sk89q.worldedit.world.World) {
            return ((com.sk89q.worldedit.world.World) o).getName().equals(getName());
        } else {
            return false;
        }
    }

    @Override
    public List<? extends Entity> getEntities(Region region) {
        List<Entity> entities = new ArrayList<>();
        for (net.minecraft.entity.Entity entity : getWorld().loadedEntityList) {
            if (region.contains(BlockVector3.at(entity.posX, entity.posY, entity.posZ))) {
                entities.add(new CarpetWEEntity(entity));
            }
        }
        return entities;
    }

    @Override
    public List<? extends Entity> getEntities() {
        List<Entity> entities = new ArrayList<>();
        for (net.minecraft.entity.Entity entity : getWorld().loadedEntityList) {
            entities.add(new CarpetWEEntity(entity));
        }
        return entities;
    }

    @Nullable
    @Override
    public Entity createEntity(Location location, BaseEntity entity) {
        World world = getWorld();
        net.minecraft.entity.Entity createdEntity = EntityType.create(world, new ResourceLocation(entity.getType().getId()));
        if (createdEntity != null) {
            CompoundTag nativeTag = entity.getNbtData();
            if (nativeTag != null) {
                NBTTagCompound tag = NBTConverter.toNative(entity.getNbtData());
                for (String name : Constants.NO_COPY_ENTITY_NBT_FIELDS) {
                    tag.remove(name);
                }
                createdEntity.read(tag);
            }

            createdEntity.setLocationAndAngles(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());

            world.spawnEntity(createdEntity);
            return new CarpetWEEntity(createdEntity);
        } else {
            return null;
        }
    }

}
