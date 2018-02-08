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

    public ItemFootUpgrade(String name, EnumSlot[] slot, String[] subnames)
    {
        super(name, new EnumSlot[][] { slot }, subnames);
        MinecraftForge.EVENT_BUS.register(this);

    }

    @Override
    public NonNullList<NonNullList<ItemStack>> required(ItemStack stack)
    {
        if (stack.getItemDamage() != 1) return NonNullList.create();

        return NNLUtil.fromArray(new ItemStack[][] {
                new ItemStack[] { new ItemStack(CyberwareContent.cyberlimbs, 1, 2), new ItemStack(CyberwareContent.cyberlimbs, 1, 3) }});
    }

    @SubscribeEvent
    public void handleHorseMove(LivingUpdateEvent event)
    {
        EntityLivingBase e = event.getEntityLiving();
        if (e instanceof EntityHorse)
        {
            EntityHorse horse = (EntityHorse) e;
            for (Entity pass : horse.getPassengers())
            {
                if (pass instanceof EntityLivingBase && CyberwareAPI.isCyberwareInstalled(pass, new ItemStack(this, 1, 0)))
                {
                    horse.addPotionEffect(new PotionEffect(MobEffects.SPEED, 1, 5, true, false));
                    break;
                }
            }
        }
    }


    private Map<UUID, Boolean> lastAqua = new HashMap<UUID, Boolean>();
    private Map<UUID, Integer> lastWheels = new HashMap<UUID, Integer>();
    private Map<UUID, Float> stepAssist = new HashMap<UUID, Float>();

    @SubscribeEvent(priority=EventPriority.NORMAL)
    public void handleLivingUpdate(CyberwareUpdateEvent event)
    {
        EntityLivingBase e = event.getEntityLiving();

        ItemStack test = new ItemStack(this, 1, 1);
        if (CyberwareAPI.isCyberwareInstalled(e, test) && e.isInWater() && !e.onGround)
        {
            int numLegs = 0;
            if (CyberwareAPI.isCyberwareInstalledInSlot(e, test, EnumSlot.FOOT))
            {
                numLegs++;
            }
            if (CyberwareAPI.isCyberwareInstalledInSlot(e, test, EnumSlot.FOOT_LEFT))
            {
                numLegs++;
            }
            Boolean last = getLastAqua(e);

            boolean powerUsed = e.ticksExisted % 20 == 0 ? CyberwareAPI.getCapability(e).usePower(test, getPowerConsumption(test)) : last;
            if (powerUsed)
            {
                if (e.moveForward > 0)
                {
                    //e.moveRelative(0F, numLegs * 0.4F, 0.075F);
                    //e.moveRelative(0F, numLegs * 0.4F, 0.075F, 0.0F);
                    e.moveRelative(0F, 0F, numLegs * 0.4F, 0.075F);
                }
            }

            lastAqua.put(e.getUniqueID(), powerUsed);
        }
        else
        {
            lastAqua.put(e.getUniqueID(), true);
        }

        test = new ItemStack(this, 1, 2);
        if (CyberwareAPI.isCyberwareInstalled(e, test))
        {
            Boolean last = getLastWheels(e) > 0;

            boolean powerUsed = EnableDisableHelper.isEnabled(CyberwareAPI.getCyberware(e, test)) && (e.ticksExisted % 20 == 0 ? CyberwareAPI.getCapability(e).usePower(test, getPowerConsumption(test)) : last);
            if (powerUsed)
            {
                if (!stepAssist.containsKey(e.getUniqueID()))
                {
                    stepAssist.put(e.getUniqueID(), Math.max(e.stepHeight, .6F));
                }
                e.stepHeight = 1F;

                lastWheels.put(e.getUniqueID(), 10);


            }
            else if (stepAssist.containsKey(e.getUniqueID()) && last)
            {

                e.stepHeight = stepAssist.get(e.getUniqueID());

                lastWheels.put(e.getUniqueID(), getLastWheels(e) - 1);
            }
            else
            {
                lastWheels.put(e.getUniqueID(), 0);
            }


        }
        else if (stepAssist.containsKey(e.getUniqueID()))
        {

            e.stepHeight = stepAssist.get(e.getUniqueID());

            int glw = getLastWheels(e) - 1;

            if (glw == 0)
            {
                stepAssist.remove(e.getUniqueID());
            }

            lastWheels.put(e.getUniqueID(), glw);

        }
    }

    private boolean getLastAqua(EntityLivingBase e)
    {
        if (!lastAqua.containsKey(e.getUniqueID()))
        {
            lastAqua.put(e.getUniqueID(), Boolean.TRUE);
        }
        return lastAqua.get(e.getUniqueID());
    }

    private int getLastWheels(EntityLivingBase e)
    {
        if (!lastWheels.containsKey(e.getUniqueID()))
        {
            lastWheels.put(e.getUniqueID(), 10);
        }
        return lastWheels.get(e.getUniqueID());
    }

    @Override
    public int getPowerConsumption(ItemStack stack)
    {
        return stack.getItemDamage() == 1 ? LibConstants.AQUA_CONSUMPTION :
                stack.getItemDamage() == 2 ? LibConstants.WHEEL_CONSUMPTION : 0;
    }

    @Override
    public boolean hasMenu(ItemStack stack)
    {
        return stack.getItemDamage() == 2;
    }

    @Override
    public void use(Entity e, ItemStack stack)
    {
        EnableDisableHelper.toggle(stack);
    }

    @Override
    public String getUnlocalizedLabel(ItemStack stack)
    {
        return EnableDisableHelper.getUnlocalizedLabel(stack);
    }


    private static final float[] f = new float[] { 1F, 0F, 0F };

    @Override
    public float[] getColor(ItemStack stack)
    {
        return EnableDisableHelper.isEnabled(stack) ? f : null;
    }
}