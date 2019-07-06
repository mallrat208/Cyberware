package flaxbeard.cyberware.common.item;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.common.ISpecialArmor;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.EnderTeleportEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.BreakSpeed;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.api.CyberwareUpdateEvent;
import flaxbeard.cyberware.api.item.EnableDisableHelper;
import flaxbeard.cyberware.api.item.IMenuItem;
import flaxbeard.cyberware.common.CyberwareContent;
import flaxbeard.cyberware.common.lib.LibConstants;
import flaxbeard.cyberware.common.network.CyberwarePacketHandler;
import flaxbeard.cyberware.common.network.DodgePacket;

public class ItemBrainUpgrade extends ItemCyberware implements IMenuItem
{
    
    public static final int META_CORTICAL_STACK             = 0;
    public static final int META_ENDER_JAMMER               = 1;
    public static final int META_CONSCIOUSNESS_TRANSMITTER  = 2;
    public static final int META_NEURAL_CONTEXTUALIZER      = 3;
    public static final int META_THREAT_MATRIX              = 4;
    public static final int META_RADIO                      = 5;
    
    public ItemBrainUpgrade(String name, EnumSlot slot, String[] subnames)
    {
        super(name, slot, subnames);
        MinecraftForge.EVENT_BUS.register(this);
    }
    
    @Override
    public boolean isIncompatible(ItemStack stack, ItemStack other)
    {
        return other.getItem() == this
            && stack.getItemDamage() == META_CORTICAL_STACK
            && other.getItemDamage() == META_CONSCIOUSNESS_TRANSMITTER;
    }

    @SubscribeEvent
    public void handleTeleJam(EnderTeleportEvent event)
    {
        EntityLivingBase entityLivingBase = event.getEntityLiving();
        ItemStack itemStackJammer = new ItemStack(this, 1, META_ENDER_JAMMER);

        if ( CyberwareAPI.isCyberwareInstalled(entityLivingBase, itemStackJammer)
          && EnableDisableHelper.isEnabled(CyberwareAPI.getCyberware(entityLivingBase, itemStackJammer)) )
        {
            event.setCanceled(true);
            return;
        }
        if (entityLivingBase != null)
        {
            float range = 25F;
            List<EntityLivingBase> entitiesInRange = entityLivingBase.world.getEntitiesWithinAABB(EntityLivingBase.class,
                                                                                      new AxisAlignedBB(entityLivingBase.posX - range, entityLivingBase.posY - range, entityLivingBase.posZ - range,
                                                                                                        entityLivingBase.posX + entityLivingBase.width + range, entityLivingBase.posY + entityLivingBase.height + range, entityLivingBase.posZ + entityLivingBase.width + range));
            for (EntityLivingBase entityInRange : entitiesInRange)
            {
                if (entityLivingBase.getDistance(entityInRange) <= range)
                {
                    if ( CyberwareAPI.isCyberwareInstalled(entityInRange, itemStackJammer)
                      && EnableDisableHelper.isEnabled(CyberwareAPI.getCyberware(entityInRange, itemStackJammer)) )
                    {
                        event.setCanceled(true);
                        return;
                    }
                }
            }
        }

    }

    @SubscribeEvent
    public void handleClone(PlayerEvent.Clone event)
    {
        if (event.isWasDeath())
        {
            EntityPlayer entityPlayerOriginal = event.getOriginal();
            
            if (entityPlayerOriginal.world.getGameRules().getBoolean("keepInventory")) {
                return;
            }
            
            if (CyberwareAPI.isCyberwareInstalled(entityPlayerOriginal, new ItemStack(this, 1, META_CORTICAL_STACK)))
            {
				/*
				float range = 5F;
				List<EntityXPOrb> orbs = entityPlayerOriginal.world.getEntitiesWithinAABB(EntityXPOrb.class, new AxisAlignedBB(entityPlayerOriginal.posX - range, entityPlayerOriginal.posY - range, entityPlayerOriginal.posZ - range, entityPlayerOriginal.posX + entityPlayerOriginal.width + range, entityPlayerOriginal.posY + entityPlayerOriginal.height + range, entityPlayerOriginal.posZ + entityPlayerOriginal.width + range));
				for (EntityXPOrb orb : orbs)
				{
					orb.setDead();
				}
				*/

                if (!entityPlayerOriginal.world.isRemote)
                {
                    ItemStack stack = new ItemStack(CyberwareContent.expCapsule);
                    NBTTagCompound tagCompound = new NBTTagCompound();
                    tagCompound.setInteger("xp", entityPlayerOriginal.experienceTotal);
                    stack.setTagCompound(tagCompound);
                    EntityItem item = new EntityItem(entityPlayerOriginal.world, entityPlayerOriginal.posX, entityPlayerOriginal.posY, entityPlayerOriginal.posZ, stack);
                    entityPlayerOriginal.world.spawnEntity(item);
                }
            }
            else if (CyberwareAPI.isCyberwareInstalled(entityPlayerOriginal, new ItemStack(this, 1, META_CONSCIOUSNESS_TRANSMITTER)))
            {
                event.getEntityPlayer().addExperience((int) (Math.min(100, entityPlayerOriginal.experienceLevel * 7) * .9F));
            }
        }
    }

