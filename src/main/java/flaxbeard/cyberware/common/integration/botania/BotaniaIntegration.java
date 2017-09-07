package flaxbeard.cyberware.common.integration.botania;

import flaxbeard.cyberware.api.item.ICyberware;
import net.minecraft.item.Item;

public class BotaniaIntegration
{
	public static Item manaLens;

	public static void preInit()
	{
		manaLens = new ItemManaLens("manaseerLens", ICyberware.EnumSlot.EYES, new String[] { "lens", "link" });
	}
}
