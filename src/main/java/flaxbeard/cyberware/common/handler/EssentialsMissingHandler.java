package flaxbeard.cyberware.common.handler;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.lwjgl.opengl.GL11;

import com.google.common.collect.HashMultimap;

import flaxbeard.cyberware.Cyberware;
import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.api.CyberwareUpdateEvent;
import flaxbeard.cyberware.api.ICyberwareUserData;
import flaxbeard.cyberware.api.item.ICyberware.EnumSlot;
import flaxbeard.cyberware.api.item.ICyberware.ISidedLimb.EnumSide;
import flaxbeard.cyberware.client.ClientUtils;
import flaxbeard.cyberware.common.CyberwareConfig;
import flaxbeard.cyberware.common.CyberwareContent;
import flaxbeard.cyberware.common.block.tile.TileEntitySurgery;
import flaxbeard.cyberware.common.item.ItemCyberlimb;

public class EssentialsMissingHandler
{
	public static final DamageSource brainless = new DamageSource("cyberware.brainless").setDamageBypassesArmor().setDamageIsAbsolute();
	public static final DamageSource heartless = new DamageSource("cyberware.heartless").setDamageBypassesArmor().setDamageIsAbsolute();
	public static final DamageSource surgery = new DamageSource("cyberware.surgery").setDamageBypassesArmor();
	public static final DamageSource spineless = new DamageSource("cyberware.spineless").setDamageBypassesArmor().setDamageIsAbsolute();
	public static final DamageSource nomuscles = new DamageSource("cyberware.nomuscles").setDamageBypassesArmor().setDamageIsAbsolute();
	public static final DamageSource noessence = new DamageSource("cyberware.noessence").setDamageBypassesArmor().setDamageIsAbsolute();
	public static final DamageSource lowessence = new DamageSource("cyberware.lowessence").setDamageBypassesArmor().setDamageIsAbsolute();

	public static final EssentialsMissingHandler INSTANCE = new EssentialsMissingHandler();

	private static Map<Integer, Integer> timesLungs = new HashMap<>();
	
	private static final UUID speedId = UUID.fromString("fe00fdea-5044-11e6-beb8-9e71128cae77");

	private Map<Integer, Boolean> last = new HashMap<>();
	private Map<Integer, Boolean> lastClient = new HashMap<>();
	
	@SubscribeEvent
	public void triggerCyberwareEvent(LivingUpdateEvent event)
	{
		EntityLivingBase entityLivingBase = event.getEntityLiving();
		
		ICyberwareUserData cyberwareUserData = CyberwareAPI.getCapabilityOrNull(entityLivingBase);
		if (cyberwareUserData != null)
		{
			CyberwareUpdateEvent cyberwareUpdateEvent = new CyberwareUpdateEvent(entityLivingBase);
			MinecraftForge.EVENT_BUS.post(cyberwareUpdateEvent);
		}
	}
	
