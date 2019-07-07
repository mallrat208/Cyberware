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
import flaxbeard.cyberware.api.ICyberwareUserData;
import flaxbeard.cyberware.common.CyberwareContent;
import flaxbeard.cyberware.common.handler.EssentialsMissingHandler;
import flaxbeard.cyberware.common.lib.LibConstants;

public class ItemSkinUpgrade extends ItemCyberware
{

	public static final int META_SOLARSKIN              = 0;
	public static final int META_SUBDERMAL_SPIKES       = 1;
	public static final int META_SYNTHETIC_SKIN         = 2;
	public static final int META_IMMUNOSUPPRESSANT      = 3;
	
	public ItemSkinUpgrade(String name, EnumSlot slot, String[] subnames)
	{
		super(name, slot, subnames);
		MinecraftForge.EVENT_BUS.register(this);
	}
	
	@SubscribeEvent
	public void handleLivingUpdate(CyberwareUpdateEvent event)
	{
		EntityLivingBase entityLivingBase = event.getEntityLiving();
        if (entityLivingBase == null) return;
        if (entityLivingBase.ticksExisted % 20 != 0) return;
        
		ICyberwareUserData cyberwareUserData = CyberwareAPI.getCapabilityOrNull(entityLivingBase);
		if (cyberwareUserData == null) return;
		
		ItemStack itemStackSolarskin = cyberwareUserData.getCyberware(new ItemStack(this, 1, META_SOLARSKIN));
		if (!itemStackSolarskin.isEmpty())
		{
			if (entityLivingBase.world.canBlockSeeSky(new BlockPos(entityLivingBase.posX, entityLivingBase.posY + entityLivingBase.height, entityLivingBase.posZ)))
			{
				cyberwareUserData.addPower(getPowerProduction(itemStackSolarskin), itemStackSolarskin);
			}
		}
	}
	
	private Set<UUID> setIsImmunosuppressantPowered = new HashSet<>();
	private static Map<UUID, Collection<PotionEffect>> mapPotions = new HashMap<>();

	@SubscribeEvent(priority = EventPriority.HIGH)
	public void handleMissingEssentials(CyberwareUpdateEvent event)
	{
		EntityLivingBase entityLivingBase = event.getEntityLiving();
		ICyberwareUserData cyberwareUserData = CyberwareAPI.getCapabilityOrNull(entityLivingBase);
		if (cyberwareUserData == null) return;
		
		ItemStack itemStackImmunosuppressant = cyberwareUserData.getCyberware(new ItemStack(this, 1, META_IMMUNOSUPPRESSANT));
		if (!itemStackImmunosuppressant.isEmpty())
		{
			boolean isPowered = entityLivingBase.ticksExisted % 20 == 0
			                  ? cyberwareUserData.usePower(itemStackImmunosuppressant, getPowerConsumption(itemStackImmunosuppressant))
			                  : setIsImmunosuppressantPowered.contains(entityLivingBase.getUniqueID());
			
			if ( !isPowered
			  && entityLivingBase instanceof EntityPlayer
			  && entityLivingBase.ticksExisted % 100 == 0
			  && !entityLivingBase.isPotionActive(CyberwareContent.neuropozyneEffect) )
			{
				entityLivingBase.attackEntityFrom(EssentialsMissingHandler.lowessence, 2F);
			}
			
			if (mapPotions.containsKey(entityLivingBase.getUniqueID()))
			{
				Collection<PotionEffect> potionsLastActive = mapPotions.get(entityLivingBase.getUniqueID());
				Collection<PotionEffect> currentEffects = entityLivingBase.getActivePotionEffects();
				for (PotionEffect potionEffectCurrent : currentEffects)
				{
					if ( potionEffectCurrent.getPotion() == MobEffects.POISON
					  || potionEffectCurrent.getPotion() == MobEffects.HUNGER )
					{
						boolean found = false;
						for (PotionEffect potionEffectLast : potionsLastActive)
						{
							if ( potionEffectLast.getPotion() == potionEffectCurrent.getPotion()
							  && potionEffectLast.getAmplifier() == potionEffectCurrent.getAmplifier() )
							{
								found = true;
								break;
							}
						}
						
						if (!found)
						{
							entityLivingBase.addPotionEffect(new PotionEffect(potionEffectCurrent.getPotion(),
							                                                  (int) (potionEffectCurrent.getDuration() * 1.8F),
							                                                  potionEffectCurrent.getAmplifier(),
							                                                  potionEffectCurrent.getIsAmbient(),
							                                                  potionEffectCurrent.doesShowParticles() ));
						}
					}
				}
			}
			
			if (isPowered)
			{
				setIsImmunosuppressantPowered.add(entityLivingBase.getUniqueID());
			}
			else
			{
				setIsImmunosuppressantPowered.remove(entityLivingBase.getUniqueID());
			}
			
			mapPotions.put(entityLivingBase.getUniqueID(), entityLivingBase.getActivePotionEffects());
		}
		else
		{
			setIsImmunosuppressantPowered.remove(entityLivingBase.getUniqueID());
			mapPotions.remove(entityLivingBase.getUniqueID());
		}
	}
	
	@Override
	public int getPowerConsumption(ItemStack stack)
	{
		return stack.getItemDamage() == META_IMMUNOSUPPRESSANT ? LibConstants.IMMUNO_CONSUMPTION : 0;
	}
	
	@SubscribeEvent
	public void handleHurt(LivingHurtEvent event)
	{
		EntityLivingBase entityLivingBase = event.getEntityLiving();
		ICyberwareUserData cyberwareUserData = CyberwareAPI.getCapabilityOrNull(entityLivingBase);
		if (cyberwareUserData == null) return;
		
		if (cyberwareUserData.isCyberwareInstalled(new ItemStack(this, 1, META_SUBDERMAL_SPIKES)))
		{
			if ( event.getSource() instanceof EntityDamageSource
			  && !(event.getSource() instanceof EntityDamageSourceIndirect) )
			{
				for (ItemStack stack : entityLivingBase.getArmorInventoryList())
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
						if (((ISpecialArmor) stack.getItem()).getProperties(entityLivingBase, stack, event.getSource(), event.getAmount(), 1).AbsorbRatio * 25D > 4)
						{
							return;
						}
					}
				}
				
				Random random = entityLivingBase.getRNG();
				Entity attacker = event.getSource().getTrueSource();
				if (EnchantmentThorns.shouldHit(3, random))
				{
					if (attacker != null)
					{
						attacker.attackEntityFrom(DamageSource.causeThornsDamage(entityLivingBase), (float) EnchantmentThorns.getDamage(2, random));
					}
				}
			}
		}
	}
	
	@Override
	public int getPowerProduction(ItemStack stack)
	{
		return stack.getItemDamage() == META_SOLARSKIN ? LibConstants.SOLAR_PRODUCTION : 0;
	}
}
