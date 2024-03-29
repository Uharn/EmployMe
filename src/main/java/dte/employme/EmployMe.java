package dte.employme;

import static dte.employme.messages.MessageKey.GLOBAL_JOB_BOARD_IS_FULL;
import static dte.employme.messages.MessageKey.JOB_ADDED_NOTIFIER_NOT_FOUND;
import static dte.employme.messages.MessageKey.MATERIAL_NOT_FOUND;
import static dte.employme.messages.MessageKey.MUST_BE_SUBSCRIBED_TO_GOAL;
import static dte.employme.messages.MessageKey.MUST_NOT_BE_CONVERSING;
import static dte.employme.messages.MessageKey.YOU_OFFERED_TOO_MANY_JOBS;
import static org.bukkit.ChatColor.DARK_GREEN;
import static org.bukkit.ChatColor.GREEN;
import static org.bukkit.ChatColor.RED;

import java.util.List;
import java.util.stream.Stream;

import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import co.aikar.commands.BukkitCommandManager;
import co.aikar.commands.ConditionFailedException;
import co.aikar.commands.InvalidCommandArgument;
import dte.employme.board.SimpleJobBoard;
import dte.employme.board.displayers.InventoryBoardDisplayer;
import dte.employme.board.listenable.EmployerNotificationListener;
import dte.employme.board.listenable.JobAddNotificationListener;
import dte.employme.board.listenable.JobCompletedMessagesListener;
import dte.employme.board.listenable.JobGoalTransferListener;
import dte.employme.board.listenable.JobRewardGiveListener;
import dte.employme.board.listenable.ListenableJobBoard;
import dte.employme.board.listenable.SimpleListenableJobBoard;
import dte.employme.commands.EmploymentCommand;
import dte.employme.config.ConfigFile;
import dte.employme.config.ConfigFileFactory;
import dte.employme.config.Messages;
import dte.employme.containers.service.PlayerContainerService;
import dte.employme.containers.service.SimplePlayerContainerService;
import dte.employme.items.JobIconFactory;
import dte.employme.job.Job;
import dte.employme.job.SimpleJob;
import dte.employme.job.addnotifiers.AllJobsNotifier;
import dte.employme.job.addnotifiers.DoNotNotify;
import dte.employme.job.addnotifiers.JobAddedNotifier;
import dte.employme.job.addnotifiers.MaterialSubscriptionNotifier;
import dte.employme.job.addnotifiers.service.JobAddedNotifierService;
import dte.employme.job.addnotifiers.service.SimpleJobAddedNotifierService;
import dte.employme.job.rewards.ItemsReward;
import dte.employme.job.rewards.MoneyReward;
import dte.employme.job.service.JobService;
import dte.employme.job.service.SimpleJobService;
import dte.employme.job.subscription.JobSubscriptionService;
import dte.employme.job.subscription.SimpleJobSubscriptionService;
import dte.employme.listeners.AutoUpdateListeners;
import dte.employme.listeners.PlayerContainerAbuseListener;
import dte.employme.messages.Placeholders;
import dte.employme.messages.service.ColoredMessageService;
import dte.employme.messages.service.MessageService;
import dte.employme.messages.service.TranslatedMessageService;
import dte.employme.utils.AutoUpdater;
import dte.employme.utils.PermissionUtils;
import dte.employme.utils.java.ServiceLocator;
import dte.modernjavaplugin.ModernJavaPlugin;
import net.milkbowl.vault.economy.Economy;

public class EmployMe extends ModernJavaPlugin
{
	private Economy economy;
	private ListenableJobBoard globalJobBoard;
	private JobService jobService;
	private PlayerContainerService playerContainerService;
	private JobSubscriptionService jobSubscriptionService;
	private JobAddedNotifierService jobAddedNotifierService;
	private MessageService messageService;
	private JobIconFactory jobIconFactory;
	private ConfigFile jobsConfig, subscriptionsConfig, jobAddNotifiersConfig, itemsContainersConfig, rewardsContainersConfig, messagesConfig;
	
	public static final String CHAT_PREFIX = DARK_GREEN + "[" + GREEN + "EmployMe" + DARK_GREEN + "]";

