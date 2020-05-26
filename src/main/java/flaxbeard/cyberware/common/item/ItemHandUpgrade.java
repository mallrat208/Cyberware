package flaxbeard.cyberware.common.item;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import flaxbeard.cyberware.api.ICyberwareUserData;
import flaxbeard.cyberware.common.CyberwareConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.NonNullList;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent.BreakSpeed;
import net.minecraftforge.event.entity.player.PlayerEvent.HarvestCheck;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

import com.google.common.collect.HashMultimap;

import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.api.CyberwareUpdateEvent;
import flaxbeard.cyberware.api.item.EnableDisableHelper;
import flaxbeard.cyberware.api.item.IMenuItem;
import flaxbeard.cyberware.common.CyberwareContent;
import flaxbeard.cyberware.common.misc.NNLUtil;

public class ItemHandUpgrade extends ItemCyberware implements IMenuItem
{

    public static final int META_CRAFT_HANDS                = 0;
    public static final int META_CLAWS                      = 1;
    public static final int META_MINING                     = 2;
    
    private final Item tool_level;
    
    private static final UUID uuidClawsStrengthAttribute = UUID.fromString("63c32801-94fb-40d4-8bd2-89135c1e44b1");
    private static final HashMultimap<String, AttributeModifier> multimapClawsStrengthAttribute;
    private static final Map<UUID, Boolean> lastClaws = new HashMap<>();
    public static float clawsTime;
    
    static {
        multimapClawsStrengthAttribute = HashMultimap.create();
        multimapClawsStrengthAttribute.put(SharedMonsterAttributes.ATTACK_SPEED.getName(),
                                           new AttributeModifier(uuidClawsStrengthAttribute, "Claws damage upgrade", 5.5F, 0) );
    }
    
    public ItemHandUpgrade(String name, EnumSlot slot, String[] subnames)
    {
        super(name, slot, subnames);
        MinecraftForge.EVENT_BUS.register(this);
        this.tool_level = CyberwareConfig.FIST_MINING_LEVEL == 3
                        ? Items.DIAMOND_PICKAXE
                        : CyberwareConfig.FIST_MINING_LEVEL == 2
                        ? Items.IRON_PICKAXE
                        : Items.STONE_PICKAXE;
    }

    @Override
    public NonNullList<NonNullList<ItemStack>> required(ItemStack stack)
    {
        return NNLUtil.fromArray(new ItemStack[][] {
                new ItemStack[] { CyberwareContent.cyberlimbs.getCachedStack(ItemCyberlimb.META_LEFT_CYBER_ARM),
                                  CyberwareContent.cyberlimbs.getCachedStack(ItemCyberlimb.META_RIGHT_CYBER_ARM) }});
    }

    @Override
    public boolean isIncompatible(ItemStack stack, ItemStack other)
    {
        return other.getItem() == this;
    }

    @SubscribeEvent(priority=EventPriority.NORMAL)
    public void handleLivingUpdate(CyberwareUpdateEvent event)
    {
        EntityLivingBase entityLivingBase = event.getEntityLiving();
        ICyberwareUserData cyberwareUserData = event.getCyberwareUserData();
        
        ItemStack itemStackClaws = cyberwareUserData.getCyberware(getCachedStack(META_CLAWS));
        if (!itemStackClaws.isEmpty())
        {
            boolean wasEquipped = getLastClaws(entityLivingBase);
            boolean isEquipped = entityLivingBase.getHeldItemMainhand().isEmpty()
                 && ( entityLivingBase.getPrimaryHand() == EnumHandSide.RIGHT
                    ? (cyberwareUserData.isCyberwareInstalled(CyberwareContent.cyberlimbs.getCachedStack(ItemCyberlimb.META_RIGHT_CYBER_ARM)))
                    : (cyberwareUserData.isCyberwareInstalled(CyberwareContent.cyberlimbs.getCachedStack(ItemCyberlimb.META_LEFT_CYBER_ARM))) )
                 && EnableDisableHelper.isEnabled(itemStackClaws);
            if (isEquipped)
            {
                addUnarmedDamage(entityLivingBase, itemStackClaws);
                lastClaws.put(entityLivingBase.getUniqueID(), Boolean.TRUE);
                
                if ( !wasEquipped
                  && entityLivingBase.getEntityWorld().isRemote )
                {
                    updateHand(entityLivingBase, true);
                }
            }
            else if (wasEquipped)
            {
                removeUnarmedDamage(entityLivingBase, itemStackClaws);
                lastClaws.put(entityLivingBase.getUniqueID(), Boolean.FALSE);
            }
        }
        else if (entityLivingBase.ticksExisted % 20 == 0)
        {
            removeUnarmedDamage(entityLivingBase, itemStackClaws);
            lastClaws.put(entityLivingBase.getUniqueID(), Boolean.FALSE);
        }
    }

