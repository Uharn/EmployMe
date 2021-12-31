package dte.employme.messages;

public enum MessageKey
{
	//Jobs
	JOB_ADDED_TO_BOARD,
	JOB_COMPLETED,
	ITEMS_JOB_COMPLETED,
	JOB_SUCCESSFULLY_DELETED,
	PLAYER_COMPLETED_YOUR_JOB,
	ITEMS_JOB_NO_ITEMS_WARNING,
	NEW_JOB_POSTED,
	GLOBAL_JOB_BOARD_IS_FULL,
	
	//Job Added Notifiers
	JOB_ADDED_NOTIFIER_NOT_FOUND,
	YOUR_NEW_JOB_ADDED_NOTIFIER_IS,
	THE_JOB_ADDED_NOTIFIERS_ARE,
	
	//Subscriptions
	SUCCESSFULLY_SUBSCRIBED_TO_GOAL,
	SUCCESSFULLY_UNSUBSCRIBED_FROM_GOAL,
	SUBSCRIBED_TO_GOALS_NOTIFICATION,
	MUST_BE_SUBSCRIBED_TO_GOAL,
	YOUR_SUBSCRIPTIONS_ARE,
	
	//Rewards
	MONEY_PAYMENT_AMOUNT_QUESTION,

	//Goals
	ITEM_GOAL_FORMAT_QUESTION,
	ITEM_GOAL_INVALID,

	//Rewards
	MONEY_REWARD_ERROR_NEGATIVE,
	MONEY_REWARD_NOT_ENOUGH,
	MONEY_REWARD_NOT_A_NUMBER,
	
	//Enchantments
	ENTER_ENCHANTMENT_LEVEL,
	ENCHANTMENT_LEVEL_NOT_A_NUMBER,
	ENCHANTMENT_LEVEL_OUT_OF_BOUNDS,

	//General
	MUST_NOT_BE_CONVERSING,
	MUST_HAVE_JOBS,
	MATERIAL_NOT_FOUND,
	NONE,
	NEW_UPDATE_AVAILABLE;
}