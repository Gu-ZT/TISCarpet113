package carpet.logging.tickwarp;

import carpet.helpers.TickSpeed;
import net.minecraft.entity.player.EntityPlayer;

public class MemorizedTickWarpInfo extends TickWarpInfo
{
	private long totalTicks;
	private long timeRemaining;
	private long startTime;
	private EntityPlayer timeAdvancer;
	private boolean recordedSomething = false;
	private long lastRecordingTime;

	/**
	 * Should be called at the end of {@link TickSpeed#tickrate_advance}
	 * Carpet mod might reset the advancer field before {@link TickSpeed#finish_time_warp} so we record it at the beginning
	 */
	public void recordTickWarpAdvancer()
	{
		this.timeAdvancer = super.getTimeAdvancer();
	}

	/**
	 * Only record if the tick warping has started, so /tick warp 0 won't mess the result
	 * Should be called at the beginning of {@link TickSpeed#finish_time_warp}
	 */
	public void recordResultIfsuitable()
	{
		if (super.getStartTime() != 0)
		{
			this.totalTicks = super.getTotalTicks();
			this.timeRemaining = super.getRemainingTicks();
			this.startTime = super.getStartTime();
			this.recordedSomething = true;
			this.lastRecordingTime = System.nanoTime();
		}
	}

	@Override
	public long getTotalTicks()
	{
		return this.totalTicks;
	}

	@Override
	public long getRemainingTicks()
	{
		return this.timeRemaining;
	}

	@Override
	public long getStartTime()
	{
		return this.startTime;
	}

	@Override
	public EntityPlayer getTimeAdvancer()
	{
		return this.timeAdvancer;
	}

	@Override
	public long getCurrentTime()
	{
		return this.lastRecordingTime;
	}

	public boolean hasData()
	{
		return this.recordedSomething;
	}

	public long getLastRecordingTime()
	{
		return this.lastRecordingTime;
	}
}