    private void updateHand(EntityLivingBase entityLivingBase, boolean delay)
    {
        if ( Minecraft.getMinecraft() != null
          && Minecraft.getMinecraft().player != null
          && entityLivingBase == Minecraft.getMinecraft().player )
        {
            clawsTime = Minecraft.getMinecraft().getRenderPartialTicks() + entityLivingBase.ticksExisted + (delay ? 5 : 0);
        }
    }

    private boolean getLastClaws(EntityLivingBase entityLivingBase)
    {
        if (!lastClaws.containsKey(entityLivingBase.getUniqueID()))
        {
            lastClaws.put(entityLivingBase.getUniqueID(), Boolean.FALSE);
        }
        return lastClaws.get(entityLivingBase.getUniqueID());
    }
    
    private void addUnarmedDamage(EntityLivingBase entityLivingBase, ItemStack stack)
    {
        if (stack.getItemDamage() == META_CLAWS)
        {
            entityLivingBase.getAttributeMap().applyAttributeModifiers(multimapClawsStrengthAttribute);
        }
    }
    
    private void removeUnarmedDamage(EntityLivingBase entityLivingBase, ItemStack stack)
    {
        if (stack.getItemDamage() == META_CLAWS)
        {
            entityLivingBase.getAttributeMap().removeAttributeModifiers(multimapClawsStrengthAttribute);
        }
    }
    
    @Override
    public void onRemoved(EntityLivingBase entityLivingBase, ItemStack stack)
    {
        if (stack.getItemDamage() == META_CLAWS)
        {
            removeUnarmedDamage(entityLivingBase, stack);
        }
    }

    @SubscribeEvent
    public void handleMining(HarvestCheck event)
    {
        EntityPlayer entityPlayer = event.getEntityPlayer();
        ICyberwareUserData cyberwareUserData = CyberwareAPI.getCapabilityOrNull(entityPlayer);
        if (cyberwareUserData == null) return;
        
        ItemStack itemStackMining = cyberwareUserData.getCyberware(getCachedStack(META_MINING));
        boolean rightArm = ( entityPlayer.getPrimaryHand() == EnumHandSide.RIGHT
                           ? (cyberwareUserData.isCyberwareInstalled(CyberwareContent.cyberlimbs.getCachedStack(ItemCyberlimb.META_RIGHT_CYBER_ARM)))
                           : (cyberwareUserData.isCyberwareInstalled(CyberwareContent.cyberlimbs.getCachedStack(ItemCyberlimb.META_LEFT_CYBER_ARM))) );
        if ( rightArm
          && !itemStackMining.isEmpty()
          && entityPlayer.getHeldItemMainhand().isEmpty() )
        {
            ItemStack pick = new ItemStack(tool_level);
            if (pick.canHarvestBlock(event.getTargetBlock()))
            {
                event.setCanHarvest(true);
            }
        }
    }

    @SubscribeEvent
    public void handleMineSpeed(BreakSpeed event)
    {
        EntityPlayer entityPlayer = event.getEntityPlayer();
        ICyberwareUserData cyberwareUserData = CyberwareAPI.getCapabilityOrNull(entityPlayer);
        if (cyberwareUserData == null) return;
    
        ItemStack itemStackMining = cyberwareUserData.getCyberware(getCachedStack(META_MINING));
        boolean rightArm = ( entityPlayer.getPrimaryHand() == EnumHandSide.RIGHT
                           ? (cyberwareUserData.isCyberwareInstalled(CyberwareContent.cyberlimbs.getCachedStack(ItemCyberlimb.META_RIGHT_CYBER_ARM)))
                           : (cyberwareUserData.isCyberwareInstalled(CyberwareContent.cyberlimbs.getCachedStack(ItemCyberlimb.META_LEFT_CYBER_ARM))) );
        if ( rightArm
          && !itemStackMining.isEmpty()
          && entityPlayer.getHeldItemMainhand().isEmpty() )
        {
            ItemStack pick = new ItemStack(tool_level);
            event.setNewSpeed(event.getNewSpeed() * pick.getDestroySpeed(entityPlayer.world.getBlockState(event.getPos())));
        }
    }

    @Override
    public boolean hasMenu(ItemStack stack)
    {
        return stack.getItemDamage() == META_CLAWS;
    }

    @Override
    public void use(Entity entity, ItemStack stack)
    {
        EnableDisableHelper.toggle(stack);
        if (entity instanceof EntityLivingBase && FMLCommonHandler.instance().getSide() == Side.CLIENT)
        {
            updateHand((EntityLivingBase) entity, false);
        }
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
