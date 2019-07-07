package flaxbeard.cyberware.common.item;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.util.CombatTracker;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.EntityDamageSourceIndirect;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.api.CyberwareUpdateEvent;
import flaxbeard.cyberware.api.item.EnableDisableHelper;
import flaxbeard.cyberware.api.item.IMenuItem;
import flaxbeard.cyberware.common.lib.LibConstants;
import flaxbeard.cyberware.common.network.CyberwarePacketHandler;
import flaxbeard.cyberware.common.network.SwitchHeldItemAndRotationPacket;

public class ItemMuscleUpgrade extends ItemCyberware implements IMenuItem
{

	private static final int META_WIRED_REFLEXES          = 0;
	private static final int META_MUSCLE_REPLACEMENTS     = 1;
	
	public ItemMuscleUpgrade(String name, EnumSlot slot, String[] subnames)
	{
		super(name, slot, subnames);
		MinecraftForge.EVENT_BUS.register(this);
	}
	
	private static final UUID speedId = UUID.fromString("f0ab4766-4be1-11e6-beb8-9e71128cae77");
	private static final UUID strengthId = UUID.fromString("f63d6916-4be1-11e6-beb8-9e71128cae77");

	@Override
	public void onAdded(EntityLivingBase entityLivingBase, ItemStack stack)
	{
		if (stack.getItemDamage() == META_WIRED_REFLEXES)
		{
			HashMultimap<String, AttributeModifier> multimap = HashMultimap.create();
			
			multimap.put(SharedMonsterAttributes.ATTACK_SPEED.getName(), new AttributeModifier(speedId, "Muscle speed upgrade", 1.5F, 0));
			entityLivingBase.getAttributeMap().applyAttributeModifiers(multimap);
		}
		else if (stack.getItemDamage() == META_MUSCLE_REPLACEMENTS)
		{
			HashMultimap<String, AttributeModifier> multimap = HashMultimap.create();
			
			multimap.put(SharedMonsterAttributes.ATTACK_DAMAGE.getName(), new AttributeModifier(strengthId, "Muscle damage upgrade", 3F, 0));
			entityLivingBase.getAttributeMap().applyAttributeModifiers(multimap);
		}
	}

	@Override
	public void onRemoved(EntityLivingBase entityLivingBase, ItemStack stack)
	{
		if (stack.getItemDamage() == META_WIRED_REFLEXES)
		{
			HashMultimap<String, AttributeModifier> multimap = HashMultimap.create();
			
			multimap.put(SharedMonsterAttributes.ATTACK_SPEED.getName(), new AttributeModifier(speedId, "Muscle speed upgrade", 1.5F, 0));
			entityLivingBase.getAttributeMap().removeAttributeModifiers(multimap);
		}
		else if (stack.getItemDamage() == META_MUSCLE_REPLACEMENTS)
		{
			HashMultimap<String, AttributeModifier> multimap = HashMultimap.create();
			
			multimap.put(SharedMonsterAttributes.ATTACK_DAMAGE.getName(), new AttributeModifier(strengthId, "Muscle damage upgrade", 3F, 0));
			entityLivingBase.getAttributeMap().removeAttributeModifiers(multimap);
		}
	}
	
	@Override
	public int installedStackSize(ItemStack stack)
	{
		return stack.getItemDamage() == META_WIRED_REFLEXES ? 3 : 1;
	}
	
