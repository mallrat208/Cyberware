package flaxbeard.cyberware.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.Capability.IStorage;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import flaxbeard.cyberware.Cyberware;
import flaxbeard.cyberware.api.item.HotkeyHelper;
import flaxbeard.cyberware.api.item.ICyberware;
import flaxbeard.cyberware.api.item.ICyberware.EnumSlot;
import flaxbeard.cyberware.api.item.ICyberware.ISidedLimb.EnumSide;
import flaxbeard.cyberware.api.item.IHudjack;
import flaxbeard.cyberware.api.item.IMenuItem;
import flaxbeard.cyberware.common.CyberwareConfig;
import flaxbeard.cyberware.common.lib.LibConstants;

public class CyberwareUserDataImpl implements ICyberwareUserData
{
	public static final IStorage<ICyberwareUserData> STORAGE = new CyberwareUserDataStorage();

	private NonNullList<NonNullList<ItemStack>> wares = NonNullList.create();
	private boolean[] missingEssentials = new boolean[EnumSlot.values().length * 2];
	
	private int storedPower = 0;
	private int prod = 0;
 	private int lastProd = 0;
 	private int cons = 0;
 	private int lastCons = 0;
	private int powerCap = 0;
	private Map<ItemStack, Integer> powerBuffer = new HashMap<ItemStack, Integer>();
	private Map<ItemStack, Integer> powerBufferLast = new HashMap<ItemStack, Integer>();
	private NonNullList<ItemStack> outOfPower = NonNullList.create();
	private List<Integer> outOfPowerTimes = new ArrayList<Integer>();
	private int missingEssence = 0;
	private NonNullList<ItemStack> specialBatteries = NonNullList.create();
	private int essence = 0;
	private int maxEssence = 0;
	private NonNullList<ItemStack> activeItems = NonNullList.create();
	private NonNullList<ItemStack> hudjackItems = NonNullList.create();
	private Map<Integer, ItemStack> hotkeys = new HashMap<Integer, ItemStack>();
	private NBTTagCompound hudData = new NBTTagCompound();
	private boolean hasOpenedRadialMenu = false;
	
	private int hudColor = 0x00FFFF;
	private float[] hudColorFloat = new float[] { 0F, 1F, 1F };
	
	public CyberwareUserDataImpl()
	{
		hudData = new NBTTagCompound();
		for (int i = 0; i < EnumSlot.values().length; i ++){
			NonNullList<ItemStack> wareSlot = NonNullList.create();
			for (int j = 0; j < LibConstants.WARE_PER_SLOT; j ++){
				wareSlot.add(ItemStack.EMPTY);
			}
			wares.add(wareSlot);
		}
		resetWare(null);
	}
	
	@Override
	public void resetWare(EntityLivingBase e)
	{
		for (NonNullList<ItemStack> slot : this.wares)
		{
			for (ItemStack item : slot)
			{
				if (CyberwareAPI.isCyberware(item))
				{
					CyberwareAPI.getCyberware(item).onRemoved(e, item);
				}
			}
		}
		missingEssence = 0;
		for (int i = 0; i < wares.size(); i++)
		{
			NonNullList<ItemStack> wareSlot = NonNullList.create();
			NonNullList<ItemStack> startItems = CyberwareConfig.getStartingItems(EnumSlot.values()[i]);
			for (ItemStack s : startItems){
				wareSlot.add(s.copy());
			}
			wares.set(i,wareSlot);
		}
		this.missingEssentials =  new boolean[EnumSlot.values().length * 2];
		this.updateCapacity();
	}
	
	@Override
	public List<ItemStack> getPowerOutages()
	{
		return outOfPower;
	}
	
	@Override
	public List<Integer> getPowerOutageTimes()
	{
		return outOfPowerTimes;
	}
	
	@Override
	public int getCapacity()
	{
		int specialCap = 0;
		for (ItemStack item : this.specialBatteries)
		{
			ISpecialBattery battery = (ISpecialBattery) CyberwareAPI.getCyberware(item);
			specialCap += battery.getCapacity(item);
		}
		return powerCap + specialCap;
	}
	
