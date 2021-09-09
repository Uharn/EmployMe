package dte.employme.reward;

import java.util.Arrays;
import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.google.common.collect.Lists;

import dte.employme.visitors.reward.RewardVisitor;

public class ItemsReward implements Reward
{
	private final ItemStack[] items;
	
	public ItemsReward(ItemStack... items) 
	{
		//TODO: move this to its own branch
		this.items = Arrays.stream(items)
				.map(ItemStack::new) //clone the items using the copy constructor
				.toArray(ItemStack[]::new);
	}
	
	@Override
	public void giveTo(Player whoCompleted) 
	{
		whoCompleted.getInventory().addItem(this.items);
	}
	
	public List<ItemStack> getItems() 
	{
		return Lists.newArrayList(this.items);
	}

	@Override
	public <R> R accept(RewardVisitor<R> visitor) 
	{
		return visitor.visit(this);
	}
}