package dte.employme.inventories;

import static dte.employme.messages.MessageKey.INVENTORY_ITEMS_REWARD_OFFER_TITLE;
import static dte.employme.messages.MessageKey.INVENTORY_ITEMS_JOB_NO_ITEMS_WARNING;
import static java.util.stream.Collectors.toList;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;

import dte.employme.EmployMe;
import dte.employme.board.JobBoard;
import dte.employme.containers.service.PlayerContainerService;
import dte.employme.job.rewards.ItemsReward;
import dte.employme.messages.service.MessageService;
import dte.employme.utils.InventoryUtils;

public class ItemsRewardOfferGUI extends ChestGui
{
	public ItemsRewardOfferGUI(JobBoard jobBoard, MessageService messageService, PlayerContainerService playerContainerService) 
	{
		super(6, messageService.getMessage(INVENTORY_ITEMS_REWARD_OFFER_TITLE).first());

		setOnClose(event -> 
		{
			Player player = (Player) event.getPlayer();
			List<ItemStack> offeredItems = InventoryUtils.itemsStream(event.getInventory(), true).collect(toList());

			if(offeredItems.isEmpty()) 
			{
				messageService.getMessage(INVENTORY_ITEMS_JOB_NO_ITEMS_WARNING)
				.withGeneralPrefix()
				.sendTo(player);
				return;
			}
			ItemsReward itemsReward = new ItemsReward(offeredItems, playerContainerService);
			GoalCustomizationGUI goalCustomizationGUI = new GoalCustomizationGUI(messageService, jobBoard, itemsReward);

			Bukkit.getScheduler().runTask(EmployMe.getInstance(), () -> goalCustomizationGUI.show(player));
		});
	}
}