	/*public void updateEssential()
	{
		missingEssentials = new boolean[EnumSlot.values().length * 2];
		for (int slotNum = 0; slotNum < EnumSlot.values().length; slotNum++)
		{
			EnumSlot slot = EnumSlot.values()[slotNum];
			for (int i = 0; i < LibConstants.WARE_PER_SLOT; i++)
			{
				ItemStack stack = wares[slotNum][i];
				
				if (!stack.isEmpty())
				{
					ICyberware ware = CyberwareAPI.getCyberware(stack);
					if (ware.isEssential(stack))
					{
						if (slot.isSided() && ware instanceof ISidedLimb)
						{
							if (((ISidedLimb) ware).getSide(stack) == EnumSide.LEFT )
							{
								setHasEssential(slot, true, this.hasEssential(slot, EnumSide.RIGHT));
							}
							else
							{
								setHasEssential(slot, this.hasEssential(slot, EnumSide.LEFT), true);
							}
						}
						else
						{
							setHasEssential(slot, true, true);
						}
					}
					
				}
			}
		}
	}*/
	
	@Override
	public int getStoredPower()
	{
		int specialStored = 0;
		for (ItemStack item : this.specialBatteries)
		{
			ISpecialBattery battery = (ISpecialBattery) CyberwareAPI.getCyberware(item);
			specialStored += battery.getStoredEnergy(item);
		}
		return storedPower + specialStored;
	}
	
	@Override
	public float getPercentFull()
	{
		if (getCapacity() == 0) return -1F;
		return getStoredPower() / (1F * getCapacity());
	}
	
	@Override
	public boolean isAtCapacity(ItemStack stack)
	{
		return isAtCapacity(stack, 0);
	}
	
	@Override
	public boolean isAtCapacity(ItemStack stack, int buffer)
	{
		// buffer = Math.min(powerCap - 1, buffer); TODO
		int leftOverSpaceNormal = powerCap - storedPower;

		if (leftOverSpaceNormal > buffer) return false;
		
		int leftOverSpaceSpecial = 0;

		for (ItemStack batteryStack : this.specialBatteries)
		{
			ISpecialBattery battery = (ISpecialBattery) CyberwareAPI.getCyberware(batteryStack);
			int spaceInThisSpecial = battery.add(batteryStack, stack, buffer + 1, true);
			leftOverSpaceSpecial += spaceInThisSpecial;
			
			if (leftOverSpaceNormal + leftOverSpaceSpecial > buffer) return false;
		}
		
		return true;
	}
	
	
	
	@Override
	public void addPower(int amount, ItemStack inputter)
	{
		if (amount < 0)
		{
			throw new IllegalArgumentException("Amount must be positive!");
		}
		//if (powerCap < storedPower + amount)
		//{
		
		int amountAlready = 0;
		ItemStack stack = ItemStack.EMPTY;
		if (!inputter.isEmpty())
		{
			stack = new ItemStack(inputter.getItem(), 1, inputter.getItemDamage());
		}
		if (powerBuffer.containsKey(stack))
		{
			amountAlready = powerBuffer.get(stack);
		}
		powerBuffer.put(stack, amount + amountAlready);

		prod += amount;
		//}
		//storedPower = Math.min(powerCap, storedPower + amount);
	}
	
	private boolean canGiveOut = true;
	
	@Override
	public boolean usePower(ItemStack stack, int amount)
	{
		return this.usePower(stack, amount, true);
	}
	
	private int addMap(Map<ItemStack, Integer> map)
	{
		int total = 0;
		for (ItemStack key : map.keySet())
		{
			total += map.get(key);
		}
		return total;
	}
	
	private void subtractFromBufferLast(int amount)
	{
		for (ItemStack key : powerBufferLast.keySet())
		{
			int get = powerBufferLast.get(key);
			int amountToSubtract = Math.min(get, amount);
			amount -= amountToSubtract;
			powerBufferLast.put(key, get - amountToSubtract);
			if (amount <= 0) break;
		}
	}

