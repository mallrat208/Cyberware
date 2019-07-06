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
	
	public static final int META_INTERNAL_DEFIBRILLATOR  = 0;
	public static final int META_PLATELET_DISPATCHER     = 1;
	public static final int META_STEM_CELL_SYNTHESIZER   = 2;
	public static final int META_CARDIOVASCULAR_COUPLER  = 3;
	
	public ItemHeartUpgrade(String name, EnumSlot slot, String[] subnames)
	{
		super(name, slot, subnames);
		MinecraftForge.EVENT_BUS.register(this);
	}
	
	@Override
	public boolean isIncompatible(ItemStack stack, ItemStack other)
	{
		return other.getItem() == CyberwareContent.cyberheart
		    && ( stack.getItemDamage() == META_INTERNAL_DEFIBRILLATOR
		      || stack.getItemDamage() == META_CARDIOVASCULAR_COUPLER );
	}
	
	@SubscribeEvent
	public void handleDeath(LivingDeathEvent event)
	{
		EntityLivingBase entityLivingBase = event.getEntityLiving();
		ItemStack itemStackInternalDefibrillator = new ItemStack(this, 1, META_INTERNAL_DEFIBRILLATOR);
		if (CyberwareAPI.isCyberwareInstalled(entityLivingBase, itemStackInternalDefibrillator) && !event.isCanceled())
		{
			ICyberwareUserData cyberwareUserData = CyberwareAPI.getCapability(entityLivingBase);
			ItemStack stack = CyberwareAPI.getCyberware(entityLivingBase, itemStackInternalDefibrillator);
			if ( (!CyberwareAPI.getCyberwareNBT(stack).hasKey("used"))
			  && cyberwareUserData.usePower(itemStackInternalDefibrillator, getPowerConsumption(itemStackInternalDefibrillator), false) )
			{
				NonNullList<ItemStack> items = cyberwareUserData.getInstalledCyberware(EnumSlot.HEART);
				NonNullList<ItemStack> itemsNew = NonNullList.create();
				itemsNew.addAll(items);
				for (int i = 0; i < items.size(); i++)
				{
					ItemStack item = items.get(i);
					if ( !item.isEmpty()
					  && item.getItem() == this
					  && item.getItemDamage() == META_INTERNAL_DEFIBRILLATOR )
					{
						itemsNew.set(i, ItemStack.EMPTY);
						break;
					}
				}
				if (entityLivingBase instanceof EntityPlayer)
				{
					cyberwareUserData.setInstalledCyberware(entityLivingBase, EnumSlot.HEART, itemsNew);
					cyberwareUserData.updateCapacity();
					if (!entityLivingBase.world.isRemote)
					{
						CyberwareAPI.updateData(entityLivingBase);
					}
				}
				else
				{
					stack = CyberwareAPI.getCyberware(entityLivingBase, itemStackInternalDefibrillator);
					NBTTagCompound tagCompoundCyberware = CyberwareAPI.getCyberwareNBT(stack);
					tagCompoundCyberware.setBoolean("used", true);
					stack.getTagCompound().setTag(CyberwareAPI.DATA_TAG, tagCompoundCyberware);

					CyberwareAPI.updateData(entityLivingBase);
				}
				entityLivingBase.setHealth(entityLivingBase.getMaxHealth() / 3F);
				CyberwarePacketHandler.INSTANCE.sendToAllAround(new ParticlePacket(1, (float) entityLivingBase.posX, (float) entityLivingBase.posY + entityLivingBase.height / 2F, (float) entityLivingBase.posZ), 
						new TargetPoint(entityLivingBase.world.provider.getDimension(), entityLivingBase.posX, entityLivingBase.posY, entityLivingBase.posZ, 20));
				event.setCanceled(true);
			}
		}
	}
	
	private static Map<UUID, Integer> timesPlatelets = new HashMap<>();

	@SubscribeEvent
	public void handleLivingUpdate(CyberwareUpdateEvent event)
	{
		EntityLivingBase entityLivingBase = event.getEntityLiving();
		
		
		ItemStack test = new ItemStack(this, 1, META_STEM_CELL_SYNTHESIZER);
		if (entityLivingBase.ticksExisted % 20 == 0 && CyberwareAPI.isCyberwareInstalled(entityLivingBase, test))
		{
			isStemWorking.put(entityLivingBase.getUniqueID(), CyberwareAPI.getCapability(entityLivingBase).usePower(test, getPowerConsumption(test)));
		}
		
		
		test = new ItemStack(this, 1, META_PLATELET_DISPATCHER);
		if (entityLivingBase.ticksExisted % 20 == 0 && CyberwareAPI.isCyberwareInstalled(entityLivingBase, test))
		{
			isPlateletWorking.put(entityLivingBase.getUniqueID(), CyberwareAPI.getCapability(entityLivingBase).usePower(test, getPowerConsumption(test)));
		}
		if (isPlateletWorking(entityLivingBase) && CyberwareAPI.isCyberwareInstalled(entityLivingBase, test))
		{
			if (entityLivingBase.getHealth() >= entityLivingBase.getMaxHealth() * .8F && entityLivingBase.getHealth() != entityLivingBase.getMaxHealth())
			{
				int t = getPlateletTime(entityLivingBase);
				if (t >= 40)
				{
					timesPlatelets.put(entityLivingBase.getUniqueID(), entityLivingBase.ticksExisted);
					entityLivingBase.heal(1);
				}
			}
			else
			{
				timesPlatelets.put(entityLivingBase.getUniqueID(), entityLivingBase.ticksExisted);
			}
		}
		else
		{
			timesPlatelets.remove(entityLivingBase.getUniqueID());
		}
		
		if (CyberwareAPI.isCyberwareInstalled(entityLivingBase, new ItemStack(this, 1, META_STEM_CELL_SYNTHESIZER)))
		{
			if (isStemWorking(entityLivingBase))
			{
				int t = getMedkitTime(entityLivingBase);
				if ( t >= 100
				  && damageMedkit.get(entityLivingBase.getUniqueID()) > 0F )
				{
					CyberwarePacketHandler.INSTANCE.sendToAllAround(new ParticlePacket(0, (float) entityLivingBase.posX, (float) entityLivingBase.posY + entityLivingBase.height / 2F, (float) entityLivingBase.posZ), 
							new TargetPoint(entityLivingBase.world.provider.getDimension(), entityLivingBase.posX, entityLivingBase.posY, entityLivingBase.posZ, 20));

					entityLivingBase.heal(damageMedkit.get(entityLivingBase.getUniqueID()));
					timesMedkit.put(entityLivingBase.getUniqueID(), 0);
					damageMedkit.put(entityLivingBase.getUniqueID(), 0F);
				}
			}
		}
		/*
		else
		{
			if (timesMedkit.containsKey(entityLivingBase.getEntityId()))
			{
				timesMedkit.remove(entityLivingBase);
				damageMedkit.remove(entityLivingBase);
			}
		}
		*/
	}
	
	private static Map<UUID, Boolean> isPlateletWorking = new HashMap<>();
	
	private boolean isPlateletWorking(EntityLivingBase entityLivingBase)
	{
		if (!isPlateletWorking.containsKey(entityLivingBase.getUniqueID()))
		{
			isPlateletWorking.put(entityLivingBase.getUniqueID(), false);
			return false;
		}
		
		return isPlateletWorking.get(entityLivingBase.getUniqueID());
	}
	
	private static Map<UUID, Boolean> isStemWorking = new HashMap<>();
	
	private boolean isStemWorking(EntityLivingBase entityLivingBase)
	{
		if (!isStemWorking.containsKey(entityLivingBase.getUniqueID()))
		{
			isStemWorking.put(entityLivingBase.getUniqueID(), false);
			return false;
		}
		
		return isStemWorking.get(entityLivingBase.getUniqueID());
	}
	
	
	private static Map<UUID, Integer> timesMedkit = new HashMap<>();
	private static Map<UUID, Float> damageMedkit = new HashMap<>();
	
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void handleHurt(LivingHurtEvent event)
	{
		EntityLivingBase entityLivingBase = event.getEntityLiving();
		if ( !event.isCanceled()
		  && CyberwareAPI.isCyberwareInstalled(entityLivingBase, new ItemStack(this, 1, META_STEM_CELL_SYNTHESIZER)) )
		{
			float damageAmount = event.getAmount();
			DamageSource damageSrc = event.getSource();

			damageAmount = applyArmorCalculations(entityLivingBase, damageSrc, damageAmount);
			damageAmount = applyPotionDamageCalculations(entityLivingBase, damageSrc, damageAmount);
			damageAmount = Math.max(damageAmount - entityLivingBase.getAbsorptionAmount(), 0.0F);
			
			damageMedkit.put(entityLivingBase.getUniqueID(), damageAmount);
			timesMedkit.put(entityLivingBase.getUniqueID(), entityLivingBase.ticksExisted);
		}
	}
	
	// Stolen from EntityLivingBase
    protected float applyArmorCalculations(EntityLivingBase entityLivingBase, DamageSource source, float damage)
    {
        if (!source.isUnblockable())
        {
            damage = CombatRules.getDamageAfterAbsorb(damage, (float)entityLivingBase.getTotalArmorValue(), (float)entityLivingBase.getEntityAttribute(SharedMonsterAttributes.ARMOR_TOUGHNESS).getAttributeValue());
        }

        return damage;
    }
	
	// Stolen from EntityLivingBase
	protected float applyPotionDamageCalculations(EntityLivingBase entityLivingBase, DamageSource source, float damage)
	{
		if (source.isDamageAbsolute())
		{
			return damage;
		}
		else
		{
			if (entityLivingBase.isPotionActive(MobEffects.RESISTANCE) && source != DamageSource.OUT_OF_WORLD)
			{
				int i = (entityLivingBase.getActivePotionEffect(MobEffects.RESISTANCE).getAmplifier() + 1) * 5;
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
				int k = EnchantmentHelper.getEnchantmentModifierDamage(entityLivingBase.getArmorInventoryList(), source);

				if (k > 0)
				{
					damage = CombatRules.getDamageAfterMagicAbsorb(damage, (float)k);
				}

				return damage;
			}
		}
	}
	
	private int getPlateletTime(EntityLivingBase entityLivingBase)
	{
		if (entityLivingBase != null)
		{
			if (!timesPlatelets.containsKey(entityLivingBase.getUniqueID()))
			{
				timesPlatelets.put(entityLivingBase.getUniqueID(), entityLivingBase.ticksExisted);
				return 0;
			}
			return entityLivingBase.ticksExisted - timesPlatelets.get(entityLivingBase.getUniqueID());
		}
		return 0;
	}
	
	private int getMedkitTime(EntityLivingBase entityLivingBase)
	{
		if (entityLivingBase != null)
		{
			if (!timesMedkit.containsKey(entityLivingBase.getUniqueID()))
			{
				timesMedkit.put(entityLivingBase.getUniqueID(), entityLivingBase.ticksExisted);
				damageMedkit.put(entityLivingBase.getUniqueID(), 0F);
				return 0;
			}
			return entityLivingBase.ticksExisted - timesMedkit.get(entityLivingBase.getUniqueID());
		}
		return 0;
	}
	
	@SubscribeEvent
	public void power(CyberwareUpdateEvent event)
	{
		EntityLivingBase entityLivingBase = event.getEntityLiving();
		ItemStack test = new ItemStack(this, 1, META_CARDIOVASCULAR_COUPLER);
		if (entityLivingBase.ticksExisted % 20 == 0 && CyberwareAPI.isCyberwareInstalled(entityLivingBase, test))
		{
			CyberwareAPI.getCapability(entityLivingBase).addPower(getPowerProduction(test), test);
		}
	}
	
	@Override
	public int getPowerConsumption(ItemStack stack)
	{
		return stack.getItemDamage() == META_INTERNAL_DEFIBRILLATOR ? LibConstants.DEFIBRILLATOR_CONSUMPTION
		     : stack.getItemDamage() == META_PLATELET_DISPATCHER ? LibConstants.PLATELET_CONSUMPTION
		     : stack.getItemDamage() == META_STEM_CELL_SYNTHESIZER ? LibConstants.STEMCELL_CONSUMPTION
		     : 0;
	}
	
	@Override
	public int getCapacity(ItemStack stack)
	{
		return stack.getItemDamage() == META_INTERNAL_DEFIBRILLATOR ? LibConstants.DEFIBRILLATOR_CONSUMPTION: 0;
	}
	
	@Override
	public boolean hasCustomPowerMessage(ItemStack stack)
	{
		return stack.getItemDamage() == META_INTERNAL_DEFIBRILLATOR;
	}
	
	@Override
	public int getPowerProduction(ItemStack stack)
	{
		return stack.getItemDamage() == META_CARDIOVASCULAR_COUPLER ? LibConstants.COUPLER_PRODUCTION + 1 : 0;
	}

}
