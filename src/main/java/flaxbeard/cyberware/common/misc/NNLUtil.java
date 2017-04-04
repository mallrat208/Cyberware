package flaxbeard.cyberware.common.misc;

import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;

public class NNLUtil {
	public static NonNullList<ItemStack> copyList(NonNullList<ItemStack> l){
		NonNullList<ItemStack> l2 = NonNullList.create();
		l2.addAll(l);
		return l2;
	}
	
	public static NonNullList<ItemStack> fromArray(ItemStack[] array){
		NonNullList<ItemStack> l = NonNullList.create();
		for (ItemStack s : array){
			l.add(s);
		}
		return l;
	}
	
	public static NonNullList<NonNullList<ItemStack>> fromArray(ItemStack[][] array){
		NonNullList<NonNullList<ItemStack>> l = NonNullList.create();
		for (ItemStack[] a : array){
			NonNullList<ItemStack> l2 = NonNullList.create();
			for (ItemStack s : a){
				l2.add(s);
			}
			l.add(l2);
		}
		return l;
	}
	
	public static NonNullList<ItemStack> initListOfSize(int l){
		NonNullList<ItemStack> l2 = NonNullList.create();
		for (int i = 0; i < l; i ++){
			l2.add(ItemStack.EMPTY);
		}
		return l2;
	}
}