	@Override
	public boolean usePower(ItemStack stack, int amount, boolean isPassive)
	{
		if (isImmune) return true;
		
		if (!canGiveOut)
		{
			if (FMLCommonHandler.instance().getSide() == Side.CLIENT)
			{
				setOutOfPower(stack);
			}
			return false;
		}

		cons += amount;
		
		int getPowerBufferLast = addMap(powerBufferLast);
		//System.out.println("BEFORE: " + getPowerBufferLast + " " + amount);
		int amountAvailable = storedPower + getPowerBufferLast;
		
		int amountAvailableSpecial = 0;
		if (amountAvailable < amount)
		{

			int toGo = amount - amountAvailable;

			for (ItemStack batteryStack : this.specialBatteries)
			{
				ISpecialBattery battery = (ISpecialBattery) CyberwareAPI.getCyberware(batteryStack);

				int extract = battery.extract(batteryStack, toGo, true);

				toGo -= extract;
				amountAvailableSpecial += extract;
				
				
				if (toGo <= 0) break;
			}
			
			if (amountAvailableSpecial + amountAvailable >= amount)
			{
				toGo = amount - amountAvailable;
				
				for (ItemStack batteryStack : this.specialBatteries)
				{
					ISpecialBattery battery = (ISpecialBattery) CyberwareAPI.getCyberware(batteryStack);
					int extract = battery.extract(batteryStack, toGo, false);
					toGo -= extract;
					
					if (toGo <= 0) break;
				}
				
				amount -= amountAvailableSpecial;
			}
			
		}
		
		if (amountAvailable < amount)
		{
			
			if (FMLCommonHandler.instance().getSide() == Side.CLIENT)
			{
				setOutOfPower(stack);
			}
			if (isPassive)
			{
				canGiveOut = false;
			}
			return false;
		}
		
		int leftAfterBuffer = Math.max(0, amount - getPowerBufferLast);
		subtractFromBufferLast(amount);
		storedPower -= leftAfterBuffer;
		//System.out.println("AFTER: " + addMap(powerBufferLast));
		return true;
	}
	
	@SideOnly(Side.CLIENT)
	public void setOutOfPower(ItemStack stack)
	{
		EntityPlayer p = Minecraft.getMinecraft().player;
		if (p != null && !stack.isEmpty())
		{
			int i = -1;
			int n = 0;
			for (ItemStack e : outOfPower)
			{
				if (!e.isEmpty() && e.getItem() == stack.getItem() && e.getItemDamage() == stack.getItemDamage())
				{
					i = n;
					break;
				}
				n++;
			}
			if (i != -1)
			{
				outOfPower.remove(i);
				outOfPowerTimes.remove(i);
			}
			outOfPower.add(stack);
			outOfPowerTimes.add(p.ticksExisted);
			if (outOfPower.size() >= 8)
			{
				outOfPower.remove(0);
				outOfPowerTimes.remove(0);
			}
		}
	}
	
	@Override
	public NonNullList<ItemStack> getInstalledCyberware(EnumSlot slot)
	{
		return wares.get(slot.ordinal());
	}
	
	@Override
	public boolean hasEssential(EnumSlot slot)
	{
		return !missingEssentials[slot.ordinal() * 2];
	}
	
	@Override
	public boolean hasEssential(EnumSlot slot, EnumSide side)
	{
		return !missingEssentials[slot.ordinal() * 2 + (side == EnumSide.LEFT ? 0 : 1)];
	}
	
	@Override
	public void setHasEssential(EnumSlot slot, boolean hasLeft, boolean hasRight)
	{
		missingEssentials[slot.ordinal() * 2] = !hasLeft;
		missingEssentials[slot.ordinal() * 2 + 1] = !hasRight;
	}

	@Override
	public void setInstalledCyberware(EntityLivingBase entity, EnumSlot slot, List<ItemStack> cyberware)
	{
		while (cyberware.size() > LibConstants.WARE_PER_SLOT)
		{
			cyberware.remove(cyberware.size() - 1);
		}
		while (cyberware.size() < LibConstants.WARE_PER_SLOT)
		{
			cyberware.add(ItemStack.EMPTY);
		}
		setInstalledCyberware(entity, slot, cyberware);
	}
	
	@Override
	public void updateCapacity()
	{
		powerCap = 0;
		specialBatteries = NonNullList.create();
		activeItems = NonNullList.create();
		hudjackItems = NonNullList.create();
		hotkeys = new HashMap<Integer, ItemStack>();
		
		for (EnumSlot slot : EnumSlot.values())
		{
			for (ItemStack wareStack : getInstalledCyberware(slot))
			{
				if (CyberwareAPI.isCyberware(wareStack))
				{
					ICyberware ware = CyberwareAPI.getCyberware(wareStack);
					
					if (ware instanceof IMenuItem && ((IMenuItem) ware).hasMenu(wareStack))
					{
						this.activeItems.add(wareStack);
						
						int hotkey = HotkeyHelper.getHotkey(wareStack);
						if (hotkey != -1)
						{
							hotkeys.put(hotkey, wareStack);
						}
					}
					
					if (ware instanceof IHudjack)
					{
						this.hudjackItems.add(wareStack);
					}
					
					if (ware instanceof ISpecialBattery)
					{
						this.specialBatteries.add(wareStack);
					}
					else
					{
						powerCap += ware.getCapacity(wareStack);
					}
				}
			}
		}
		
		storedPower = Math.min(storedPower, powerCap);
	}
	
