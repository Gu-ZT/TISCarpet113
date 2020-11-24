package carpet.microtiming.utils;

import carpet.logging.Logger;
import carpet.microtiming.MicroTimingLogger;
import carpet.microtiming.MicroTimingLoggerManager;
import carpet.utils.Messenger;
import net.minecraft.entity.player.EntityPlayer;

import java.util.Arrays;

public class MicroTimingStandardCarpetLogger extends Logger
{
	public static final String NAME = MicroTimingLogger.NAME;
	private static final Translator translator = new Translator("logger", NAME + ".carpet_logger");

	public MicroTimingStandardCarpetLogger(String logName, String def, String[] options)
	{
		super(logName, def, options);
	}

	public static MicroTimingStandardCarpetLogger create()
	{
		String def = MicroTimingLogger.LoggingOption.DEFAULT.toString();
		String[] options = Arrays.stream(MicroTimingLogger.LoggingOption.values()).map(MicroTimingLogger.LoggingOption::toString).map(String::toLowerCase).toArray(String[]::new);
		return new MicroTimingStandardCarpetLogger(NAME, def, options);
	}

	@Override
	public void addPlayer(String playerName, String option)
	{
		super.addPlayer(playerName, option);
		EntityPlayer player = this.playerFromName(playerName);
		if (player != null && !MicroTimingLoggerManager.isLoggerActivated())
		{
			String command = "/carpet microTiming true";
			Messenger.m(player, Messenger.c(
					"w " + String.format(translator.tr("rule_hint", "Use command %s to start logging"), command),
					"?" + command,
					"^w " + translator.tr("Click to execute")
			));
		}
	}
}