    @SubscribeEvent
    public void handleMining(BreakSpeed event)
    {
        EntityPlayer entityPlayer = event.getEntityPlayer();

        ItemStack test = new ItemStack(this, 1, META_NEURAL_CONTEXTUALIZER);
        if ( CyberwareAPI.isCyberwareInstalled(entityPlayer, test)
          && EnableDisableHelper.isEnabled(CyberwareAPI.getCyberware(entityPlayer, test))
          && isContextWorking(entityPlayer)
          && !entityPlayer.isSneaking() )
        {
            IBlockState state = event.getState();
            ItemStack tool = entityPlayer.getHeldItem(EnumHand.MAIN_HAND);

            if (!tool.isEmpty() && (tool.getItem() instanceof ItemSword || tool.getItem().getTranslationKey().contains("sword"))) return;

            if (isToolEffective(tool, state)) return;

            for (int i = 0; i < 10; i++)
            {
                if (i != entityPlayer.inventory.currentItem)
                {
                    ItemStack potentialTool = entityPlayer.inventory.mainInventory.get(i);
                    if (isToolEffective(potentialTool, state))
                    {
                        entityPlayer.inventory.currentItem = i;
                        return;
                    }
                }
            }
        }
    }

    private static Map<UUID, Boolean> isContextWorking = new HashMap<>();
    private static Map<UUID, Boolean> isMatrixWorking = new HashMap<>();
    private static Map<UUID, Boolean> isRadioWorking = new HashMap<>();

    @SubscribeEvent(priority=EventPriority.NORMAL)
    public void handleLivingUpdate(CyberwareUpdateEvent event)
    {
        EntityLivingBase entityLivingBase = event.getEntityLiving();

        ItemStack test = new ItemStack(this, 1, META_NEURAL_CONTEXTUALIZER);
        if ( entityLivingBase.ticksExisted % 20 == 0
          && CyberwareAPI.isCyberwareInstalled(entityLivingBase, test)
          && EnableDisableHelper.isEnabled(CyberwareAPI.getCyberware(entityLivingBase, test)) )
        {
            isContextWorking.put(entityLivingBase.getUniqueID(), CyberwareAPI.getCapability(entityLivingBase).usePower(test, getPowerConsumption(test)));
        }

        test = new ItemStack(this, 1, META_THREAT_MATRIX);
        if ( entityLivingBase.ticksExisted % 20 == 0
          && CyberwareAPI.isCyberwareInstalled(entityLivingBase, test) )
        {
            isMatrixWorking.put(entityLivingBase.getUniqueID(), CyberwareAPI.getCapability(entityLivingBase).usePower(test, getPowerConsumption(test)));
        }

        test = new ItemStack(this, 1, META_RADIO);
        if ( entityLivingBase.ticksExisted % 20 == 0
          && CyberwareAPI.isCyberwareInstalled(entityLivingBase, test)
          && EnableDisableHelper.isEnabled(CyberwareAPI.getCyberware(entityLivingBase, test)) )
        {
            isRadioWorking.put(entityLivingBase.getUniqueID(), CyberwareAPI.getCapability(entityLivingBase).usePower(test, getPowerConsumption(test)));
        }
    }

    public static boolean isRadioWorking(EntityLivingBase entityLivingBase)
    {
        if (!isRadioWorking.containsKey(entityLivingBase.getUniqueID()))
        {
            isRadioWorking.put(entityLivingBase.getUniqueID(), Boolean.FALSE);
        }

        return isRadioWorking.get(entityLivingBase.getUniqueID());
    }

