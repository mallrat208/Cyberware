package flaxbeard.cyberware.common.item;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.NonNullList;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.api.CyberwareUpdateEvent;
import flaxbeard.cyberware.api.item.EnableDisableHelper;
import flaxbeard.cyberware.api.item.IMenuItem;
import flaxbeard.cyberware.common.CyberwareContent;
import flaxbeard.cyberware.common.lib.LibConstants;
import flaxbeard.cyberware.common.misc.NNLUtil;

public class ItemFootUpgrade extends ItemCyberware implements IMenuItem
{

    public static final int META_SPURS                      = 0;
    public static final int META_AQUA                       = 1;
    public static final int META_WHEELS                     = 2;
    
    public ItemFootUpgrade(String name, EnumSlot slot, String[] subnames)
    {
        super(name, slot, subnames);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public NonNullList<NonNullList<ItemStack>> required(ItemStack stack)
    {
        if (stack.getItemDamage() != META_AQUA) return NonNullList.create();

        return NNLUtil.fromArray(new ItemStack[][] {
                new ItemStack[] { new ItemStack(CyberwareContent.cyberlimbs, 1, ItemCyberlimb.META_LEFT_CYBER_LEG),
                                  new ItemStack(CyberwareContent.cyberlimbs, 1, ItemCyberlimb.META_RIGHT_CYBER_LEG) }});
    }

    @SubscribeEvent
    public void handleHorseMove(LivingUpdateEvent event)
    {
        EntityLivingBase entityLivingBase = event.getEntityLiving();
        if (entityLivingBase instanceof EntityHorse)
        {
            EntityHorse entityHorse = (EntityHorse) entityLivingBase;
            for (Entity entityPassenger : entityHorse.getPassengers())
            {
                if ( entityPassenger instanceof EntityLivingBase
                  && CyberwareAPI.isCyberwareInstalled(entityPassenger, new ItemStack(this, 1, META_SPURS)) )
                {
                    entityHorse.addPotionEffect(new PotionEffect(MobEffects.SPEED, 1, 5, true, false));
                    break;
                }
            }
        }
    }

    private Map<UUID, Boolean> mapIsAquaPowered = new HashMap<>();
    private Map<UUID, Integer> mapCountdownWheelsPowered = new HashMap<>();
    private Map<UUID, Float> mapStepHeight = new HashMap<>();

    @SubscribeEvent(priority=EventPriority.NORMAL)
    public void handleLivingUpdate(CyberwareUpdateEvent event)
    {
        EntityLivingBase entityLivingBase = event.getEntityLiving();

        ItemStack itemStackAqua = new ItemStack(this, 1, META_AQUA);
        if ( CyberwareAPI.isCyberwareInstalled(entityLivingBase, itemStackAqua)
          && entityLivingBase.isInWater() && !entityLivingBase.onGround )
        {
            int numLegs = 0;
            if (CyberwareAPI.isCyberwareInstalled(entityLivingBase, new ItemStack(CyberwareContent.cyberlimbs, 1, ItemCyberlimb.META_LEFT_CYBER_LEG)))
            {
                numLegs++;
            }
            if (CyberwareAPI.isCyberwareInstalled(entityLivingBase, new ItemStack(CyberwareContent.cyberlimbs, 1, ItemCyberlimb.META_RIGHT_CYBER_LEG)))
            {
                numLegs++;
            }
            boolean wasPowered = getIsAquaPowered(entityLivingBase);

            boolean isPowered = entityLivingBase.ticksExisted % 20 == 0
                              ? CyberwareAPI.getCapability(entityLivingBase).usePower(itemStackAqua, getPowerConsumption(itemStackAqua))
                              : wasPowered;
            if (isPowered)
            {
                if (entityLivingBase.moveForward > 0)
                {
                    entityLivingBase.moveRelative(0F, 0F, numLegs * 0.4F, 0.075F);
                }
            }

            mapIsAquaPowered.put(entityLivingBase.getUniqueID(), isPowered);
        }
        else
        {
            mapIsAquaPowered.put(entityLivingBase.getUniqueID(), true);
        }

        ItemStack itemStackWheels = new ItemStack(this, 1, META_WHEELS);
        if (CyberwareAPI.isCyberwareInstalled(entityLivingBase, itemStackWheels))
        {
            boolean wasPowered = getCountdownWheelsPowered(entityLivingBase) > 0;

            boolean isPowered = EnableDisableHelper.isEnabled(CyberwareAPI.getCyberware(entityLivingBase, itemStackWheels))
                             && ( entityLivingBase.ticksExisted % 20 == 0
                                ? CyberwareAPI.getCapability(entityLivingBase).usePower(itemStackWheels, getPowerConsumption(itemStackWheels))
                                : wasPowered );
            if (isPowered)
            {
                if (!mapStepHeight.containsKey(entityLivingBase.getUniqueID()))
                {
                    mapStepHeight.put(entityLivingBase.getUniqueID(), Math.max(entityLivingBase.stepHeight, .6F));
                }
                entityLivingBase.stepHeight = 1F;

                mapCountdownWheelsPowered.put(entityLivingBase.getUniqueID(), 10);
            }
            else if (mapStepHeight.containsKey(entityLivingBase.getUniqueID()) && wasPowered)
            {
                entityLivingBase.stepHeight = mapStepHeight.get(entityLivingBase.getUniqueID());

                mapCountdownWheelsPowered.put(entityLivingBase.getUniqueID(), getCountdownWheelsPowered(entityLivingBase) - 1);
            }
            else
            {
                mapCountdownWheelsPowered.put(entityLivingBase.getUniqueID(), 0);
            }
        }
        else if (mapStepHeight.containsKey(entityLivingBase.getUniqueID()))
        {
            entityLivingBase.stepHeight = mapStepHeight.get(entityLivingBase.getUniqueID());

            int countdownWheelsPowered = getCountdownWheelsPowered(entityLivingBase) - 1;
            if (countdownWheelsPowered == 0)
            {
                mapStepHeight.remove(entityLivingBase.getUniqueID());
            }

            mapCountdownWheelsPowered.put(entityLivingBase.getUniqueID(), countdownWheelsPowered);
        }
    }

    private boolean getIsAquaPowered(EntityLivingBase entityLivingBase)
    {
        if (!mapIsAquaPowered.containsKey(entityLivingBase.getUniqueID()))
        {
            mapIsAquaPowered.put(entityLivingBase.getUniqueID(), Boolean.TRUE);
        }
        return mapIsAquaPowered.get(entityLivingBase.getUniqueID());
    }

    private int getCountdownWheelsPowered(EntityLivingBase entityLivingBase)
    {
        if (!mapCountdownWheelsPowered.containsKey(entityLivingBase.getUniqueID()))
        {
            mapCountdownWheelsPowered.put(entityLivingBase.getUniqueID(), 10);
        }
        return mapCountdownWheelsPowered.get(entityLivingBase.getUniqueID());
    }

    @Override
    public int getPowerConsumption(ItemStack stack)
    {
        return stack.getItemDamage() == META_AQUA ? LibConstants.AQUA_CONSUMPTION :
               stack.getItemDamage() == META_WHEELS ? LibConstants.WHEEL_CONSUMPTION : 0;
    }

    @Override
    public boolean hasMenu(ItemStack stack)
    {
        return stack.getItemDamage() == META_WHEELS;
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