package dte.employme.items;

import static dte.employme.utils.ChatColorUtils.bold;
import static dte.employme.utils.ChatColorUtils.colorize;
import static dte.employme.utils.ChatColorUtils.createSeparationLine;
import static dte.employme.utils.ChatColorUtils.underlined;
import static org.bukkit.ChatColor.AQUA;
import static org.bukkit.ChatColor.DARK_RED;
import static org.bukkit.ChatColor.GRAY;
import static org.bukkit.ChatColor.GREEN;
import static org.bukkit.ChatColor.RED;
import static org.bukkit.ChatColor.WHITE;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.craftbukkit.libs.org.apache.commons.lang3.StringUtils;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import com.google.common.collect.Lists;

import dte.employme.board.JobBoard;
import dte.employme.goal.Goal;
import dte.employme.goal.ItemGoal;
import dte.employme.job.Job;
import dte.employme.utils.items.builder.ItemBuilder;
import dte.employme.visitors.goal.InventoryGoalDescriptor;
import dte.employme.visitors.reward.InventoryRewardDescriptor;

public class ItemFactory
{
	//Container of factory methods
	private ItemFactory(){}

	/*
	 * Jobs
	 */
	public static ItemStack createBasicIcon(Job job) 
	{
		//lore
		List<String> lore = new ArrayList<>();
		lore.add(underlined(AQUA) + "Description" + AQUA + ":");
		lore.add(WHITE + "I need " + job.getGoal().accept(InventoryGoalDescriptor.INSTANCE));
		lore.add(" ");
		lore.addAll(job.getReward().accept(InventoryRewardDescriptor.INSTANCE));

		return new ItemBuilder(getJobMaterial(job), GREEN + job.getEmployer().getName() + "'s Offer")
				.itemFlags(ItemFlag.HIDE_ATTRIBUTES)
				.newLore(lore.toArray(new String[0]))
				.createCopy();
	}

	public static ItemStack createOfferIcon(JobBoard jobBoard, Job job, Player player) 
	{
		ItemStack basicIcon = createBasicIcon(job);

		//add the status and ID to the lore
		List<String> lore = basicIcon.getItemMeta().getLore();
		lore.add(" ");
		lore.addAll(createJobStatusLore(job, player));
		lore.add(createIDLine(job, jobBoard));

		return new ItemBuilder(basicIcon, false)
				.newLore(lore.toArray(new String[0]))
				.createCopy();
	}

	public static ItemStack createDeletionIcon(JobBoard jobBoard, Job job) 
	{
		return new ItemBuilder(createBasicIcon(job), false)
				.addToLore(true,
						createSeparationLine(GRAY, 23),
						bold(DARK_RED) + "Click to Delete!",
						createSeparationLine(GRAY, 23),
						createIDLine(job, jobBoard))
				.createCopy();
	}

	public static Optional<String> getJobID(ItemStack jobIcon)
	{
		if(!jobIcon.hasItemMeta() || !jobIcon.getItemMeta().hasLore() || jobIcon.getItemMeta().getLore().isEmpty())
			return Optional.empty();

		List<String> lore = jobIcon.getItemMeta().getLore();
		String lastLine = lore.get(lore.size()-1);

		return Optional.of(ChatColor.stripColor(lastLine.substring(6)));
	}


	private static List<String> createJobStatusLore(Job job, Player player) 
	{
		boolean finished = job.hasFinished(player);
		ChatColor lineColor = finished ? WHITE : DARK_RED;

		return Lists.newArrayList(
				createSeparationLine(lineColor, 23),
				finished ? (StringUtils.repeat(' ', 6) + bold(GREEN) +  "Click to Finish!") : (RED + "You didn't complete this Job."),
						createSeparationLine(lineColor, 23)
				);
	}

	private static Material getJobMaterial(Job job) 
	{
		Goal goal = job.getGoal();

		if(goal instanceof ItemGoal)
		{
			ItemGoal itemGoal = (ItemGoal) goal;

			return itemGoal.getItem().getType();
		}
		return Material.BOOK;
	}
	
	private static String createIDLine(Job job, JobBoard jobBoard)
	{
		return colorize(String.format("&7ID: %s", jobBoard.getJobID(job).get()));
	}
}