	@Override
	public void setInstalledCyberware(EntityLivingBase entity, EnumSlot slot, NonNullList<ItemStack> cyberware)
	{
		if (cyberware.size() != wares.get(slot.ordinal()).size())
		{
			System.out.println("ERROR!");
		}
		NonNullList<ItemStack> newWares = cyberware;
		NonNullList<ItemStack> oldWares = wares.get(slot.ordinal());
		
		if (entity != null)
		{
			for (ItemStack oldWare : oldWares)
			{
				if (CyberwareAPI.isCyberware(oldWare))
				{
					boolean found = false;
					for (ItemStack newWare : newWares)
					{
						if (CyberwareAPI.areCyberwareStacksEqual(newWare, oldWare) && newWare.getCount() == oldWare.getCount())
						{
							found = true;
							break;
						}
					}
					
					if (!found)
					{
						
						CyberwareAPI.getCyberware(oldWare).onRemoved(entity, oldWare);
					}
				}
			}
			
			for (ItemStack newWare : newWares)
			{
				if (CyberwareAPI.isCyberware(newWare))
				{
					boolean found = false;
					for (ItemStack oldWare : oldWares)
					{
						if (CyberwareAPI.areCyberwareStacksEqual(newWare, oldWare) && newWare.getCount() == oldWare.getCount())
						{
							found = true;
							break;
						}
					}
					
					if (!found)
					{
						
						CyberwareAPI.getCyberware(newWare).onAdded(entity, newWare);
					}
				}
			}
		}
		
		wares.set(slot.ordinal(),cyberware);
	}
	
	@Override
	public boolean isCyberwareInstalled(ItemStack cyberware)
	{
		return getCyberwareRank(cyberware) > 0;
	}
	
	@Override
	public int getCyberwareRank(ItemStack cyberware)
	{
		ItemStack cw = getCyberware(cyberware);
		
		if (!cw.isEmpty())
		{
			return cw.getCount();
		}
		
		return 0;
	}
	
	@Override
	public ItemStack getCyberware(ItemStack cyberware)
	{
		NonNullList<ItemStack> slotItems = getInstalledCyberware(CyberwareAPI.getCyberware(cyberware).getSlot(cyberware));
		for (ItemStack item : slotItems)
		{
			if (!item.isEmpty() && item.getItem() == cyberware.getItem() && item.getItemDamage() == cyberware.getItemDamage())
			{
				return item;
			}
		}
		return ItemStack.EMPTY;
	}
	
	@Override
	public NBTTagCompound serializeNBT()
	{
		NBTTagCompound compound = new NBTTagCompound();
		NBTTagList list = new NBTTagList();
		
		for (EnumSlot slot : EnumSlot.values())
		{
			NBTTagList list2 = new NBTTagList();
			for (ItemStack cyberware : getInstalledCyberware(slot))
			{
				NBTTagCompound temp = new NBTTagCompound();
				if (!cyberware.isEmpty())
				{
					temp = cyberware.writeToNBT(temp);
				}
				list2.appendTag(temp);
			}
			list.appendTag(list2);
		}
		
		compound.setTag("cyberware", list);
		
		NBTTagList essentialList = new NBTTagList();
		for (int i = 0; i < this.missingEssentials.length; i++)
		{
			essentialList.appendTag(new NBTTagByte((byte) (this.missingEssentials[i] ? 1 : 0)));
		}
		compound.setTag("discard", essentialList);
		compound.setTag("powerBuffer", writeMap(this.powerBuffer));
		compound.setTag("powerBufferLast", writeMap(this.powerBufferLast));
		compound.setInteger("powerCap", this.powerCap);
		compound.setInteger("storedPower", this.storedPower);
		compound.setInteger("missingEssence", missingEssence);
		compound.setTag("hud", hudData);
		compound.setInteger("color", hudColor);
		compound.setBoolean("hasOpenedRadialMenu", hasOpenedRadialMenu);
		return compound;
	}
	