	private static EmployMe INSTANCE;

	@Override
	public void onEnable()
	{
		INSTANCE = this;

		//init economy
		this.economy = getEconomy();

		if(this.economy == null) 
		{
			disableWithError(RED + "Economy wasn't found! Shutting Down...");
			return;
		}
		ServiceLocator.register(Economy.class, this.economy);
		
		
		
		//init the configs
		Stream.of(SimpleJob.class, MoneyReward.class, ItemsReward.class).forEach(ConfigurationSerialization::registerClass);
		
		ConfigFileFactory configFileFactory = new ConfigFileFactory.Builder()
				.handleCreationException((exception, config) -> disableWithError(RED + String.format("Error while creating %s: %s", config.getFile().getName(), exception.getMessage())))
				.handleSaveException((exception, config) -> disableWithError(RED + String.format("Error while saving %s: %s", config.getFile().getName(), exception.getMessage())))
				.build();
		
		this.subscriptionsConfig = configFileFactory.loadConfig("subscriptions");
		this.jobAddNotifiersConfig = configFileFactory.loadConfig("job add notifiers");
		this.itemsContainersConfig = configFileFactory.loadContainer("items");
		this.rewardsContainersConfig = configFileFactory.loadContainer("rewards");
		this.messagesConfig = configFileFactory.loadMessagesConfig(Messages.ENGLISH);
		
		if(this.subscriptionsConfig == null || this.jobAddNotifiersConfig == null || this.itemsContainersConfig == null || this.rewardsContainersConfig == null || this.messagesConfig == null)
			return;
		
		
		
		//init the global job board, services, factories, etc.
		this.globalJobBoard = new SimpleListenableJobBoard(new SimpleJobBoard());
		this.messageService = new ColoredMessageService(new TranslatedMessageService(this.messagesConfig));
		
		this.jobSubscriptionService = new SimpleJobSubscriptionService(this.subscriptionsConfig);
		this.jobSubscriptionService.loadSubscriptions();
		ServiceLocator.register(JobSubscriptionService.class, this.jobSubscriptionService);
		
		this.playerContainerService = new SimplePlayerContainerService(this.itemsContainersConfig, this.rewardsContainersConfig, this.messageService);
		this.playerContainerService.loadContainers();
		ServiceLocator.register(PlayerContainerService.class, this.playerContainerService);
		
		this.jobsConfig = configFileFactory.loadConfig("jobs");
		
		if(this.jobsConfig == null)
			return;
		
		this.jobIconFactory = new JobIconFactory(this.messageService);
		
		this.jobService = new SimpleJobService(this.globalJobBoard, this.jobsConfig);
		this.jobService.loadJobs();

		this.jobAddedNotifierService = new SimpleJobAddedNotifierService(this.jobAddNotifiersConfig);
		this.jobAddedNotifierService.register(new DoNotNotify());
		this.jobAddedNotifierService.register(new AllJobsNotifier(this.messageService));
		this.jobAddedNotifierService.register(new MaterialSubscriptionNotifier(this.messageService, this.jobSubscriptionService));
		this.jobAddedNotifierService.loadPlayersNotifiers();

		this.globalJobBoard.registerCompleteListener(new JobRewardGiveListener(), new JobGoalTransferListener(this.playerContainerService), new JobCompletedMessagesListener(this.messageService));
		this.globalJobBoard.registerAddListener(new EmployerNotificationListener(this.messageService), new JobAddNotificationListener(this.jobAddedNotifierService));



		//register commands, listeners, metrics
		registerCommands();
		registerListeners(new PlayerContainerAbuseListener(this.playerContainerService));

		setDisableListener(() -> 
		{
			this.jobService.saveJobs();
			this.playerContainerService.saveContainers();
			this.jobSubscriptionService.saveSubscriptions();
			this.jobAddedNotifierService.savePlayersNotifiers();
		});
		
		new Metrics(this, 13423);
		
		AutoUpdater.forPlugin(this, 96513)
		.ifRequestFailed(exception -> logToConsole(RED + "There was an internet error while checking for an update!"))
		.ifNewUpdate(newVersion -> registerListeners(new AutoUpdateListeners(this.messageService, newVersion)))
		.check();
	}