	@SubscribeEvent
	public void handleHurt(LivingHurtEvent event)
	{
		EntityLivingBase entityLivingBase = event.getEntityLiving();

		if (entityLivingBase == null) return;

		ItemStack test = new ItemStack(this, 1, META_WIRED_REFLEXES);
		int rank = CyberwareAPI.getCyberwareRank(entityLivingBase, test);
		if ( !event.isCanceled()
		  && entityLivingBase instanceof EntityPlayer
		  && rank > 1
		  && EnableDisableHelper.isEnabled(CyberwareAPI.getCyberware(entityLivingBase, test))
		  && setIsStrengthPowered.contains(entityLivingBase.getUniqueID()) )
		{
			EntityPlayer entityPlayer = (EntityPlayer) entityLivingBase;
			if ( event.getSource() instanceof EntityDamageSource
			  && !(event.getSource() instanceof EntityDamageSourceIndirect) )
			{
				EntityDamageSource source = (EntityDamageSource) event.getSource();
				Entity attacker = source.getTrueSource();
				int lastAttacked = ReflectionHelper.getPrivateValue(CombatTracker.class, entityPlayer.getCombatTracker(), 2);
				
				if (entityPlayer.ticksExisted - lastAttacked > 120)
				{
					int indexWeapon = -1;
					ItemStack itemMainhand = entityPlayer.getHeldItemMainhand();
					if (!itemMainhand.isEmpty())
					{
						if ( entityPlayer.getItemInUseCount() > 0
						  || itemMainhand.getItem() instanceof ItemSword
						  || itemMainhand.getItem().getAttributeModifiers(EntityEquipmentSlot.MAINHAND, itemMainhand).containsKey(SharedMonsterAttributes.ATTACK_DAMAGE.getName()) )
						{
							indexWeapon = entityPlayer.inventory.currentItem;
						}
					}
					
					if (indexWeapon == -1)
					{
						double mostDamage = 0F;
						
						for (int indexHotbar = 0; indexHotbar < 10; indexHotbar++)
						{
							if (indexHotbar != entityPlayer.inventory.currentItem)
							{
								ItemStack potentialWeapon = entityPlayer.inventory.mainInventory.get(indexHotbar);
								if (!potentialWeapon.isEmpty())
								{
									Multimap<String, AttributeModifier> modifiers = potentialWeapon.getItem().getAttributeModifiers(EntityEquipmentSlot.MAINHAND, potentialWeapon);
									if (modifiers.containsKey(SharedMonsterAttributes.ATTACK_DAMAGE.getName()))
									{
										double damage = modifiers.get(SharedMonsterAttributes.ATTACK_DAMAGE.getName()).iterator().next().getAmount();
										
										if (damage > mostDamage || indexWeapon == -1)
										{
											mostDamage = damage;
											indexWeapon = indexHotbar;
										}
									}
								}
							}
						}
					}
					
					if (indexWeapon != -1)
					{
						entityPlayer.inventory.currentItem = indexWeapon;
						
						CyberwarePacketHandler.INSTANCE.sendTo(new SwitchHeldItemAndRotationPacket(indexWeapon, entityPlayer.getEntityId(),
						                                                                           rank > 2 && attacker != null ? attacker.getEntityId() : -1 ),
						                                       (EntityPlayerMP) entityPlayer);
						
						WorldServer worldServer = (WorldServer) entityPlayer.world;
						
						for (EntityPlayer trackingPlayer : worldServer.getEntityTracker().getTrackingPlayers(entityPlayer))
						{
							CyberwarePacketHandler.INSTANCE.sendTo(new SwitchHeldItemAndRotationPacket(indexWeapon, entityPlayer.getEntityId(),
							                                                                           rank > 2 && attacker != null ? attacker.getEntityId() : -1 ),
							                                       (EntityPlayerMP) trackingPlayer);
						}
					}
				}
			}
		}
	}
	
	private Set<UUID> setIsSpeedPowered = new HashSet<>();
	private Set<UUID> setIsStrengthPowered = new HashSet<>();

