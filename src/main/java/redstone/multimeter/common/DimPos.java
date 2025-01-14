package redstone.multimeter.common;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;

import redstone.multimeter.util.NbtUtils;

public class DimPos {
	
	private final ResourceLocation dimensionId;
	private final BlockPos blockPos;
	
	public DimPos(ResourceLocation dimensionId, BlockPos blockPos) {
		this.dimensionId = dimensionId;
		this.blockPos = blockPos.toImmutable();
	}
	
	public DimPos(ResourceLocation dimensionId, int x, int y, int z) {
		this(dimensionId, new BlockPos(x, y, z));
	}
	
	public DimPos(World world, BlockPos pos) {
		this(DimensionType.getKey(world.dimension.getType()), pos);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof DimPos) {
			DimPos pos = (DimPos)obj;
			return pos.dimensionId.equals(dimensionId) && pos.blockPos.equals(blockPos);
		}
		
		return false;
	}
	
	@Override
	public int hashCode() {
		return blockPos.hashCode() + 31 * dimensionId.hashCode();
	}
	
	@Override
	public String toString() {
		return String.format("%s[%d, %d, %d]", dimensionId.toString(), blockPos.getX(), blockPos.getY(), blockPos.getZ());
	}
	
	public ResourceLocation getDimensionId() {
		return dimensionId;
	}
	
	public boolean isOf(World world) {
		return DimensionType.getKey(world.dimension.getType()).equals(dimensionId);
	}
	
	public DimPos offset(ResourceLocation dimensionId) {
		return new DimPos(dimensionId, blockPos);
	}
	
	public BlockPos getBlockPos() {
		return blockPos;
	}
	
	public DimPos offset(EnumFacing dir) {
		return offset(dir, 1);
	}
	
	public DimPos offset(EnumFacing dir, int distance) {
		return new DimPos(dimensionId, blockPos.offset(dir, distance));
	}
	
	public DimPos offset(Axis axis) {
		return offset(axis, 1);
	}
	
	public DimPos offset(Axis axis, int distance) {
		int dx = axis.getCoordinate(distance, 0, 0);
		int dy = axis.getCoordinate(0, distance, 0);
		int dz = axis.getCoordinate(0, 0, distance);
		
		return offset(dx, dy, dz);
	}
	
	public DimPos offset(int dx, int dy, int dz) {
		return new DimPos(dimensionId, blockPos.add(dx, dy, dz));
	}
	
	public NBTTagCompound toNbt() {
		NBTTagCompound nbt = new NBTTagCompound();
		
		// The key is "world id" to match RSMM for 1.16+
		// Keeping this key consistent between versions
		// allows clients and servers of different versions
		// to communicate effectively through the use of
		// mods like ViaVersion or multiconnect
		nbt.put("world id", NbtUtils.identifierToNbt(dimensionId));
		nbt.putInt("x", blockPos.getX());
		nbt.putInt("y", blockPos.getY());
		nbt.putInt("z", blockPos.getZ());
		
		return nbt;
	}
	
	public static DimPos fromNbt(NBTTagCompound nbt) {
		ResourceLocation dimensionId = NbtUtils.nbtToIdentifier(nbt.getCompound("world id"));
		int x = nbt.getInt("x");
		int y = nbt.getInt("y");
		int z = nbt.getInt("z");
		
		return new DimPos(dimensionId, x, y, z);
	}
}