	public static EmployMe getInstance()
	{
		return INSTANCE;
	}

	private Economy getEconomy() 
	{
		if(Bukkit.getPluginManager().getPlugin("Vault") == null)
			return null;

		RegisteredServiceProvider<Economy> provider = Bukkit.getServicesManager().getRegistration(Economy.class);

		if(provider == null)
			return null;

		return provider.getProvider();
	}

	@SuppressWarnings("deprecation")
	private void registerCommands() 
	{
		BukkitCommandManager commandManager = new BukkitCommandManager(this);
		commandManager.enableUnstableAPI("help");

		//register conditions
		commandManager.getCommandConditions().addCondition(Player.class, "Not Conversing", (handler, context, payment) -> 
		{
			if(context.getPlayer().isConversing())
				throw new InvalidCommandArgument(this.messageService.getMessage(MUST_NOT_BE_CONVERSING).first(), false);
		});

		commandManager.getCommandConditions().addCondition(Material.class, "Subscribed To Goal", (handler, context, material) -> 
		{
			if(!this.jobSubscriptionService.isSubscribedTo(context.getPlayer().getUniqueId(), material))
				throw new InvalidCommandArgument(this.messageService.getMessage(MUST_BE_SUBSCRIBED_TO_GOAL).first(), false);
		});
		
		commandManager.getCommandConditions().addCondition("Global Jobs Board Not Full", context -> 
		{
			if(this.globalJobBoard.getOfferedJobs().size() == ((6*9)-26)) 
				throw new ConditionFailedException(this.messageService.getMessage(GLOBAL_JOB_BOARD_IS_FULL).first());
		});
		
		commandManager.getCommandConditions().addCondition(Player.class, "Can Offer More Jobs", (handler, context, player) -> 
		{
			String jobPermission = PermissionUtils.findPermission(player, permission -> permission.startsWith("employme.jobs.allowed."))
					.orElse("employme.jobs.allowed.3");
			
			int allowedJobs = Integer.parseInt(jobPermission.split("\\.")[jobPermission.split("\\.").length-1]);
			
			if(this.globalJobBoard.getJobsOfferedBy(player.getUniqueId()).size() >= allowedJobs)
				throw new ConditionFailedException(this.messageService.getMessage(YOU_OFFERED_TOO_MANY_JOBS).first());
		});
		
		//register contexts
		commandManager.getCommandContexts().registerContext(Material.class, context -> 
		{
			Material material = Material.matchMaterial(context.popFirstArg());

			if(material == null)
				throw new InvalidCommandArgument(this.messageService.getMessage(MATERIAL_NOT_FOUND).first(), false);

			return material;
		});

		commandManager.getCommandContexts().registerContext(JobAddedNotifier.class, context -> 
		{
			String notifierName = context.joinArgs();
			JobAddedNotifier notifier = this.jobAddedNotifierService.getByName(notifierName);

			if(notifier == null) 
				throw new InvalidCommandArgument(this.messageService.getMessage(JOB_ADDED_NOTIFIER_NOT_FOUND)
						.inject(Placeholders.JOB_ADDED_NOTIFIER, notifierName)
						.first(), false);

			return notifier;
		});
		
		commandManager.getCommandContexts().registerIssuerOnlyContext(List.class, context -> 
		{
			if(!context.hasFlag("Jobs Able To Delete"))
				return null;
			
			Player player = context.getPlayer();
			
			return player.hasPermission("employme.admin.delete") ? this.globalJobBoard.getOfferedJobs() : this.globalJobBoard.getJobsOfferedBy(player.getUniqueId());
		});

		//register commands
		InventoryBoardDisplayer inventoryBoardDisplayer = new InventoryBoardDisplayer(Job.ORDER_BY_GOAL_NAME, this.jobService, this.messageService, this.jobIconFactory);
		
		commandManager.registerCommand(new EmploymentCommand(this.globalJobBoard, this.playerContainerService, this.jobSubscriptionService, this.jobAddedNotifierService, this.messageService, inventoryBoardDisplayer, this.economy, this.jobIconFactory));
	}
}