	@SubscribeEvent(priority=EventPriority.LOWEST)
	public void handleMissingEssentials(CyberwareUpdateEvent event)
	{
		EntityLivingBase entityLivingBase = event.getEntityLiving();
		if (entityLivingBase == null) return;
		ICyberwareUserData cyberwareUserData = CyberwareAPI.getCapabilityOrNull(entityLivingBase);
		if (cyberwareUserData == null) return;
		
		if (entityLivingBase.ticksExisted % 20 == 0)
		{
			cyberwareUserData.resetBuffer();
		}
		
		if (!cyberwareUserData.hasEssential(EnumSlot.CRANIUM))
		{
			entityLivingBase.attackEntityFrom(brainless, Integer.MAX_VALUE);
		}

		if ( cyberwareUserData.getTolerance(entityLivingBase) < CyberwareConfig.CRITICAL_ESSENCE
		  && entityLivingBase instanceof EntityPlayer
		  && entityLivingBase.ticksExisted % 100 == 0
		  && !entityLivingBase.isPotionActive(CyberwareContent.neuropozyneEffect) )
		{
			entityLivingBase.addPotionEffect(new PotionEffect(CyberwareContent.rejectionEffect, 110, 0, true, false));
			entityLivingBase.attackEntityFrom(lowessence, 2F);
		}
		
		int numMissingLegs = 0;
		int numMissingLegsVisible = 0;
		
		if (cyberwareUserData.getTolerance(entityLivingBase) <= 0)
		{
			entityLivingBase.attackEntityFrom(noessence, Integer.MAX_VALUE);
		}
		
		if (!cyberwareUserData.hasEssential(EnumSlot.LEG, EnumSide.LEFT))
		{
			numMissingLegs++;
			numMissingLegsVisible++;
		}
		if (!cyberwareUserData.hasEssential(EnumSlot.LEG, EnumSide.RIGHT))
		{
			numMissingLegs++;
			numMissingLegsVisible++;
		}
		
		ItemStack legLeft = cyberwareUserData.getCyberware(new ItemStack(CyberwareContent.cyberlimbs, 1, ItemCyberlimb.META_LEFT_CYBER_LEG));
		if ( !legLeft.isEmpty()
		  && !ItemCyberlimb.isPowered(legLeft) )
		{
			numMissingLegs++;
		}
		
		ItemStack legRight = cyberwareUserData.getCyberware(new ItemStack(CyberwareContent.cyberlimbs, 1, ItemCyberlimb.META_RIGHT_CYBER_LEG));
		if ( !legRight.isEmpty()
		  && !ItemCyberlimb.isPowered(legRight) )
		{
			numMissingLegs++;
		}
		
		if (entityLivingBase instanceof EntityPlayer)
		{
			if (numMissingLegsVisible == 2)
			{
				entityLivingBase.height = 1.8F - (10F / 16F);
				((EntityPlayer) entityLivingBase).eyeHeight = ((EntityPlayer) entityLivingBase).getDefaultEyeHeight() - (10F / 16F);
				AxisAlignedBB axisalignedbb = entityLivingBase.getEntityBoundingBox();
				entityLivingBase.setEntityBoundingBox(new AxisAlignedBB(
						axisalignedbb.minX, axisalignedbb.minY, axisalignedbb.minZ,
						axisalignedbb.minX + entityLivingBase.width, axisalignedbb.minY + entityLivingBase.height, axisalignedbb.minZ + entityLivingBase.width));
				
				if (entityLivingBase.world.isRemote)
				{
					lastClient.put(entityLivingBase.getEntityId(), true);
				}
				else
				{
					last.put(entityLivingBase.getEntityId(), true);
				}
			}
			else if (last(entityLivingBase.world.isRemote, entityLivingBase))
			{
				entityLivingBase.height = 1.8F;
				((EntityPlayer) entityLivingBase).eyeHeight = ((EntityPlayer) entityLivingBase).getDefaultEyeHeight();
				AxisAlignedBB axisalignedbb = entityLivingBase.getEntityBoundingBox();
				entityLivingBase.setEntityBoundingBox(new AxisAlignedBB(
						axisalignedbb.minX, axisalignedbb.minY, axisalignedbb.minZ,
						axisalignedbb.minX + entityLivingBase.width, axisalignedbb.minY + entityLivingBase.height, axisalignedbb.minZ + entityLivingBase.width ));
				
				if (entityLivingBase.world.isRemote)
				{
					lastClient.put(entityLivingBase.getEntityId(), false);
				}
				else
				{
					last.put(entityLivingBase.getEntityId(), false);
				}
			}
		}
		
		if ( numMissingLegs >= 1
		  && entityLivingBase.onGround )
		{
			HashMultimap<String, AttributeModifier> multimap = HashMultimap.create();
			
			multimap.put(SharedMonsterAttributes.MOVEMENT_SPEED.getName(), new AttributeModifier(speedId, "Missing leg speed", -100F, 0));
			entityLivingBase.getAttributeMap().applyAttributeModifiers(multimap);

			//entityLivingBase.moveEntity(entityLivingBase.lastTickPosX - entityLivingBase.posX, 0, entityLivingBase.lastTickPosZ - entityLivingBase.posZ);
		}
		else
		{
			HashMultimap<String, AttributeModifier> multimap = HashMultimap.create();
			
			multimap.put(SharedMonsterAttributes.MOVEMENT_SPEED.getName(), new AttributeModifier(speedId, "Missing leg speed", -100F, 0));
			entityLivingBase.getAttributeMap().removeAttributeModifiers(multimap);
		}
		
		if (!cyberwareUserData.hasEssential(EnumSlot.HEART))
		{
			entityLivingBase.attackEntityFrom(heartless, Integer.MAX_VALUE);
		}
		
		if (!cyberwareUserData.hasEssential(EnumSlot.BONE))
		{
			entityLivingBase.attackEntityFrom(spineless, Integer.MAX_VALUE);
		}
		
		if (!cyberwareUserData.hasEssential(EnumSlot.MUSCLE))
		{
			entityLivingBase.attackEntityFrom(nomuscles, Integer.MAX_VALUE);
		}
		
		if (!cyberwareUserData.hasEssential(EnumSlot.LUNGS))
		{
			if (getLungsTime(entityLivingBase) >= 20)
			{
				timesLungs.put(entityLivingBase.getEntityId(), entityLivingBase.ticksExisted);
				entityLivingBase.attackEntityFrom(DamageSource.DROWN, 2F);
			}
		}
		else
		{
			timesLungs.remove(entityLivingBase.getEntityId());
		}
	}
	
