package flaxbeard.cyberware.common.item;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.CombatRules;
import net.minecraft.util.DamageSource;
import net.minecraft.util.NonNullList;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;
import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.api.CyberwareUpdateEvent;
import flaxbeard.cyberware.api.ICyberwareUserData;
import flaxbeard.cyberware.common.CyberwareContent;
import flaxbeard.cyberware.common.lib.LibConstants;
import flaxbeard.cyberware.common.network.CyberwarePacketHandler;
import flaxbeard.cyberware.common.network.ParticlePacket;

public class ItemHeartUpgrade extends ItemCyberware
{

	public ItemHeartUpgrade(String name, EnumSlot slot, String[] subnames)
	{
		super(name, slot, subnames);
		MinecraftForge.EVENT_BUS.register(this);

	}
	
	@Override
	public boolean isIncompatible(ItemStack stack, ItemStack other)
	{
		return other.getItem() == CyberwareContent.cyberheart && (stack.getItemDamage() == 0 || stack.getItemDamage() == 3);
	}
	
	@SubscribeEvent
	public void handleDeath(LivingDeathEvent event)
	{

		EntityLivingBase e = event.getEntityLiving();
		ItemStack test = new ItemStack(this, 1, 0);
		if (CyberwareAPI.isCyberwareInstalled(e, test) && !event.isCanceled())
		{
			ICyberwareUserData cyberware = CyberwareAPI.getCapability(e);
			ItemStack stack = CyberwareAPI.getCyberware(e, test);
			if ((!CyberwareAPI.getCyberwareNBT(stack).hasKey("used")) && cyberware.usePower(test, this.getPowerConsumption(test), false))
			{
				NonNullList<ItemStack> items = cyberware.getInstalledCyberware(EnumSlot.HEART);
				NonNullList<ItemStack> itemsNew = NonNullList.create();
				itemsNew.addAll(items);
				for (int i = 0; i < items.size(); i++)
				{
					ItemStack item = items.get(i);
					if (!item.isEmpty() && item.getItem() == this && item.getItemDamage() == 0)
					{
						itemsNew.set(i,ItemStack.EMPTY);
						break;
					}
				}
				if (e instanceof EntityPlayer)
				{
					cyberware.setInstalledCyberware(e, EnumSlot.HEART, itemsNew);
					cyberware.updateCapacity();
					if (!e.world.isRemote)
					{
						CyberwareAPI.updateData(e);
					}
				}
				else
				{
					stack = CyberwareAPI.getCyberware(e, test);
					NBTTagCompound com = CyberwareAPI.getCyberwareNBT(stack);
					com.setBoolean("used", true);
					stack.getTagCompound().setTag(CyberwareAPI.DATA_TAG, com);

					CyberwareAPI.updateData(e);
				}
				e.setHealth(e.getMaxHealth() / 3F);
				CyberwarePacketHandler.INSTANCE.sendToAllAround(new ParticlePacket(1, (float) e.posX, (float) e.posY + e.height / 2F, (float) e.posZ), 
						new TargetPoint(e.world.provider.getDimension(), e.posX, e.posY, e.posZ, 20));
				event.setCanceled(true);
			}
		}
	}
	
	private static Map<UUID, Integer> timesPlatelets = new HashMap<UUID, Integer>();

	@SubscribeEvent
	public void handleLivingUpdate(CyberwareUpdateEvent event)
	{
		EntityLivingBase e = event.getEntityLiving();
		
		
		ItemStack test = new ItemStack(this, 1, 2);
		if (e.ticksExisted % 20 == 0 && CyberwareAPI.isCyberwareInstalled(e, test))
		{
			isStemWorking.put(e.getUniqueID(), CyberwareAPI.getCapability(e).usePower(test, getPowerConsumption(test)));
		}
		
		
		test = new ItemStack(this, 1, 1);
		if (e.ticksExisted % 20 == 0 && CyberwareAPI.isCyberwareInstalled(e, test))
		{
			isPlateletWorking.put(e.getUniqueID(), CyberwareAPI.getCapability(e).usePower(test, getPowerConsumption(test)));
		}
		if (e != null && isPlateletWorking(e) && CyberwareAPI.isCyberwareInstalled(e, test))
		{
			if (e.getHealth() >= e.getMaxHealth() * .8F && e.getHealth() != e.getMaxHealth())
			{
				int t = getPlateletTime(e);
				if (t >= 40)
				{
					timesPlatelets.put(e.getUniqueID(), e.ticksExisted);
					e.heal(1);
				}
			}
			else
			{
				timesPlatelets.put(e.getUniqueID(), e.ticksExisted);
			}
		}
		else
		{
			if (timesPlatelets.containsKey(e.getUniqueID()))
			{
				timesPlatelets.remove(e.getUniqueID());
			}
		}
		
		if (CyberwareAPI.isCyberwareInstalled(e, new ItemStack(this, 1, 2)))
		{
			if (isStemWorking(e))
			{
				int t = getMedkitTime(e);
				if (t >= 100 && damageMedkit.get(e.getUniqueID()) > 0F)
				{
					CyberwarePacketHandler.INSTANCE.sendToAllAround(new ParticlePacket(0, (float) e.posX, (float) e.posY + e.height / 2F, (float) e.posZ), 
							new TargetPoint(e.world.provider.getDimension(), e.posX, e.posY, e.posZ, 20));

					e.heal(damageMedkit.get(e.getUniqueID()));
					timesMedkit.put(e.getUniqueID(), 0);
					damageMedkit.put(e.getUniqueID(), 0F);
				}
			}

		}
		else
		{
			if (timesMedkit.containsKey(e.getEntityId()))
			{
				//timesMedkit.remove(e);
				//damageMedkit.remove(e);
			}
		}
	}
	
