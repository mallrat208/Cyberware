package flaxbeard.cyberware.common.handler;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import flaxbeard.cyberware.api.item.ILimbReplacement;
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
import net.minecraftforge.event.entity.player.AttackEntityEvent;
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

	private static Map<Integer, Integer> timesLungs = new HashMap<Integer, Integer>();
	
	private static final UUID speedId = UUID.fromString("fe00fdea-5044-11e6-beb8-9e71128cae77");

	private Map<Integer, Boolean> last = new HashMap<Integer, Boolean>();
	private Map<Integer, Boolean> lastClient = new HashMap<Integer, Boolean>();
	
	@SubscribeEvent
	public void triggerCyberwareEvent(LivingUpdateEvent event)
	{
		EntityLivingBase e = event.getEntityLiving();

		if (e == null)
			return;

		if (CyberwareAPI.hasCapability(e))
		{
			CyberwareUpdateEvent event2 = new CyberwareUpdateEvent(e);
			MinecraftForge.EVENT_BUS.post(event2);
		}
	}

	@SubscribeEvent(priority=EventPriority.LOWEST)
	public void handleMissingEssentials(CyberwareUpdateEvent event)
	{
		EntityLivingBase e = event.getEntityLiving();

		if (e == null)
			return;

		ICyberwareUserData cyberware = CyberwareAPI.getCapability(e);
		
		if (e.ticksExisted % 20 == 0)
		{
			cyberware.resetBuffer();
		}
		
		if (!cyberware.hasEssential(EnumSlot.CRANIUM))
		{
			e.attackEntityFrom(brainless, Integer.MAX_VALUE);
		}

		if (cyberware.getTolerance(e) < CyberwareConfig.CRITICAL_ESSENCE && e instanceof EntityPlayer && e.ticksExisted % 100 == 0 && !e.isPotionActive(CyberwareContent.neuropozyneEffect))
		{
			e.addPotionEffect(new PotionEffect(CyberwareContent.rejectionEffect, 110, 0, true, false));
			e.attackEntityFrom(lowessence, 2F);
		}
					
		int numMissingLegs = 0;
		int numMissingLegsVisible = 0;

		if (cyberware.getTolerance(e) <= 0)
		{
			e.attackEntityFrom(noessence, Integer.MAX_VALUE);
		}
		
		if (!cyberware.hasEssential(EnumSlot.LEG, EnumSide.LEFT))
		{
			numMissingLegs++;
			numMissingLegsVisible++;
		}
		if (!cyberware.hasEssential(EnumSlot.LEG, EnumSide.RIGHT))
		{
			numMissingLegs++;
			numMissingLegsVisible++;

		}
		
		ItemStack legLeft = cyberware.getLimb(EnumSlot.LEG, EnumSide.LEFT);
		if (!legLeft.isEmpty() && !((ILimbReplacement)legLeft.getItem()).isLimbActive(legLeft))
		{
			numMissingLegs++;
		}
		
		ItemStack legRight = cyberware.getLimb(EnumSlot.LEG, EnumSide.RIGHT);
		if (!legRight.isEmpty() && !((ILimbReplacement)legRight.getItem()).isLimbActive(legRight))
		{
			numMissingLegs++;
		}
		

		if (e instanceof EntityPlayer)
		{
	
			
			if (numMissingLegsVisible == 2)
			{
				e.height = 1.8F - (10F / 16F);
				((EntityPlayer) e).eyeHeight = ((EntityPlayer) e).getDefaultEyeHeight() - (10F / 16F);
				AxisAlignedBB axisalignedbb = e.getEntityBoundingBox();
				e.setEntityBoundingBox(new AxisAlignedBB(axisalignedbb.minX, axisalignedbb.minY, axisalignedbb.minZ, axisalignedbb.minX + (double)e.width, axisalignedbb.minY + (double)e.height, axisalignedbb.minZ + (double)e.width));
				
				if (e.world.isRemote)
				{
					lastClient.put(e.getEntityId(), true);
				}
				else
				{
					last.put(e.getEntityId(), true);
				}

			}
			else if (last(e.world.isRemote, e))
			{
				e.height = 1.8F;
				((EntityPlayer) e).eyeHeight = ((EntityPlayer) e).getDefaultEyeHeight();
				AxisAlignedBB axisalignedbb = e.getEntityBoundingBox();
				e.setEntityBoundingBox(new AxisAlignedBB(axisalignedbb.minX, axisalignedbb.minY, axisalignedbb.minZ, axisalignedbb.minX + (double)e.width, axisalignedbb.minY + (double)e.height, axisalignedbb.minZ + (double)e.width));
				
				if (e.world.isRemote)
				{
					lastClient.put(e.getEntityId(), false);
				}
				else
				{
					last.put(e.getEntityId(), false);
				}
			}
		}
		
		if (numMissingLegs >= 1 && e.onGround)
		{

			HashMultimap<String, AttributeModifier> multimap = HashMultimap.<String, AttributeModifier>create();
			
			multimap.put(SharedMonsterAttributes.MOVEMENT_SPEED.getName(), new AttributeModifier(speedId, "Missing leg speed", -100F, 0));
			e.getAttributeMap().applyAttributeModifiers(multimap);

			//e.moveEntity(e.lastTickPosX - e.posX, 0, e.lastTickPosZ - e.posZ);
		}
		else
		{
			HashMultimap<String, AttributeModifier> multimap = HashMultimap.<String, AttributeModifier>create();
			
			multimap.put(SharedMonsterAttributes.MOVEMENT_SPEED.getName(), new AttributeModifier(speedId, "Missing leg speed", -100F, 0));
			e.getAttributeMap().removeAttributeModifiers(multimap);
		}
		
		

		if (!cyberware.hasEssential(EnumSlot.HEART))
		{
			e.attackEntityFrom(heartless, Integer.MAX_VALUE);
		}
		
		if (!cyberware.hasEssential(EnumSlot.BONE))
		{
			
			e.attackEntityFrom(spineless, Integer.MAX_VALUE);
		}
		
		if (!cyberware.hasEssential(EnumSlot.MUSCLE))
		{
			e.attackEntityFrom(nomuscles, Integer.MAX_VALUE);
		}
		
		
		if (!cyberware.hasEssential(EnumSlot.LUNGS))
		{
			if (getLungsTime(e) >= 20)
			{
				timesLungs.put(e.getEntityId(), e.ticksExisted);
				e.attackEntityFrom(DamageSource.DROWN, 2F);
			}
		}
		else
		{
			timesLungs.remove(e.getEntityId());
		}
	}
	
	private boolean last(boolean remote, EntityLivingBase e)
	{
		if (remote)
		{
			if (!lastClient.containsKey(e.getEntityId()))
			{
				lastClient.put(e.getEntityId(), false);
			}
			return lastClient.get(e.getEntityId());
		}
		else
		{
			if (!last.containsKey(e.getEntityId()))
			{
				last.put(e.getEntityId(), false);
			}
			return last.get(e.getEntityId());
		}
	}
	

	@SubscribeEvent
	public void handleJump(LivingJumpEvent event)
	{
	    EntityLivingBase e = event.getEntityLiving();

        if (e == null)
            return;

		if (CyberwareAPI.hasCapability(e))
		{
			ICyberwareUserData cyberware = CyberwareAPI.getCapability(e);

			int numMissingLegs = 0;
			
			if (!cyberware.hasEssential(EnumSlot.LEG, EnumSide.LEFT))
			{
				numMissingLegs++;
			}
			if (!cyberware.hasEssential(EnumSlot.LEG, EnumSide.RIGHT))
			{
				numMissingLegs++;
			}
			
			ItemStack legLeft = cyberware.getCyberware(new ItemStack(CyberwareContent.cyberlimbs, 1, 2));
			if (!legLeft.isEmpty() && !ItemCyberlimb.isPowered(legLeft))
			{
				numMissingLegs++;
			}
			
			ItemStack legRight = cyberware.getCyberware(new ItemStack(CyberwareContent.cyberlimbs, 1, 3));
			if (!legRight.isEmpty() && !ItemCyberlimb.isPowered(legRight))
			{
				numMissingLegs++;
			}
			
			if (numMissingLegs == 2)
			{
				e.motionY = 0.2F;
			}
		}
	}
		
	private int getLungsTime(EntityLivingBase e)
	{
		if (!timesLungs.containsKey(e.getEntityId()))
		{
			timesLungs.put(e.getEntityId(), e.ticksExisted);
		}
		return e.ticksExisted - timesLungs.get(e.getEntityId());
	}
	
	private static Map<Integer, Integer> hunger = new HashMap<Integer, Integer>();
	private static Map<Integer, Float> saturation = new HashMap<Integer, Float>();

	@SubscribeEvent
	public void handleEatFoodTick(LivingEntityUseItemEvent.Tick event)
	{
		EntityLivingBase e = event.getEntityLiving();
		ItemStack stack = event.getItem();

        if (e == null)
            return;

		if (e instanceof EntityPlayer && CyberwareAPI.hasCapability(e) && !stack.isEmpty() && stack.getItem().getItemUseAction(stack) == EnumAction.EAT)
		{
			EntityPlayer p = (EntityPlayer) e;
			ICyberwareUserData cyberware = CyberwareAPI.getCapability(e);
			
			if (!cyberware.hasEssential(EnumSlot.LOWER_ORGANS))
			{
				hunger.put(p.getEntityId(), p.getFoodStats().getFoodLevel());
				saturation.put(p.getEntityId(), p.getFoodStats().getSaturationLevel());
			}
		}
		else
		{
			hunger.remove(e.getEntityId());
			saturation.remove(e.getEntityId());
		}
	}
	
	@SubscribeEvent
	public void handleEatFoodEnd(LivingEntityUseItemEvent.Finish event)
	{
		EntityLivingBase e = event.getEntityLiving();
		ItemStack stack = event.getItem();

        if (e == null)
            return;

		if (e instanceof EntityPlayer && CyberwareAPI.hasCapability(e) && !stack.isEmpty() && stack.getItem().getItemUseAction(stack) == EnumAction.EAT)
		{
			EntityPlayer p = (EntityPlayer) e;
			ICyberwareUserData cyberware = CyberwareAPI.getCapability(e);
			
			if (!cyberware.hasEssential(EnumSlot.LOWER_ORGANS))
			{
				int hungerVal = hunger.keySet().contains(p) ? hunger.get(p) : p.getFoodStats().getFoodLevel();
				float satVal = saturation.keySet().contains(p) ? saturation.get(p) : p.getFoodStats().getSaturationLevel();

				p.getFoodStats().setFoodLevel(hungerVal);
				p.getFoodStats().setFoodSaturationLevel(satVal);
			}
		}
	}
	
	public static final ResourceLocation BLACK_PX = new ResourceLocation(Cyberware.MODID + ":textures/gui/blackpx.png");
	
	private boolean removedMove = false;

	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void overlayPre(ClientTickEvent event)
	{
		if (event.phase == Phase.START && Minecraft.getMinecraft() != null && Minecraft.getMinecraft().player != null)
		{
			EntityPlayer e = Minecraft.getMinecraft().player;

			HashMultimap<String, AttributeModifier> multimap = HashMultimap.<String, AttributeModifier>create();
			multimap.put(SharedMonsterAttributes.MOVEMENT_SPEED.getName(), new AttributeModifier(speedId, "Missing leg speed", -100F, 0));
			e.getAttributeMap().removeAttributeModifiers(multimap);
		}
	}

	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void overlayPre(RenderGameOverlayEvent.Pre event)
	{
		if (event.getType() == ElementType.ALL)
		{
			EntityPlayer e = Minecraft.getMinecraft().player;

            if (e == null)
                return;

			HashMultimap<String, AttributeModifier> multimap = HashMultimap.<String, AttributeModifier>create();
			multimap.put(SharedMonsterAttributes.MOVEMENT_SPEED.getName(), new AttributeModifier(speedId, "Missing leg speed", -100F, 0));
			//e.getAttributeMap().removeAttributeModifiers(multimap);
			
			
			if (CyberwareAPI.hasCapability(e))
			{
				ICyberwareUserData cyberware = CyberwareAPI.getCapability(e);
				
				if (!cyberware.hasEssential(EnumSlot.EYES) && !e.isCreative())
				{
					GlStateManager.pushMatrix();
					GlStateManager.enableBlend();
					GlStateManager.color(1F, 1F, 1F, .9F);
					Minecraft.getMinecraft().getTextureManager().bindTexture(BLACK_PX);
					ClientUtils.drawTexturedModalRect(0, 0, 0, 0, Minecraft.getMinecraft().displayWidth, Minecraft.getMinecraft().displayHeight);
					GlStateManager.popMatrix();
				}
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
		EntityLivingBase e = event.getEntityLiving();

        if (e == null)
            return;

		if (CyberwareAPI.hasCapability(e))
		{
			ICyberwareUserData cyberware = CyberwareAPI.getCapability(e);
			
			if (!cyberware.hasEssential(EnumSlot.SKIN))
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
		EntityLivingBase e = event.getEntityLiving();

        if (e == null)
            return;

		if (CyberwareAPI.hasCapability(e))
		{
			ICyberwareUserData cyberware = CyberwareAPI.getCapability(e);
			processEvent(event, event.getHand(), event.getEntityPlayer(), cyberware, 2);
		}
	}
	
	@SubscribeEvent
	public void handleLeftClickBlock(PlayerInteractEvent.LeftClickBlock event)
	{
		EntityLivingBase e = event.getEntityLiving();

        if (e == null)
            return;

		if (CyberwareAPI.hasCapability(e))
		{
			ICyberwareUserData cyberware = CyberwareAPI.getCapability(e);
			processEvent(event, event.getHand(), event.getEntityPlayer(), cyberware, 2);
		}
	}
	
	@SubscribeEvent
	public void handleLeftClickEntity(AttackEntityEvent event)
	{
		EntityLivingBase e = event.getEntityLiving();
		
		if (CyberwareAPI.hasCapability(e))
		{
			ICyberwareUserData cyberware = CyberwareAPI.getCapability(e);
			processEvent(event, EnumHand.MAIN_HAND, event.getEntityPlayer(), cyberware, 2);
		}
	}
	
	@SubscribeEvent
	public void handleRightClickBlock(PlayerInteractEvent.RightClickBlock event)
	{
		EntityLivingBase e = event.getEntityLiving();

        if (e == null)
            return;

		if (CyberwareAPI.hasCapability(e))
		{
			ICyberwareUserData cyberware = CyberwareAPI.getCapability(e);
			processEvent(event, event.getHand(), event.getEntityPlayer(), cyberware, 2);
		}
	}
	
	@SubscribeEvent
	public void handleRightClickItem(PlayerInteractEvent.RightClickItem event)
	{
		EntityLivingBase e = event.getEntityLiving();

        if (e == null)
            return;

		if (CyberwareAPI.hasCapability(e))
		{
			ICyberwareUserData cyberware = CyberwareAPI.getCapability(e);
			
			if (!canUseItem(event.getHand(), event.getEntityPlayer(), cyberware))
			{
				event.setCanceled(true);
			}
			
			processEvent(event, event.getHand(), event.getEntityPlayer(), cyberware, 1);
		}
	}
	
	private void processEvent(Event event, EnumHand hand, EntityPlayer p, ICyberwareUserData cyberware, int check)
	{
		EnumHandSide mainHand = p.getPrimaryHand();
		EnumHandSide offHand = ((mainHand == EnumHandSide.LEFT) ? EnumHandSide.RIGHT : EnumHandSide.LEFT);
		EnumSide correspondingMainHand = ((mainHand == EnumHandSide.RIGHT) ? EnumSide.RIGHT : EnumSide.LEFT);
		EnumSide correspondingOffHand = ((offHand == EnumHandSide.RIGHT) ? EnumSide.RIGHT : EnumSide.LEFT);
		
		boolean leftInactive = false;
		boolean rightInactive = false;
		
		ItemStack armLeft = cyberware.getLimb(EnumSlot.ARM, EnumSide.LEFT);
		ItemStack armRight = cyberware.getLimb(EnumSlot.ARM, EnumSide.RIGHT);
		if (check == 0)
		{
			leftInactive = !armLeft.isEmpty() && !((ILimbReplacement) CyberwareAPI.getCyberware(armLeft)).isLimbActive(armLeft);
			rightInactive = !armRight.isEmpty() && !((ILimbReplacement) CyberwareAPI.getCyberware(armRight)).isLimbActive(armRight);
		}
		else if (check == 1)
		{
			leftInactive = !armLeft.isEmpty() && !((ILimbReplacement) CyberwareAPI.getCyberware(armLeft)).canInteract(armLeft);
			rightInactive = !armRight.isEmpty() && !((ILimbReplacement) CyberwareAPI.getCyberware(armRight)).canInteract(armRight);
		}
		else if (check == 2)
		{
			leftInactive = !armLeft.isEmpty() && !((ILimbReplacement) CyberwareAPI.getCyberware(armLeft)).canMine(armLeft);
			rightInactive = !armRight.isEmpty() && !((ILimbReplacement) CyberwareAPI.getCyberware(armRight)).canMine(armRight);
		}
		
		boolean mainHandUnpowered = (correspondingMainHand == EnumSide.RIGHT) ?  rightInactive : leftInactive;
		boolean offHandUnpowered = (correspondingMainHand == EnumSide.RIGHT) ?  leftInactive : rightInactive;
		
		if (hand == EnumHand.MAIN_HAND && (!cyberware.hasEssential(EnumSlot.ARM, correspondingMainHand) || mainHandUnpowered))
		{
			event.setCanceled(true);
		}
		else if (hand == EnumHand.OFF_HAND && (!cyberware.hasEssential(EnumSlot.ARM, correspondingOffHand) || offHandUnpowered))
		{
			event.setCanceled(true);
		}
	}
	
	private boolean canUseItem(EnumHand hand, EntityPlayer p, ICyberwareUserData cyberware)
	{
		EnumHandSide mainHand = p.getPrimaryHand();
		EnumHandSide offHand = ((mainHand == EnumHandSide.LEFT) ? EnumHandSide.RIGHT : EnumHandSide.LEFT);
		EnumSide correspondingMainHand = ((mainHand == EnumHandSide.RIGHT) ? EnumSide.RIGHT : EnumSide.LEFT);
		EnumSide correspondingOffHand = ((offHand == EnumHandSide.RIGHT) ? EnumSide.RIGHT : EnumSide.LEFT);
		
		boolean leftInactive = false;
		boolean rightInactive = false;
		
		ItemStack armLeft = cyberware.getLimb(EnumSlot.ARM, EnumSide.LEFT);
		ItemStack armRight = cyberware.getLimb(EnumSlot.ARM, EnumSide.RIGHT);
		leftInactive = !armLeft.isEmpty() && !((ILimbReplacement) CyberwareAPI.getCyberware(armLeft)).canHoldItem(armLeft);
		rightInactive = !armRight.isEmpty() && !((ILimbReplacement) CyberwareAPI.getCyberware(armRight)).canHoldItem(armRight);
		
		boolean mainHandUnpowered = (correspondingMainHand == EnumSide.RIGHT) ?  rightInactive : leftInactive;
		boolean offHandUnpowered = (correspondingMainHand == EnumSide.RIGHT) ?  leftInactive : rightInactive;
		
		if (hand == EnumHand.MAIN_HAND && (!cyberware.hasEssential(EnumSlot.ARM, correspondingMainHand) || mainHandUnpowered))
		{
			return false;
		}
		else if (hand == EnumHand.OFF_HAND && (!cyberware.hasEssential(EnumSlot.ARM, correspondingOffHand) || offHandUnpowered))
		{
			return false;
		}
		return true;
	}
}