	private boolean last(boolean remote, EntityLivingBase entityLivingBase)
	{
		if (remote)
		{
			if (!lastClient.containsKey(entityLivingBase.getEntityId()))
			{
				lastClient.put(entityLivingBase.getEntityId(), false);
			}
			return lastClient.get(entityLivingBase.getEntityId());
		}
		else
		{
			if (!last.containsKey(entityLivingBase.getEntityId()))
			{
				last.put(entityLivingBase.getEntityId(), false);
			}
			return last.get(entityLivingBase.getEntityId());
		}
	}
	
	@SubscribeEvent
	public void handleJump(LivingJumpEvent event)
	{
	    EntityLivingBase entityLivingBase = event.getEntityLiving();
		
		ICyberwareUserData cyberwareUserData = CyberwareAPI.getCapabilityOrNull(entityLivingBase);
		if (cyberwareUserData != null)
		{
			int numMissingLegs = 0;
			
			if (!cyberwareUserData.hasEssential(EnumSlot.LEG, EnumSide.LEFT))
			{
				numMissingLegs++;
			}
			if (!cyberwareUserData.hasEssential(EnumSlot.LEG, EnumSide.RIGHT))
			{
				numMissingLegs++;
			}
			
			ItemStack legLeft = cyberwareUserData.getCyberware(new ItemStack(CyberwareContent.cyberlimbs, 1, ItemCyberlimb.META_LEFT_CYBER_LEG));
			if (!legLeft.isEmpty() && !ItemCyberlimb.isPowered(legLeft))
			{
				numMissingLegs++;
			}
			
			ItemStack legRight = cyberwareUserData.getCyberware(new ItemStack(CyberwareContent.cyberlimbs, 1, ItemCyberlimb.META_RIGHT_CYBER_LEG));
			if (!legRight.isEmpty() && !ItemCyberlimb.isPowered(legRight))
			{
				numMissingLegs++;
			}
			
			if (numMissingLegs == 2)
			{
				entityLivingBase.motionY = 0.2F;
			}
		}
	}
		
	private int getLungsTime(@Nonnull EntityLivingBase entityLivingBase)
	{
		Integer timeLungs = timesLungs.computeIfAbsent(entityLivingBase.getEntityId(), k -> entityLivingBase.ticksExisted);
		return entityLivingBase.ticksExisted - timeLungs;
	}
	
	private static Map<Integer, Integer> mapHunger = new HashMap<>();
	private static Map<Integer, Float> mapSaturation = new HashMap<>();

	@SubscribeEvent
	public void handleEatFoodTick(LivingEntityUseItemEvent.Tick event)
	{
		EntityLivingBase entityLivingBase = event.getEntityLiving();
		ItemStack stack = event.getItem();

        if (entityLivingBase == null) return;

		if ( entityLivingBase instanceof EntityPlayer
		  && !stack.isEmpty()
		  && stack.getItem().getItemUseAction(stack) == EnumAction.EAT )
		{
			EntityPlayer entityPlayer = (EntityPlayer) entityLivingBase;
			ICyberwareUserData cyberwareUserData = CyberwareAPI.getCapabilityOrNull(entityLivingBase);
			
			if (cyberwareUserData != null && !cyberwareUserData.hasEssential(EnumSlot.LOWER_ORGANS))
			{
				mapHunger.put(entityPlayer.getEntityId(), entityPlayer.getFoodStats().getFoodLevel());
				mapSaturation.put(entityPlayer.getEntityId(), entityPlayer.getFoodStats().getSaturationLevel());
				return;
			}
		}
		
		mapHunger.remove(entityLivingBase.getEntityId());
		mapSaturation.remove(entityLivingBase.getEntityId());
	}
	
