package redstone.multimeter.block;

import carpet.settings.CarpetSettings;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

public interface MeterableBlock extends Meterable {
	
	default void logPowered(World world, BlockPos pos, boolean powered) {
		if (CarpetSettings.modRedstoneMultimeter && !world.isRemote()) {
			((WorldServer)world).getMultimeter().logPowered(world, pos, powered);
		}
	}
	
	default void logPowered(World world, BlockPos pos, IBlockState state) {
	    if (CarpetSettings.modRedstoneMultimeter && !world.isRemote()) {
	        ((WorldServer)world).getMultimeter().logPowered(world, pos, state);
	    }
	}
}