	@SubscribeEvent(priority=EventPriority.NORMAL)
	public void handleLivingUpdate(CyberwareUpdateEvent event)
	{
		EntityLivingBase entityLivingBase = event.getEntityLiving();

        if (entityLivingBase == null) return;

		ItemStack test = new ItemStack(this, 1, META_MUSCLE_REPLACEMENTS);
		if (CyberwareAPI.isCyberwareInstalled(entityLivingBase, test))
		{
			boolean wasPowered = setIsStrengthPowered.contains(entityLivingBase.getUniqueID());
			boolean isPowered = entityLivingBase.ticksExisted % 20 == 0
			                  ? CyberwareAPI.getCapability(entityLivingBase).usePower(test, getPowerConsumption(test))
			                  : wasPowered;
			if (isPowered)
			{
				if (!entityLivingBase.isInWater() && entityLivingBase.onGround && entityLivingBase.moveForward > 0)
				{
					entityLivingBase.moveRelative(0F, 0.0F,.5F, 0.075F);
				}

				this.onAdded(entityLivingBase, test);
				setIsStrengthPowered.add(entityLivingBase.getUniqueID());
			}
			else
			{
				this.onRemoved(entityLivingBase, test);
				setIsStrengthPowered.remove(entityLivingBase.getUniqueID());
			}
		}
		else
		{
			this.onRemoved(entityLivingBase, test);
			setIsStrengthPowered.remove(entityLivingBase.getUniqueID());
		}
		
		test = new ItemStack(this, 1, META_WIRED_REFLEXES);
		if ( CyberwareAPI.isCyberwareInstalled(entityLivingBase, test)
		  && EnableDisableHelper.isEnabled(CyberwareAPI.getCyberware(entityLivingBase, test)) )
		{
			boolean wasPowered = setIsSpeedPowered.contains(entityLivingBase.getUniqueID());
			boolean isPowered = entityLivingBase.ticksExisted % 20 == 0
			                  ? CyberwareAPI.getCapability(entityLivingBase).usePower(test, getPowerConsumption(test))
			                  : wasPowered;
			if (isPowered)
			{
				this.onAdded(entityLivingBase, test);
				setIsSpeedPowered.add(entityLivingBase.getUniqueID());
			}
			else
			{
				this.onRemoved(entityLivingBase, test);
				setIsSpeedPowered.remove(entityLivingBase.getUniqueID());
			}
		}
		else 
		{
			this.onRemoved(entityLivingBase, test);
			setIsSpeedPowered.remove(entityLivingBase.getUniqueID());
		}
	}
	
	@Override
	public int getPowerConsumption(ItemStack stack)
	{
		return stack.getItemDamage() == META_WIRED_REFLEXES ? LibConstants.REFLEXES_CONSUMPTION : LibConstants.REPLACEMENTS_CONSUMPTION;
	}
	
	@Override
	protected int getUnmodifiedEssenceCost(ItemStack stack)
	{
		if (stack.getItemDamage() == META_WIRED_REFLEXES)
		{
			switch (stack.getCount())
			{
				case 1:
					return 9;
				case 2:
					return 10;
				case 3:
					return 11;
			}
		}
		return super.getUnmodifiedEssenceCost(stack);
	}

	@Override
	public boolean hasMenu(ItemStack stack)
	{
		return stack.getItemDamage() == META_WIRED_REFLEXES;
	}

	@Override
	public void use(Entity entity, ItemStack stack)
	{
		EnableDisableHelper.toggle(stack);
	}

	@Override
	public String getUnlocalizedLabel(ItemStack stack)
	{
		return EnableDisableHelper.getUnlocalizedLabel(stack);
	}
	

	private static final float[] f = new float[] { 1.0F, 0.0F, 0.0F };
	
	@Override
	public float[] getColor(ItemStack stack)
	{
		return EnableDisableHelper.isEnabled(stack) ? f : null;
	}

	@Override
	public boolean isEssential(ItemStack stack)
	{
		return stack.getItemDamage() == META_MUSCLE_REPLACEMENTS;
	}

	@Override
	public boolean isIncompatible(ItemStack stack, ItemStack other)
	{
		return stack.getItemDamage() == META_MUSCLE_REPLACEMENTS
		    && CyberwareAPI.getCyberware(other).isEssential(other);
	}
}
