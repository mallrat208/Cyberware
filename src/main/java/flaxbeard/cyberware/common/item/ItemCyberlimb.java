package flaxbeard.cyberware.common.item;

import java.util.HashSet;
import java.util.Set;

import flaxbeard.cyberware.api.item.ILimbReplacement;
import flaxbeard.cyberware.client.render.RenderPlayerCyberware;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.PlaySoundAtEntityEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.api.CyberwareUpdateEvent;
import flaxbeard.cyberware.api.item.ICyberware;
import flaxbeard.cyberware.api.item.ICyberware.ISidedLimb;
import flaxbeard.cyberware.common.lib.LibConstants;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemCyberlimb extends ItemCyberware implements ISidedLimb, ILimbReplacement
{
	
	public ItemCyberlimb(String name, EnumSlot[] slots, String[] subnames)
	{
		super(name, slots, subnames);
		MinecraftForge.EVENT_BUS.register(this);

	}

	@Override
	public boolean isEssential(ItemStack stack)
	{
		return true;		
	}
	
	@Override
	public boolean isIncompatible(ItemStack stack, ItemStack other)
	{
		ICyberware ware = CyberwareAPI.getCyberware(other);
		
		return ware.isEssential(other);
	}
	
	@Override
	public EnumSide getSide(ItemStack stack)
	{
		return stack.getItemDamage() % 2 == 0 ? EnumSide.LEFT : EnumSide.RIGHT;
	}
	
	public static boolean isPowered(ItemStack stack)
	{
		NBTTagCompound data = CyberwareAPI.getCyberwareNBT(stack);
		if (!data.hasKey("active"))
		{
			data.setBoolean("active", true);
		}
		return data.getBoolean("active");
	}
	
	private Set<Integer> didFall = new HashSet<Integer>();
	
	@SubscribeEvent
	public void handleFallDamage(LivingAttackEvent event)
	{
		EntityLivingBase e = event.getEntityLiving();
		if (e.world.isRemote && (CyberwareAPI.isCyberwareInstalled(e, new ItemStack(this, 1, 2)) || CyberwareAPI.isCyberwareInstalled(e, new ItemStack(this, 1, 3))) && event.getSource() == DamageSource.FALL)
		{
			if (!didFall.contains(e.getEntityId()))
			{
				didFall.add(e.getEntityId());
			}
		}
	}
	
	@SubscribeEvent
	public void handleSound(PlaySoundAtEntityEvent event)
	{
		Entity e = event.getEntity();
		if (e instanceof EntityPlayer && event.getSound() == SoundEvents.ENTITY_PLAYER_HURT && e.world.isRemote)
		{
			if (didFall.contains(e.getEntityId()))
			{
				int numLegs = 0;
				
				if (CyberwareAPI.isCyberwareInstalled(e, new ItemStack(this, 1, 2)))
				{
					numLegs++;
				}
				
				if (CyberwareAPI.isCyberwareInstalled(e, new ItemStack(this, 1, 3)))
				{
					numLegs++;
				}
				
				if (numLegs > 0)
				{	
					event.setSound(SoundEvents.ENTITY_IRONGOLEM_HURT);
					event.setPitch(event.getPitch() + 1F);
					didFall.remove((Integer) e.getEntityId());
				}
			}
		}	
	}
	
	@SubscribeEvent(priority = EventPriority.HIGH)
	public void power(CyberwareUpdateEvent event)
	{
		EntityLivingBase e = event.getEntityLiving();
		
		for (int i = 0; i < 4; i++)
		{
			ItemStack test = new ItemStack(this, 1, i);
			ItemStack installed = CyberwareAPI.getCyberware(e, test);
			if (e.ticksExisted % 20 == 0 && !installed.isEmpty())
			{
				boolean used = CyberwareAPI.getCapability(e).usePower(installed, getPowerConsumption(installed));
				
				CyberwareAPI.getCyberwareNBT(installed).setBoolean("active", used);
			}
		}

	}
	
	@Override
	public int getPowerConsumption(ItemStack stack)
	{
		return LibConstants.LIMB_CONSUMPTION;
	}
	
	@Override
	public boolean isLimbActive(ItemStack stack)
	{
		NBTTagCompound data = CyberwareAPI.getCyberwareNBT(stack);
		if(!data.hasKey("active"))
		{
			data.setBoolean("active", true);
		}
		
		return data.getBoolean("active");
	}
	
	@Override
	public ResourceLocation getTexture(ItemStack stack)
	{
		if (getQuality(stack) == CyberwareAPI.QUALITY_MANUFACTURED)
		{
			return RenderPlayerCyberware.robo;
		}
		else
		{
			return RenderPlayerCyberware.roboRust;
		}
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public Object getModel(ItemStack itemStack, boolean wideArms, Object baseWide, Object baseSkiiny, EntityPlayer player)
	{
		if(wideArms)
		{
			return baseWide;
		}
		else
		{
			return baseSkiiny;
		}
	}
}