	private NBTTagList writeMap(Map<ItemStack, Integer> map)
	{
		NBTTagList list = new NBTTagList();
		
		for (ItemStack stack : map.keySet())
		{
			NBTTagCompound temp = new NBTTagCompound();
			NBTTagCompound item = new NBTTagCompound();
			temp.setBoolean("null", stack.isEmpty());
			if (!stack.isEmpty())
			{
				stack.writeToNBT(item);
				temp.setTag("item", item);
			}
			temp.setInteger("value", map.get(stack));

			list.appendTag(temp);
		}
		
		return list;
	}
	
	private Map<ItemStack, Integer> readMap(NBTTagList list)
	{
		Map<ItemStack, Integer> map = new HashMap<ItemStack, Integer>();
		for (int i = 0; i < list.tagCount(); i++)
		{
			NBTTagCompound temp = list.getCompoundTagAt(i);
			boolean isNull = temp.getBoolean("null");
			ItemStack stack = ItemStack.EMPTY;
			if (!isNull)
			{
				stack = new ItemStack(temp.getCompoundTag("item"));
			}
			
			map.put(stack, temp.getInteger("value"));
		}
		
		return map;
	}

	@Override
	public void deserializeNBT(NBTTagCompound tag)
	{
		powerBuffer = readMap((NBTTagList) tag.getTag("powerBuffer"));
		powerCap = tag.getInteger("powerCap");
		powerBufferLast = readMap((NBTTagList) tag.getTag("powerBufferLast"));

		storedPower = tag.getInteger("storedPower");
		if (tag.hasKey("essence"))
		{
			missingEssence = getMaxEssence() - tag.getInteger("essence");
		}
		else
		{
			missingEssence = tag.getInteger("missingEssence");
		}
		hudData = tag.getCompoundTag("hud");
		hasOpenedRadialMenu = tag.getBoolean("hasOpenedRadialMenu");
		NBTTagList essentialList = (NBTTagList) tag.getTag("discard");
		for (int i = 0; i < essentialList.tagCount(); i++)
		{
			this.missingEssentials[i] = ((NBTTagByte) essentialList.get(i)).getByte() > 0;
		}
		
		NBTTagList list = (NBTTagList) tag.getTag("cyberware");
		for (int i = 0; i < list.tagCount(); i++)
		{
			EnumSlot slot = EnumSlot.values()[i];
			
			NBTTagList list2 = (NBTTagList) list.get(i);
			NonNullList<ItemStack> cyberware = NonNullList.create();
			for (int j = 0; j < LibConstants.WARE_PER_SLOT; j ++){
				cyberware.add(ItemStack.EMPTY);
			}

			for (int j = 0; j < list2.tagCount() && j < cyberware.size(); j++)
			{
				
				cyberware.set(j,new ItemStack(list2.getCompoundTagAt(j)));
			}
			
			setInstalledCyberware(null, slot, cyberware);
		}
		
		int color = 0x00FFFF;

		if (tag.hasKey("color"))
		{
			color = tag.getInteger("color");
		}
		this.setHudColor(color);


		updateCapacity();
	}
	
	private static class CyberwareUserDataStorage implements IStorage<ICyberwareUserData>
	{
		@Override
		public NBTBase writeNBT(Capability<ICyberwareUserData> capability, ICyberwareUserData instance, EnumFacing side)
		{
			return instance.serializeNBT();
		}

		@Override
		public void readNBT(Capability<ICyberwareUserData> capability, ICyberwareUserData instance, EnumFacing side, NBTBase nbt)
		{
			if (nbt instanceof NBTTagCompound)
			{
				instance.deserializeNBT((NBTTagCompound) nbt);
			}
			else
			{
				throw new IllegalStateException("Cyberware NBT should be a NBTTagCompound!");
			}
		}
	}
	
	public static class Provider implements ICapabilitySerializable<NBTTagCompound>
	{
		public static final ResourceLocation NAME = new ResourceLocation(Cyberware.MODID, "cyberware");
		
		private final ICyberwareUserData cap = new CyberwareUserDataImpl();

		@Override
		public boolean hasCapability(Capability<?> capability, EnumFacing facing)
		{
			return capability == CyberwareAPI.CYBERWARE_CAPABILITY;
		}

		@Override
		public <T> T getCapability(Capability<T> capability, EnumFacing facing)
		{
			if (capability == CyberwareAPI.CYBERWARE_CAPABILITY)
			{
				return CyberwareAPI.CYBERWARE_CAPABILITY.cast(cap);
			}
			
			return null;
		}

		@Override
		public NBTTagCompound serializeNBT()
		{
			return cap.serializeNBT();
		}

