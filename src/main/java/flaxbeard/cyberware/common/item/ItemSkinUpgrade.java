package flaxbeard.cyberware.common.item;

import java.util.*;

import net.minecraft.enchantment.EnchantmentThorns;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.EntityDamageSourceIndirect;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.ISpecialArmor;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.api.CyberwareUpdateEvent;
import flaxbeard.cyberware.common.CyberwareContent;
import flaxbeard.cyberware.common.handler.EssentialsMissingHandler;
import flaxbeard.cyberware.common.lib.LibConstants;

public class ItemSkinUpgrade extends ItemCyberware
{

	public ItemSkinUpgrade(String name, EnumSlot slot, String[] subnames)
	{
		super(name, slot, subnames);
		MinecraftForge.EVENT_BUS.register(this);

	}
	
	@SubscribeEvent
	public void handleLivingUpdate(CyberwareUpdateEvent event)
	{
		EntityLivingBase e = event.getEntityLiving();

        if (e == null)
            return;

		ItemStack test = new ItemStack(this, 1, 0);
		if (e.ticksExisted % 20 == 0 && CyberwareAPI.isCyberwareInstalled(e, test))
		{
			if (e.world.canBlockSeeSky(new BlockPos((int) e.posX,(int) (e.posY + e.height),(int) e.posZ)))
			{
				CyberwareAPI.getCapability(e).addPower(getPowerProduction(test), test);
			}
		}
	}
	
	private Set<UUID> lastImmuno = new HashSet<>();
	private static Map<UUID, Collection<PotionEffect>> potions = new HashMap<UUID, Collection<PotionEffect>>();

	@SubscribeEvent(priority = EventPriority.HIGH)
	public void handleMissingEssentials(CyberwareUpdateEvent event)
	{
		EntityLivingBase e = event.getEntityLiving();

		if (e == null)
			return;

		ItemStack test = new ItemStack(this, 1, 3);
		if (CyberwareAPI.isCyberwareInstalled(e, test))
		{
			boolean powerUsed = e.ticksExisted % 20 == 0 ? CyberwareAPI.getCapability(e).usePower(test, getPowerConsumption(test)) : lastImmuno.contains(e.getUniqueID());
			
			if (!powerUsed && e instanceof EntityPlayer && e.ticksExisted % 100 == 0 && !e.isPotionActive(CyberwareContent.neuropozyneEffect))
			{
				e.attackEntityFrom(EssentialsMissingHandler.lowessence, 2F);
			}
			
			if (potions.containsKey(e.getUniqueID()))
			{
				Collection<PotionEffect> potionsLastActive = potions.get(e.getUniqueID());
				Collection<PotionEffect> currentEffects = e.getActivePotionEffects();
				for (PotionEffect cE : currentEffects)
				{
					if (cE.getPotion() == MobEffects.POISON || cE.getPotion() == MobEffects.HUNGER)
					{
						boolean found = false;
						for (PotionEffect lE : potionsLastActive)
						{
							if (lE.getPotion() == cE.getPotion() && lE.getAmplifier() == cE.getAmplifier())
							{
								found = true;
								break;
							}
						}
						
						if (!found)
						{
							e.addPotionEffect(new PotionEffect(cE.getPotion(), (int) (cE.getDuration() * 1.8F), cE.getAmplifier(), cE.getIsAmbient(), cE.doesShowParticles()));
						}
					}
				}
			}
			
			if(powerUsed)
				lastImmuno.add(e.getUniqueID());
			else
				lastImmuno.remove(e.getUniqueID());
			
			potions.put(e.getUniqueID(), e.getActivePotionEffects());
		}
		else
		{
			lastImmuno.remove(e.getUniqueID());
			potions.remove(e.getUniqueID());
		}
	}
	
	@Override
	public int getPowerConsumption(ItemStack stack)
	{
		return stack.getItemDamage() == 3 ? LibConstants.IMMUNO_CONSUMPTION : 0;
	}
	

	@SubscribeEvent
	public void handleHurt(LivingHurtEvent event)
	{
		EntityLivingBase e = event.getEntityLiving();

		if (CyberwareAPI.isCyberwareInstalled(e, new ItemStack(this, 1, 1)))
		{
			if (event.getSource() instanceof EntityDamageSource && !(event.getSource() instanceof EntityDamageSourceIndirect))
			{
				for (ItemStack stack : e.getArmorInventoryList())
				{
					if (!stack.isEmpty() && stack.getItem() instanceof ItemArmor)
					{
						if (((ItemArmor) stack.getItem()).getArmorMaterial().getDamageReductionAmount(EntityEquipmentSlot.CHEST) > 4)
						{
							return;
						}
					}
					else if (!stack.isEmpty() && stack.getItem() instanceof ISpecialArmor)
					{
						if (((ISpecialArmor) stack.getItem()).getProperties(e, stack, event.getSource(), event.getAmount(), 1).AbsorbRatio * 25D > 4)
						{
							return;
						}
					}
				}
				
				Random random = e.getRNG();
				Entity attacker = ((EntityDamageSource) event.getSource()).getTrueSource();
				int level = 2;
				if (EnchantmentThorns.shouldHit(3, random))
				{
					if (attacker != null)
					{
						attacker.attackEntityFrom(DamageSource.causeThornsDamage(e), (float) EnchantmentThorns.getDamage(2, random));
					}
				}
			}
		}
	}
	
	
	@Override
	public int getPowerProduction(ItemStack stack)
	{
		return stack.getItemDamage() == 0 ? LibConstants.SOLAR_PRODUCTION : 0;
	}
}