    private boolean isContextWorking(EntityLivingBase entityLivingBase)
    {
        if (!isContextWorking.containsKey(entityLivingBase.getUniqueID()))
        {
            isContextWorking.put(entityLivingBase.getUniqueID(), Boolean.FALSE);
        }

        return isContextWorking.get(entityLivingBase.getUniqueID());
    }

    private boolean isMatrixWorking(EntityLivingBase entityLivingBase)
    {
        if (!isMatrixWorking.containsKey(entityLivingBase.getUniqueID()))
        {
            isMatrixWorking.put(entityLivingBase.getUniqueID(), Boolean.FALSE);
        }

        return isMatrixWorking.get(entityLivingBase.getUniqueID());
    }

    public boolean isToolEffective(ItemStack tool, IBlockState state)
    {
        if (!tool.isEmpty())
        {
            for (String toolType : tool.getItem().getToolClasses(tool))
            {
                if (state.getBlock().isToolEffective(toolType, state))
                {
                    return true;
                }
            }
        }
        return false;
    }
    
    @SubscribeEvent
    public void handleXPDrop(LivingExperienceDropEvent event)
    {
        EntityLivingBase entityLivingBase = event.getEntityLiving();
        if ( CyberwareAPI.isCyberwareInstalled(entityLivingBase, new ItemStack(this, 1, META_CORTICAL_STACK))
          || CyberwareAPI.isCyberwareInstalled(entityLivingBase, new ItemStack(this, 1, META_CONSCIOUSNESS_TRANSMITTER)) )
        {
            event.setCanceled(true);
        }
    }

    private static ArrayList<String> lastHits = new ArrayList<>();

    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public void handleHurt(LivingAttackEvent event)
    {
        EntityLivingBase entityLivingBase = event.getEntityLiving();

        if ( CyberwareAPI.isCyberwareInstalled(entityLivingBase, new ItemStack(this, 1, META_THREAT_MATRIX))
          && isMatrixWorking(entityLivingBase) )
        {
            if ( !entityLivingBase.world.isRemote
              && event.getSource() instanceof EntityDamageSource )
            {
                Entity attacker = event.getSource().getTrueSource();
                if (entityLivingBase instanceof EntityPlayer)
                {
                    String str = entityLivingBase.getEntityId() + " " + entityLivingBase.ticksExisted + " " + attacker.getEntityId();
                    if (lastHits.contains(str))
                    {
                        return;
                    }
                    else
                    {
                        lastHits.add(str);
                    }
                }

                boolean armor = false;
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

                    if (!stack.isEmpty())
                    {
                        armor = true;
                    }

                }

                if (!((float) entityLivingBase.hurtResistantTime > (float) entityLivingBase.maxHurtResistantTime / 2.0F))
                {
                    Random random = entityLivingBase.getRNG();
                    if (random.nextFloat() < (armor ? LibConstants.DODGE_ARMOR : LibConstants.DODGE_NO_ARMOR))
                    {
                        event.setCanceled(true);
                        entityLivingBase.hurtResistantTime = entityLivingBase.maxHurtResistantTime;
                        entityLivingBase.hurtTime = entityLivingBase.maxHurtTime = 10;
                        ReflectionHelper.setPrivateValue(EntityLivingBase.class, entityLivingBase, 9999F, "lastDamage", "field_110153_bc");
                        CyberwarePacketHandler.INSTANCE.sendToAllAround(new DodgePacket(entityLivingBase.getEntityId()),
                                                                        new TargetPoint(entityLivingBase.world.provider.getDimension(), entityLivingBase.posX, entityLivingBase.posY, entityLivingBase.posZ, 50));
                    }
                }
            }
        }
    }

    @Override
    public int getPowerConsumption(ItemStack stack)
    {
        return stack.getItemDamage() == META_NEURAL_CONTEXTUALIZER ? LibConstants.CONTEXTUALIZER_CONSUMPTION
             : stack.getItemDamage() == META_THREAT_MATRIX ? LibConstants.MATRIX_CONSUMPTION
             : stack.getItemDamage() == META_RADIO ? LibConstants.RADIO_CONSUMPTION
             : 0;
    }
    
    @Override
    public boolean hasMenu(ItemStack stack)
    {
        return stack.getItemDamage() == META_ENDER_JAMMER
            || stack.getItemDamage() == META_NEURAL_CONTEXTUALIZER
            || stack.getItemDamage() == META_RADIO;
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
}