		@Override
		public void deserializeNBT(NBTTagCompound nbt)
		{
			cap.deserializeNBT(nbt);
		}
		
	}
	
	private void storePower(Map<ItemStack, Integer> map)
	{
		for (ItemStack item : this.specialBatteries)
		{
			for (ItemStack key : map.keySet())
			{
				ISpecialBattery battery = (ISpecialBattery) CyberwareAPI.getCyberware(item);
				int keyAmount = map.get(key);
				int amountTaken = battery.add(item, key, keyAmount, false);
				map.put(key, keyAmount - amountTaken);
			}
		}
		this.storedPower = Math.min(this.powerCap, storedPower + addMap(map));
	}

	@Override
	public void resetBuffer()
	{
		canGiveOut = true;
		storePower(powerBufferLast);
		powerBufferLast = powerBuffer;
		powerBuffer = new HashMap<ItemStack, Integer>();
		this.isImmune = false;

		lastCons = cons;
		lastProd = prod;
		prod = 0;
		cons = 0;
	}

	@Override
	public void setImmune()
	{
		this.isImmune = true;
	}
	
	private boolean isImmune = false;

	@Override
	@Deprecated
	public int getEssence()
	{
		return getMaxEssence() - missingEssence;
	}
	
	@Override
	@Deprecated
	public int getMaxEssence()
	{
		return CyberwareConfig.ESSENCE;
		//return maxEssence; TODO
	}

	@Override
	@Deprecated
	public void setEssence(int e)
	{
		missingEssence = getMaxEssence() - e;
	}

	@Override
	public int getMaxTolerance(EntityLivingBase e)
	{
		return (int)e.getAttributeMap().getAttributeInstance(CyberwareAPI.TOLERANCE_ATTR).getAttributeValue();
	}

	@Override
	public int getTolerance(EntityLivingBase e)
	{
		return getMaxTolerance(e) - missingEssence;
	}

	@Override
	public void setTolerance(EntityLivingBase e, int amt)
	{
		missingEssence = getMaxTolerance(e) - amt;
	}



	@Override
	public int getNumActiveItems()
	{
		return activeItems.size();
	}

	@Override
	public List<ItemStack> getActiveItems()
	{
		return activeItems;
	}
	
	@Override
	public List<ItemStack> getHudjackItems()
	{
		return hudjackItems;
	}

	@Override
	public void removeHotkey(int i)
	{
		if (hotkeys.containsKey(i))
		{
			hotkeys.remove(i);
		}
	}

	@Override
	public void addHotkey(int i, ItemStack stack)
	{
		hotkeys.put(i, stack);
	}
	
	@Override
	public ItemStack getHotkey(int i)
	{
		if (!hotkeys.containsKey(i))
		{
			return ItemStack.EMPTY;
		}
		return hotkeys.get(i);
	}

	@Override
	public Iterable<Integer> getHotkeys()
	{
		return hotkeys.keySet();
	}

	@Override
	public void setHudData(NBTTagCompound comp)
	{
		hudData = comp;
	}

	@Override
	public NBTTagCompound getHudData()
	{
		return hudData;
	}
	
	@Override
	public boolean hasOpenedRadialMenu()
	{
		return hasOpenedRadialMenu;
	}
	
	@Override
	public void setOpenedRadialMenu(boolean hasOpenedRadialMenu)
	{
		this.hasOpenedRadialMenu = hasOpenedRadialMenu;
	}

	@Override
	public void setHudColor(int hexVal)
	{
		float r = ((hexVal >> 16) & 0x0000FF) / 255F;
		float g = ((hexVal >> 8) & 0x0000FF) / 255F;
		float b = ((hexVal) & 0x0000FF) / 255F;
		setHudColor(new float[] { r, g, b });
	}

	@Override
	public int getHudColorHex()
	{
		return hudColor;
	}

	@Override
	public void setHudColor(float[] color)
	{
		hudColorFloat = color;
		int ri = Math.round(color[0] * 255);
		int gi = Math.round(color[1] * 255);
		int bi = Math.round(color[2] * 255);
		
		int rp = (ri << 16) & 0xFF0000;
		int gp = (gi << 8) & 0x00FF00;
		int bp = (bi) & 0x0000FF;
		hudColor = rp | gp | bp;
	}

	@Override
	public float[] getHudColor()
	{
		return hudColorFloat;
	}

	@Override
	public int getProduction()
	{
		return lastProd;
	}

	@Override
 	public int getConsumption()
 	{
		return lastCons;
	}
}