	@SubscribeEvent
	public void handleEatFoodEnd(LivingEntityUseItemEvent.Finish event)
	{
		EntityLivingBase entityLivingBase = event.getEntityLiving();
		ItemStack stack = event.getItem();

		if ( entityLivingBase instanceof EntityPlayer
		  && !stack.isEmpty()
		  && stack.getItem().getItemUseAction(stack) == EnumAction.EAT )
		{
			EntityPlayer entityPlayer = (EntityPlayer) entityLivingBase;
			ICyberwareUserData cyberwareUserData = CyberwareAPI.getCapabilityOrNull(entityLivingBase);
			
			if (cyberwareUserData != null && !cyberwareUserData.hasEssential(EnumSlot.LOWER_ORGANS))
			{
				Integer hunger = mapHunger.get(entityPlayer.getEntityId());
				if (hunger != null)
				{
					entityPlayer.getFoodStats().setFoodLevel(hunger);
				}
				
				Float saturation = mapSaturation.get(entityPlayer.getEntityId());
				if (saturation != null)
				{
					entityPlayer.getFoodStats().setFoodSaturationLevel(saturation);
				}
			}
		}
	}
	
	public static final ResourceLocation BLACK_PX = new ResourceLocation(Cyberware.MODID + ":textures/gui/blackpx.png");
	
	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void overlayPre(ClientTickEvent event)
	{
		if ( event.phase == Phase.START
		  && Minecraft.getMinecraft() != null
		  && Minecraft.getMinecraft().player != null )
		{
			EntityPlayer entityPlayer = Minecraft.getMinecraft().player;

			HashMultimap<String, AttributeModifier> multimap = HashMultimap.create();
			multimap.put(SharedMonsterAttributes.MOVEMENT_SPEED.getName(), new AttributeModifier(speedId, "Missing leg speed", -100F, 0));
			entityPlayer.getAttributeMap().removeAttributeModifiers(multimap);
		}
	}

	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void overlayPre(RenderGameOverlayEvent.Pre event)
	{
		if (event.getType() == ElementType.ALL)
		{
			EntityPlayer entityPlayer = Minecraft.getMinecraft().player;
            if (entityPlayer == null) return;
			
			ICyberwareUserData cyberwareUserData = CyberwareAPI.getCapabilityOrNull(entityPlayer);
			if ( cyberwareUserData != null
			  && !cyberwareUserData.hasEssential(EnumSlot.EYES)
			  && !entityPlayer.isCreative() )
			{
				GlStateManager.pushMatrix();
				GlStateManager.enableBlend();
				GlStateManager.color(1F, 1F, 1F, .9F);
				Minecraft.getMinecraft().getTextureManager().bindTexture(BLACK_PX);
				ClientUtils.drawTexturedModalRect(0, 0, 0, 0, Minecraft.getMinecraft().displayWidth, Minecraft.getMinecraft().displayHeight);
				GlStateManager.popMatrix();
			}
			
			if (TileEntitySurgery.workingOnPlayer)
			{
				float trans = 1.0F;
				float ticks = TileEntitySurgery.playerProgressTicks + event.getPartialTicks();
				if (ticks < 20F)
				{
					trans = ticks / 20F;
				}
				else if (ticks > 60F)
				{
					trans = (80F - ticks) / 20F;
				}
				GL11.glEnable(GL11.GL_BLEND);
				GL11.glColor4f(1.0F, 1.0F, 1.0F, trans);
				Minecraft.getMinecraft().getTextureManager().bindTexture(BLACK_PX);
				ClientUtils.drawTexturedModalRect(0, 0, 0, 0, Minecraft.getMinecraft().displayWidth, Minecraft.getMinecraft().displayHeight);
				GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
				GL11.glDisable(GL11.GL_BLEND);
			}
		}
	}
	
