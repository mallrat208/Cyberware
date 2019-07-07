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
                new ItemStack[] { new ItemStack(CyberwareContent.cyberlimbs, 1, ItemCyberlimb.META_LEFT_CYBER_ARM),
                                  new ItemStack(CyberwareContent.cyberlimbs, 1, ItemCyberlimb.META_RIGHT_CYBER_ARM) }});
    }

    @Override
    public boolean isIncompatible(ItemStack stack, ItemStack other)
    {
        return other.getItem() == this;
    }

    private Map<UUID, Boolean> lastClaws = new HashMap<>();
    public static float clawsTime;

    @SubscribeEvent(priority=EventPriority.NORMAL)
    public void handleLivingUpdate(CyberwareUpdateEvent event)
    {
        EntityLivingBase entityLivingBase = event.getEntityLiving();
        ICyberwareUserData cyberwareUserData = CyberwareAPI.getCapabilityOrNull(entityLivingBase);
        if (cyberwareUserData == null) return;
        
        ItemStack itemStackClaws = cyberwareUserData.getCyberware(new ItemStack(this, 1, META_CLAWS));
        if (!itemStackClaws.isEmpty())
        {
            boolean wasEquipped = getLastClaws(entityLivingBase);
            boolean isEquipped = entityLivingBase.getHeldItemMainhand().isEmpty()
                 && ( entityLivingBase.getPrimaryHand() == EnumHandSide.RIGHT
                    ? (cyberwareUserData.isCyberwareInstalled(new ItemStack(CyberwareContent.cyberlimbs, 1, ItemCyberlimb.META_RIGHT_CYBER_ARM)))
                    : (cyberwareUserData.isCyberwareInstalled(new ItemStack(CyberwareContent.cyberlimbs, 1, ItemCyberlimb.META_LEFT_CYBER_ARM))) );
            if ( isEquipped
              && EnableDisableHelper.isEnabled(itemStackClaws) )
            {
                addUnarmedDamage(entityLivingBase, itemStackClaws);
                lastClaws.put(entityLivingBase.getUniqueID(), true);

                if (!wasEquipped)
                {
                    if (FMLCommonHandler.instance().getSide() == Side.CLIENT)
                    {
                        updateHand(entityLivingBase, true);
                    }
                }
            }
            else if (wasEquipped)
            {
                removeUnarmedDamage(entityLivingBase, itemStackClaws);
                lastClaws.put(entityLivingBase.getUniqueID(), false);
            }
        }
        else
        {
            lastClaws.put(entityLivingBase.getUniqueID(), false);
        }
    }

    private void updateHand(EntityLivingBase entityLivingBase, boolean delay)
    {
        if ( Minecraft.getMinecraft() != null
          && Minecraft.getMinecraft().player != null )
        {
            if (entityLivingBase == Minecraft.getMinecraft().player)
            {
                clawsTime = Minecraft.getMinecraft().getRenderPartialTicks() + entityLivingBase.ticksExisted + (delay ? 5 : 0);
            }
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
    
    public void addUnarmedDamage(EntityLivingBase entityLivingBase, ItemStack stack)
    {
        if (stack.getItemDamage() == META_CLAWS)
        {
            HashMultimap<String, AttributeModifier> multimap = HashMultimap.create();

            multimap.put(SharedMonsterAttributes.ATTACK_DAMAGE.getName(), new AttributeModifier(strengthId, "Claws damage upgrade", 5.5F, 0));
            entityLivingBase.getAttributeMap().applyAttributeModifiers(multimap);
        }
    }

    public void removeUnarmedDamage(EntityLivingBase entityLivingBase, ItemStack stack)
    {
        if (stack.getItemDamage() == META_CLAWS)
        {
            HashMultimap<String, AttributeModifier> multimap = HashMultimap.create();

            multimap.put(SharedMonsterAttributes.ATTACK_DAMAGE.getName(), new AttributeModifier(strengthId, "Claws Claws upgrade", 5.5F, 0));
            entityLivingBase.getAttributeMap().removeAttributeModifiers(multimap);
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
        
        ItemStack itemStackMining = cyberwareUserData.getCyberware(new ItemStack(this, 1, META_MINING));
        boolean rightArm = ( entityPlayer.getPrimaryHand() == EnumHandSide.RIGHT
                           ? (cyberwareUserData.isCyberwareInstalled(new ItemStack(CyberwareContent.cyberlimbs, 1, ItemCyberlimb.META_RIGHT_CYBER_ARM)))
                           : (cyberwareUserData.isCyberwareInstalled(new ItemStack(CyberwareContent.cyberlimbs, 1, ItemCyberlimb.META_LEFT_CYBER_ARM))) );
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
    
        ItemStack itemStackMining = cyberwareUserData.getCyberware(new ItemStack(this, 1, META_MINING));
        boolean rightArm = ( entityPlayer.getPrimaryHand() == EnumHandSide.RIGHT
                           ? (cyberwareUserData.isCyberwareInstalled(new ItemStack(CyberwareContent.cyberlimbs, 1, ItemCyberlimb.META_RIGHT_CYBER_ARM)))
                           : (cyberwareUserData.isCyberwareInstalled(new ItemStack(CyberwareContent.cyberlimbs, 1, ItemCyberlimb.META_LEFT_CYBER_ARM))) );
        if ( rightArm
          && !itemStackMining.isEmpty()
          && entityPlayer.getHeldItemMainhand().isEmpty() )
        {
            ItemStack pick = new ItemStack(tool_level);
            event.setNewSpeed(event.getNewSpeed() * pick.getDestroySpeed(entityPlayer.world.getBlockState(event.getPos())));
        }
    }

    private static final UUID strengthId = UUID.fromString("63c32801-94fb-40d4-8bd2-89135c1e44b1");

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
