package carpet.commands.lifetime.spawning;

import carpet.utils.Messenger;
import net.minecraft.util.text.ITextComponent;

public class LiteralSpawningReason extends SpawningReason
{
	public static final LiteralSpawningReason NATURAL = new LiteralSpawningReason("natural", "Natural spawning");
	public static final LiteralSpawningReason PORTAL_PIGMAN = new LiteralSpawningReason("portal_pigman", "Nether portal pigman spawning");
	public static final LiteralSpawningReason COMMAND = new LiteralSpawningReason("command", "Summon command");
	public static final LiteralSpawningReason ITEM = new LiteralSpawningReason("item", "Spawned by item");
	public static final LiteralSpawningReason BLOCK_DROP = new LiteralSpawningReason("block_drop", "Block drop");
	public static final LiteralSpawningReason SLIME = new LiteralSpawningReason("slime", "Slime division");
	public static final LiteralSpawningReason ZOMBIE_REINFORCE = new LiteralSpawningReason("zombie_reinforce", "Zombie Reinforce");
	public static final LiteralSpawningReason SPAWNER = new LiteralSpawningReason("spawner", "Spawned by spawner");
	public static final LiteralSpawningReason SUMMON = new LiteralSpawningReason("summon", "Be summoned by entity or block");
	public static final LiteralSpawningReason BREEDING = new LiteralSpawningReason("breeding", "Breeding");
	public static final LiteralSpawningReason DISPENSED = new LiteralSpawningReason("dispensed", "Dispensed by block");

	private final String translationKey;
	private final String name;

	private LiteralSpawningReason(String translationKey, String name)
	{
		this.translationKey = translationKey;
		this.name = name;
	}

	@Override
	public ITextComponent toText()
	{
		return Messenger.s(this.tr(this.translationKey, this.name));
	}
}
