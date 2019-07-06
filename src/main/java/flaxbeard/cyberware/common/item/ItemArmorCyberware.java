package flaxbeard.cyberware.common.item;

import javax.annotation.Nonnull;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import flaxbeard.cyberware.Cyberware;
import flaxbeard.cyberware.api.item.IDeconstructable;
import flaxbeard.cyberware.client.ClientUtils;
import flaxbeard.cyberware.common.CyberwareContent;

public class ItemArmorCyberware extends ItemArmor implements IDeconstructable
{
	public static class ModelTrenchcoat extends ModelBiped
	{
		public ModelRenderer bottomThing;
		
		public ModelTrenchcoat(float modelSize)
		{
			super(modelSize);
			bottomThing = new ModelRenderer(this, 16, 0);
			bottomThing.addBox(-4.0F, 0F, -1.7F, 8, 12, 4, modelSize);
			bottomThing.setRotationPoint(0, 12.0F, 0.0F);
		}
		
		@Override
		public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor, Entity entityIn)
		{
			super.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor, entityIn);
			
			bottomThing.setRotationPoint(0, bipedLeftLeg.rotationPointY, bipedLeftLeg.rotationPointZ);
			bottomThing.rotateAngleX = Math.max(bipedLeftLeg.rotateAngleX, bipedRightLeg.rotateAngleX) + .05F * 1.1F;
		}
		
		@Override
		public void render(@Nonnull Entity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale)
		{
			super.render(entityIn, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
			GlStateManager.pushMatrix();

			if (isChild)
			{
				float f = 2.0F;
				GlStateManager.scale(1.0F / f, 1.0F / f, 1.0F / f);
				GlStateManager.translate(0.0F, 24.0F * scale, 0.0F);
				bottomThing.render(scale);
		
			}
			else
			{
				if (entityIn.isSneaking())
				{
					GlStateManager.translate(0.0F, 0.2F, 0.0F);
				}

				bottomThing.render(scale);
			}

			GlStateManager.popMatrix();
		}
	}
	
	public ItemArmorCyberware(String name, ArmorMaterial materialIn, int renderIndexIn, EntityEquipmentSlot equipmentSlotIn)
	{
		super(materialIn, renderIndexIn, equipmentSlotIn);
		
		setRegistryName(name);
		ForgeRegistries.ITEMS.register(this);
		setTranslationKey(Cyberware.MODID + "." + name);
		
		setCreativeTab(Cyberware.creativeTab);
		
		CyberwareContent.items.add(this);
	}

	@Override
	public boolean canDestroy(ItemStack stack)
	{
		return true;
	}

	@Override
	public NonNullList<ItemStack> getComponents(ItemStack stack)
	{
		Item i = stack.getItem();
		
		if (i == CyberwareContent.trenchcoat)
		{
			NonNullList<ItemStack> l = NonNullList.create();
			l.add(new ItemStack(CyberwareContent.component, 2, 2));
			l.add(new ItemStack(Items.LEATHER, 12, 0));
			l.add(new ItemStack(Items.DYE, 1, 0));
			return l;
		}
		else if (i == CyberwareContent.jacket)
		{
			NonNullList<ItemStack> l = NonNullList.create();
			l.add(new ItemStack(CyberwareContent.component, 1, 2));
			l.add(new ItemStack(Items.LEATHER, 8, 0));
			l.add(new ItemStack(Items.DYE, 1, 0));
			return l;
		}
		NonNullList<ItemStack> l = NonNullList.create();
		l.add(new ItemStack(Blocks.STAINED_GLASS, 4, 15));
		l.add(new ItemStack(CyberwareContent.component, 1, 4));
		return l;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public ModelBiped getArmorModel(EntityLivingBase entityLivingBase, ItemStack itemStack, EntityEquipmentSlot armorSlot, net.minecraft.client.model.ModelBiped _default)
	{
		ClientUtils.trench.setModelAttributes(_default);
		ClientUtils.armor.setModelAttributes(_default);
		ClientUtils.trench.bipedRightArm.isHidden = !(entityLivingBase instanceof EntityPlayer) && !(entityLivingBase instanceof EntityArmorStand);
		ClientUtils.trench.bipedLeftArm.isHidden = !(entityLivingBase instanceof EntityPlayer) && !(entityLivingBase instanceof EntityArmorStand);
		ClientUtils.armor.bipedRightArm.isHidden = ClientUtils.trench.bipedRightArm.isHidden;
		ClientUtils.armor.bipedLeftArm.isHidden = ClientUtils.trench.bipedLeftArm.isHidden;

		if (!itemStack.isEmpty() && itemStack.getItem() == CyberwareContent.trenchcoat) return ClientUtils.trench;
		
		return ClientUtils.armor;
	}
	
	@Override
	public boolean hasColor(@Nonnull ItemStack stack)
	{
		if (getArmorMaterial() != CyberwareContent.trenchMat)
		{
			return false;
		}
		else
		{
			NBTTagCompound tagCompound = stack.getTagCompound();
			return tagCompound != null
			    && tagCompound.hasKey("display", 10)
			    && tagCompound.getCompoundTag("display").hasKey("color", 3);
		}
	}
	
	@Override
	public int getColor(@Nonnull ItemStack stack)
	{
		if (getArmorMaterial() != CyberwareContent.trenchMat)
		{
			return 16777215;
		}
		else
		{
			NBTTagCompound tagCompound = stack.getTagCompound();

			if (tagCompound != null)
			{
				NBTTagCompound tagCompoundDisplay = tagCompound.getCompoundTag("display");

				if (tagCompoundDisplay.hasKey("color", 3))
				{
					return tagCompoundDisplay.getInteger("color");
				}
			}

			return 0x333333; // 0x664028
		}
	}

	@Override
	public void removeColor(@Nonnull ItemStack stack)
	{
		if (getArmorMaterial() == CyberwareContent.trenchMat)
		{
			NBTTagCompound tagCompound = stack.getTagCompound();

			if (tagCompound != null)
			{
				NBTTagCompound tagCompoundDisplay = tagCompound.getCompoundTag("display");

				if (tagCompoundDisplay.hasKey("color"))
				{
					tagCompoundDisplay.removeTag("color");
				}
			}
		}
	}

	public void setColor(ItemStack stack, int color)
	{
		if (getArmorMaterial() != CyberwareContent.trenchMat)
		{
			throw new UnsupportedOperationException("Can\'t dye non-leather!");
		}
		else
		{
			NBTTagCompound tagCompound = stack.getTagCompound();

			if (tagCompound == null)
			{
				tagCompound = new NBTTagCompound();
				stack.setTagCompound(tagCompound);
			}

			NBTTagCompound tagCompoundDisplay = tagCompound.getCompoundTag("display");

			if (!tagCompound.hasKey("display", 10))
			{
				tagCompound.setTag("display", tagCompoundDisplay);
			}

			tagCompoundDisplay.setInteger("color", color);
		}
	}
	
	@Override
	public void getSubItems(@Nonnull CreativeTabs tab, @Nonnull NonNullList<ItemStack> list)
	{
		if (isInCreativeTab(tab)) {
			if (getArmorMaterial() == CyberwareContent.trenchMat)
			{
				super.getSubItems(tab, list);
				ItemStack brown = new ItemStack(this);
				setColor(brown, 0x664028);
				list.add(brown);
				ItemStack white = new ItemStack(this);
				setColor(white, 0xEAEAEA);
				list.add(white);
			}
			else
			{
				super.getSubItems(tab, list);
			}
		}
	}
}
