package flaxbeard.cyberware.common;

import javax.annotation.Nonnull;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;

import net.minecraftforge.common.ISpecialArmor;

public enum ArmorClass {
	
	NONE(),
	LIGHT(),
	HEAVY;
	
	public static boolean isWearingLightOrNone(EntityLivingBase entityLivingBase)
	{
		return get(entityLivingBase) != HEAVY;
	}
	
	public static ArmorClass get(@Nonnull EntityLivingBase entityLivingBase)
	{
		boolean hasNoArmor = true;
		
		for (ItemStack stack : entityLivingBase.getArmorInventoryList())
		{
			if (stack.isEmpty()) continue;
			hasNoArmor = false;
			
			if (stack.getItem() instanceof ItemArmor)
			{
				if (((ItemArmor) stack.getItem()).getArmorMaterial().getDamageReductionAmount(EntityEquipmentSlot.CHEST) > 4)
				{
					return HEAVY;
				}
			}
			
			if (stack.getItem() instanceof ISpecialArmor)
			{
				if (((ISpecialArmor) stack.getItem()).getProperties(Minecraft.getMinecraft().player, stack, DamageSource.CACTUS, 1, 1).AbsorbRatio * 25D > 4)
				{
					return HEAVY;
				}
			}
		}
		return hasNoArmor ? NONE : LIGHT;
	}
}