	@SubscribeEvent
	public void handleMissingSkin(LivingHurtEvent event)
	{
		EntityLivingBase entityLivingBase = event.getEntityLiving();
		
		ICyberwareUserData cyberwareUserData = CyberwareAPI.getCapabilityOrNull(entityLivingBase);
		if (cyberwareUserData != null)
		{
			if (!cyberwareUserData.hasEssential(EnumSlot.SKIN))
			{
				if (!event.getSource().isUnblockable() || event.getSource() == DamageSource.FALL)
				{
					event.setAmount(event.getAmount() * 3F);
				}
			}
		}
	}
	
	@SubscribeEvent
	public void handleEntityInteract(PlayerInteractEvent.EntityInteract event)
	{
		EntityLivingBase entityLivingBase = event.getEntityLiving();
		
		ICyberwareUserData cyberwareUserData = CyberwareAPI.getCapabilityOrNull(entityLivingBase);
		if (cyberwareUserData != null)
		{
			processEvent(event, event.getHand(), event.getEntityPlayer(), cyberwareUserData);
		}
	}
	
	@SubscribeEvent
	public void handleLeftClickBlock(PlayerInteractEvent.LeftClickBlock event)
	{
		EntityLivingBase entityLivingBase = event.getEntityLiving();
		
		ICyberwareUserData cyberwareUserData = CyberwareAPI.getCapabilityOrNull(entityLivingBase);
		if (cyberwareUserData != null)
		{
			processEvent(event, event.getHand(), event.getEntityPlayer(), cyberwareUserData);
		}
	}
	
	@SubscribeEvent
	public void handleRightClickBlock(PlayerInteractEvent.RightClickBlock event)
	{
		EntityLivingBase entityLivingBase = event.getEntityLiving();
		
		ICyberwareUserData cyberwareUserData = CyberwareAPI.getCapabilityOrNull(entityLivingBase);
		if (cyberwareUserData != null)
		{
			processEvent(event, event.getHand(), event.getEntityPlayer(), cyberwareUserData);
		}
	}
	
	@SubscribeEvent
	public void handleRightClickItem(PlayerInteractEvent.RightClickItem event)
	{
		EntityLivingBase entityLivingBase = event.getEntityLiving();
		
		ICyberwareUserData cyberwareUserData = CyberwareAPI.getCapabilityOrNull(entityLivingBase);
		if (cyberwareUserData != null)
		{
			processEvent(event, event.getHand(), event.getEntityPlayer(), cyberwareUserData);
		}
	}

	private void processEvent(Event event, EnumHand hand, EntityPlayer entityPlayer, ICyberwareUserData cyberwareUserData)
	{
		EnumHandSide mainHand = entityPlayer.getPrimaryHand();
		EnumHandSide offHand = ((mainHand == EnumHandSide.LEFT) ? EnumHandSide.RIGHT : EnumHandSide.LEFT);
		EnumSide correspondingMainHand = ((mainHand == EnumHandSide.RIGHT) ? EnumSide.RIGHT : EnumSide.LEFT);
		EnumSide correspondingOffHand = ((offHand == EnumHandSide.RIGHT) ? EnumSide.RIGHT : EnumSide.LEFT);
		
		boolean leftUnpowered = false;
		ItemStack armLeft = cyberwareUserData.getCyberware(new ItemStack(CyberwareContent.cyberlimbs, 1, ItemCyberlimb.META_LEFT_CYBER_ARM));
		if (!armLeft.isEmpty() && !ItemCyberlimb.isPowered(armLeft))
		{
			leftUnpowered = true;
		}
		
		boolean rightUnpowered = false;
		ItemStack armRight = cyberwareUserData.getCyberware(new ItemStack(CyberwareContent.cyberlimbs, 1, ItemCyberlimb.META_RIGHT_CYBER_ARM));
		if (!armRight.isEmpty() && !ItemCyberlimb.isPowered(armRight))
		{
			rightUnpowered = true;
		}

		if (hand == EnumHand.MAIN_HAND && (!cyberwareUserData.hasEssential(EnumSlot.ARM, correspondingMainHand) || leftUnpowered))
		{
			event.setCanceled(true);
		}
		else if (hand == EnumHand.OFF_HAND && (!cyberwareUserData.hasEssential(EnumSlot.ARM, correspondingOffHand) || rightUnpowered))
		{
			event.setCanceled(true);
		}
	}
}