	private static Map<UUID, Boolean> isPlateletWorking = new HashMap<UUID, Boolean>();
	
	private boolean isPlateletWorking(EntityLivingBase e)
	{
		if (!isPlateletWorking.containsKey(e.getUniqueID()))
		{
			isPlateletWorking.put(e.getUniqueID(), false);
			return false;
		}
		
		return isPlateletWorking.get(e.getUniqueID());
	}
	
	private static Map<UUID, Boolean> isStemWorking = new HashMap<UUID, Boolean>();
	
	private boolean isStemWorking(EntityLivingBase e)
	{
		if (!isStemWorking.containsKey(e.getUniqueID()))
		{
			isStemWorking.put(e.getUniqueID(), false);
			return false;
		}
		
		return isStemWorking.get(e.getUniqueID());
	}
	
	
	private static Map<UUID, Integer> timesMedkit = new HashMap<UUID, Integer>();
	private static Map<UUID, Float> damageMedkit = new HashMap<UUID, Float>();
	
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void handleHurt(LivingHurtEvent event)
	{
		EntityLivingBase e = event.getEntityLiving();
		if (!event.isCanceled() && CyberwareAPI.isCyberwareInstalled(e, new ItemStack(this, 1, 2)))
		{
			float damageAmount = event.getAmount();
			DamageSource damageSrc = event.getSource();

			damageAmount = applyArmorCalculations(e, damageSrc, damageAmount);
			damageAmount = applyPotionDamageCalculations(e, damageSrc, damageAmount);
			damageAmount = Math.max(damageAmount - e.getAbsorptionAmount(), 0.0F);
			
			damageMedkit.put(e.getUniqueID(), damageAmount);
			timesMedkit.put(e.getUniqueID(), e.ticksExisted);
		}
	}
	
	// Stolen from EntityLivingBase
    protected float applyArmorCalculations(EntityLivingBase e, DamageSource source, float damage)
    {
        if (!source.isUnblockable())
        {
            damage = CombatRules.getDamageAfterAbsorb(damage, (float)e.getTotalArmorValue(), (float)e.getEntityAttribute(SharedMonsterAttributes.ARMOR_TOUGHNESS).getAttributeValue());
        }

        return damage;
    }
	
	// Stolen from EntityLivingBase
	protected float applyPotionDamageCalculations(EntityLivingBase e, DamageSource source, float damage)
	{
		if (source.isDamageAbsolute())
		{
			return damage;
		}
		else
		{
			if (e.isPotionActive(MobEffects.RESISTANCE) && source != DamageSource.OUT_OF_WORLD)
			{
				int i = (e.getActivePotionEffect(MobEffects.RESISTANCE).getAmplifier() + 1) * 5;
				int j = 25 - i;
				float f = damage * (float)j;
				damage = f / 25.0F;
			}

			if (damage <= 0.0F)
			{
				return 0.0F;
			}
			else
			{
				int k = EnchantmentHelper.getEnchantmentModifierDamage(e.getArmorInventoryList(), source);

				if (k > 0)
				{
					damage = CombatRules.getDamageAfterMagicAbsorb(damage, (float)k);
				}

				return damage;
			}
		}
	}
	
	private int getPlateletTime(EntityLivingBase e)
	{
		if (e != null)
		{
			if (!timesPlatelets.containsKey(e.getUniqueID()))
			{
				timesPlatelets.put(e.getUniqueID(), e.ticksExisted);
				return 0;
			}
			return e.ticksExisted - timesPlatelets.get(e.getUniqueID());
		}
		return 0;
	}
	
	private int getMedkitTime(EntityLivingBase e)
	{
		if (e != null)
		{
			if (!timesMedkit.containsKey(e.getUniqueID()))
			{
				timesMedkit.put(e.getUniqueID(), e.ticksExisted);
				damageMedkit.put(e.getUniqueID(), 0F);
				return 0;
			}
			return e.ticksExisted - timesMedkit.get(e.getUniqueID());
		}
		return 0;
	}
	
	@SubscribeEvent
	public void power(CyberwareUpdateEvent event)
	{
		EntityLivingBase e = event.getEntityLiving();
		ItemStack test = new ItemStack(this, 1, 3);
		if (e.ticksExisted % 20 == 0 && CyberwareAPI.isCyberwareInstalled(e, test))
		{
			CyberwareAPI.getCapability(e).addPower(getPowerProduction(test), test);
		}

	}
	
	@Override
	public int getPowerConsumption(ItemStack stack)
	{
		return stack.getItemDamage() == 0 ? LibConstants.DEFIBRILLATOR_CONSUMPTION :
			stack.getItemDamage() == 1 ? LibConstants.PLATELET_CONSUMPTION :
			stack.getItemDamage() == 2 ? LibConstants.STEMCELL_CONSUMPTION : 0;
	}
	
	@Override
	public int getCapacity(ItemStack stack)
	{
		return stack.getItemDamage() == 0 ? LibConstants.DEFIBRILLATOR_CONSUMPTION: 0;
	}
	
	@Override
	public boolean hasCustomPowerMessage(ItemStack stack)
	{
		return stack.getItemDamage() == 0 ? true : false;
	}
	
	@Override
	public int getPowerProduction(ItemStack stack)
	{
		return stack.getItemDamage() == 3 ? LibConstants.COUPLER_PRODUCTION + 1 : 0;
	}

}
