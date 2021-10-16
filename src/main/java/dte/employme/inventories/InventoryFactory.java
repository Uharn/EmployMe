package dte.employme.inventories;

import static dte.employme.utils.InventoryUtils.createWall;
import static org.bukkit.ChatColor.AQUA;
import static org.bukkit.ChatColor.GOLD;
import static org.bukkit.ChatColor.WHITE;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import dte.employme.board.JobBoard;
import dte.employme.items.ItemFactory;
import dte.employme.utils.InventoryUtils;
import dte.employme.utils.items.ItemBuilder;

public class InventoryFactory 
{
	private final ItemFactory itemFactory;
	private final JobBoard globalJobBoard;

	//cached menus
	private Inventory jobCreationInventory;

	public InventoryFactory(ItemFactory itemFactory, JobBoard globalJobBoard) 
	{
		this.itemFactory = itemFactory;
		this.globalJobBoard = globalJobBoard;
	}

	/*
	 * Menus
	 */
	public Inventory getCreationMenu(Player employer)
	{
		if(this.jobCreationInventory == null)
			this.jobCreationInventory = createJobCreationMenu();

		return this.jobCreationInventory;
	}

	public Inventory getDeletionMenu(Player employer)
	{
		Inventory inventory = Bukkit.createInventory(null, 9 * 6, "Select Jobs to Delete");

		this.globalJobBoard.getJobsOfferedBy(employer.getUniqueId()).stream()
		.map(job -> this.itemFactory.createDeletionIcon(this.globalJobBoard, job))
		.forEach(inventory::addItem);

		InventoryUtils.fillEmptySlots(inventory, InventoryUtils.createWall(Material.BLACK_STAINED_GLASS_PANE));

		return inventory;
	}

	private Inventory createJobCreationMenu()
	{
		Inventory inventory = Bukkit.createInventory(null, 9 * 3, "Create a new Job");

		inventory.setItem(11, new ItemBuilder(Material.GOLD_INGOT)
				.named(GOLD + "Money Job")
				.withLore(WHITE + "Click to offer a Job for which", WHITE + "You will pay a certain amount of money.")
				.createCopy());

		inventory.setItem(15, new ItemBuilder(Material.CHEST)
				.named(AQUA + "Items Job")
				.withLore(WHITE + "Click to offer a Job for which", WHITE + "You will pay with resources.")
				.createCopy());

		InventoryUtils.fillEmptySlots(inventory, createWall(Material.BLACK_STAINED_GLASS_PANE));

		return inventory;
	}
}
