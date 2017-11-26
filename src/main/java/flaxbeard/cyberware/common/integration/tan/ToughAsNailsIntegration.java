package flaxbeard.cyberware.common.integration.tan;

import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import flaxbeard.cyberware.api.item.ICyberware.EnumSlot;
import flaxbeard.cyberware.common.CyberwareContent;
import flaxbeard.cyberware.common.item.ItemCyberware;

public class ToughAsNailsIntegration
{
	public static ItemCyberware sweat;

	public static void preInit()
	{
		sweat = new ItemToughAsNailsUpgrade("tough_as_nails_upgrades",
				new EnumSlot[] { EnumSlot.SKIN, EnumSlot.SKIN },
				new String[] { "sweat", "blubber" });
		sweat.setEssenceCost(7, 14);
		sweat.setWeights(CyberwareContent.UNCOMMON, CyberwareContent.UNCOMMON);
		NonNullList<ItemStack> l1 = NonNullList.create();
		NonNullList<ItemStack> l2 = NonNullList.create();
		l1.add(new ItemStack(CyberwareContent.component, 1, 8));
		l1.add(new ItemStack(CyberwareContent.component, 2, 7));
		l1.add(new ItemStack(CyberwareContent.component, 1, 1));
		l2.add(new ItemStack(CyberwareContent.component, 2, 6));
		l2.add(new ItemStack(CyberwareContent.component, 1, 7));
		l2.add(new ItemStack(CyberwareContent.component, 3, 1));
		sweat.setComponents(
				l1, l2
				);
	}
}
