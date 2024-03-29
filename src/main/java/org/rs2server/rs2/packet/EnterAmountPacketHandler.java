package org.rs2server.rs2.packet;

import org.rs2server.Server;
import org.rs2server.cache.format.CacheItemDefinition;
import org.rs2server.rs2.content.api.bank.BankPinEvent;
import org.rs2server.rs2.domain.service.api.BankPinService;
import org.rs2server.rs2.domain.service.api.HookService;
import org.rs2server.rs2.domain.service.api.content.ItemService;
import org.rs2server.rs2.domain.service.api.content.magic.OrbChargingService;
import org.rs2server.rs2.domain.service.impl.BankPinServiceImpl;
import org.rs2server.rs2.model.Item;
import org.rs2server.rs2.model.Skills;
import org.rs2server.rs2.model.player.Player;
import org.rs2server.rs2.model.skills.Cooking;
import org.rs2server.rs2.model.skills.Cooking.CookingItem;
import org.rs2server.rs2.model.skills.Cooking.CookingMethod;
import org.rs2server.rs2.model.skills.FletchingAction;
import org.rs2server.rs2.model.skills.FletchingAction.FletchingItem;
import org.rs2server.rs2.model.skills.herblore.Herblore;
import org.rs2server.rs2.model.skills.herblore.Herblore.HerbloreType;
import org.rs2server.rs2.model.skills.herblore.Herblore.PrimaryIngredient;
import org.rs2server.rs2.model.skills.herblore.Herblore.SecondaryIngredient;
import org.rs2server.rs2.model.skills.PestleAndMortar;
import org.rs2server.rs2.model.skills.crafting.BoltCrafting;
import org.rs2server.rs2.model.skills.crafting.GemCutting;
import org.rs2server.rs2.model.skills.crafting.LeatherCrafting.LeatherProduction;
import org.rs2server.rs2.model.skills.crafting.LeatherCrafting.LeatherProductionAction;
import org.rs2server.rs2.model.skills.smithing.Forging;
import org.rs2server.rs2.model.skills.smithing.Smelting;
import org.rs2server.rs2.model.skills.smithing.SmithingUtils;
import org.rs2server.rs2.model.skills.smithing.SmithingUtils.ForgingBar;
import org.rs2server.rs2.model.skills.smithing.SmithingUtils.SmeltingBar;
import org.rs2server.rs2.net.ActionSender.DialogueType;
import org.rs2server.rs2.net.Packet;
import org.rs2server.rs2.util.Misc;

/**
 * A packet sent when the player enters a custom amount for banking etc.
 *
 * @author Graham Edgecombe
 */
public class EnterAmountPacketHandler implements PacketHandler {

	private final HookService hookService;
	private final BankPinService bankPinService;
	private final ItemService itemService;

	public EnterAmountPacketHandler() {
		hookService = Server.getInjector().getInstance(HookService.class);
		bankPinService = Server.getInjector().getInstance(BankPinService.class);
		itemService = Server.getInjector().getInstance(ItemService.class);
	}

	@Override
	public void handle(Player player, Packet packet) {
		int amount = packet.getInt();

		if (amount == 0) {
			player.getActionSender().removeChatboxInterface();
			return;
		}
		if (player.getAttribute("cutScene") != null) {
			return;
		}

		if (player.getInterfaceState().isInterfaceOpen(BankPinServiceImpl.BANK_PIN_WIDGET)) {
			if (amount == 12345) {
				player.getActionSender().removeInterface();
				bankPinService.onClose(player);
				return;
			}
			hookService.post(new BankPinEvent(player, amount));
			return;
		}

		if (player.getInterfaceAttribute("gamble_firecape") != null) {
			player.removeInterfaceAttribute("gamble_firecape");
			itemService.gambleFireCapes(player, amount);
			return;
		}
		if (player.getInterfaceAttribute("cookItem") != null && player.getInterfaceAttribute("cookMethod") != null) {
			player.getActionSender().removeChatboxInterface();
			if (amount > 0) {
				player.getActionQueue().addAction(new Cooking(player, 2, amount, (CookingItem) player.getInterfaceAttribute("cookItem"), (CookingMethod) player.getInterfaceAttribute("cookMethod")));
				player.removeInterfaceAttribute("cookItem");
				player.removeInterfaceAttribute("cookMethod");
			}
		} else if (player.getInterfaceAttribute("pestle_type") != null) {
			player.getActionSender().removeChatboxInterface();
			if (amount > 0) {
				player.getActionQueue().addAction(new PestleAndMortar(player, player.getInterfaceAttribute("pestle_type"), amount));
				player.removeInterfaceAttribute("pestle_type");
			}
		} else if (player.getInterfaceAttribute("staff_type") != null) {
			player.getActionSender().removeChatboxInterface();
			if (amount > 0) {
				player.getActionQueue().addAction(new OrbChargingService.BattleStaffAction(player, player.getInterfaceAttribute("staff_type"), amount));
				player.removeInterfaceAttribute("staff_type");
			}
		} else if (player.getInterfaceAttribute("orb_type") != null) {
			player.getActionSender().removeChatboxInterface();
			if (amount > 0) {
				player.getActionQueue().addAction(new OrbChargingService.OrbChargingAction(player, player.getInterfaceAttribute("orb_type"), amount));
				player.removeInterfaceAttribute("orb_type");
			}
		} else if (player.getInterfaceAttribute("smithingIndex") != null && player.getInterfaceAttribute("smithingBar") != null) {
			ForgingBar bar = player.getInterfaceAttribute("smithingBar");
			int clickedChild = player.getInterfaceAttribute("smithingIndex");
			int levelRequired = bar.getBaseLevel() + SmithingUtils.getLevelIncrement(bar, bar.getItems()[clickedChild]);
			int barsRequired = SmithingUtils.getBarAmount(levelRequired, bar, bar.getItems()[clickedChild]);
			String itemName = Misc.withPrefix(CacheItemDefinition.get(bar.getItems()[clickedChild]).getName().toLowerCase());
			if (!player.getInventory().hasItem(new Item(bar.getBarId(), barsRequired))) {
				String barName = bar.toString().toLowerCase();
				player.getActionSender().sendDialogue("", DialogueType.MESSAGE, -1, null, "You do not have enough " + barName + " bars to smith " + itemName + ".");
				player.getActionSender().removeInterface2();
				return;
			}
			if (player.getSkills().getLevel(Skills.SMITHING) < levelRequired) {
				player.getActionSender().sendDialogue("", DialogueType.MESSAGE, -1, null, "You need a Smithing level of at least " + levelRequired + " to make " + itemName + ".");
				player.getActionSender().removeInterface2();
				return;
			}
			player.getActionSender().removeAllInterfaces();
			player.removeInterfaceAttribute("smithingBar");
			player.removeInterfaceAttribute("smithingIndex");
			player.getActionQueue().addAction(new Forging(player, bar, amount, clickedChild));
		} else if (player.getInterfaceAttribute("herblore_index") != null && player.getInterfaceAttribute("herblore_type") != null) {
			switch ((HerbloreType) player.getInterfaceAttribute("herblore_type")) {
				case PRIMARY_INGREDIENT:
					player.getActionQueue().addAction(new Herblore(player, amount, PrimaryIngredient.forId((Integer) player.getInterfaceAttribute("herblore_index")), null, HerbloreType.PRIMARY_INGREDIENT));
					break;
				case SECONDARY_INGREDIENT:
					player.getActionQueue().addAction(new Herblore(player, amount, null, SecondaryIngredient.forId((Integer) player.getInterfaceAttribute("herblore_index")), HerbloreType.SECONDARY_INGREDIENT));
					break;
			}
			player.removeInterfaceAttribute("herblore_index");
			player.removeInterfaceAttribute("herblore_type");
		} else if (player.getAttribute("production") != null) {
			LeatherProduction toProduce = player.getAttribute("production");
			if (toProduce != null) {
				LeatherProductionAction produceAction = new LeatherProductionAction(player, toProduce, amount);
				produceAction.execute();
				if (produceAction.isRunning()) {
					player.submitTick("skill_action_tick", produceAction, true);
				}
			}
			player.removeAttribute("craftingType");
			player.removeAttribute("dhide_type");
			player.removeAttribute("production");
		} else if (player.getInterfaceAttribute("gem_index") != null && player.getInterfaceAttribute("gem_type") != null) {
			player.getActionQueue().addAction(new GemCutting(player, player.getInterfaceAttribute("gem_type"), amount));
			player.getActionSender().removeInterfaces(162, 546);
			player.removeInterfaceAttribute("gem_index");
			player.removeInterfaceAttribute("gem_type");
		} else if (player.getInterfaceAttribute("tip_index") != null && player.getInterfaceAttribute("tip_type") != null) {
			player.getActionQueue().addAction(new BoltCrafting(player, player.getInterfaceAttribute("tip_type"), amount));
			player.getActionSender().removeInterfaces(162, 546);
			player.removeInterfaceAttribute("tip_index");
			player.removeInterfaceAttribute("tip_type");
		} else if (player.getAttribute("smelting_bar") != null) {
			SmeltingBar bar = (SmeltingBar) player.getAttribute("smelting_bar");
			player.getActionQueue().addAction(new Smelting(player, bar, amount));
			player.removeAttribute("smelting_bar");
		} else if (player.getInterfaceAttribute("fletch_item") != null) {
			FletchingItem item = (FletchingItem) player.getInterfaceAttribute("fletch_item");
			if (item != null) {
				player.getActionQueue().addAction(new FletchingAction(player, amount, item));
			}
			player.removeInterfaceAttribute("fletch_item");
		} else if (player.getInterfaceAttribute("toProd") != null) {
			LeatherProduction toProduce = player.getInterfaceAttribute("toProd");
			if (toProduce != null) {
				LeatherProductionAction produceAction = new LeatherProductionAction(player, toProduce, amount);
				produceAction.execute();
				if (produceAction.isRunning()) {
					player.submitTick("skill_action_tick", produceAction, true);
				}
				player.getActionSender().removeInterface2();
			}
		}
		if (player.getInterfaceState().isEnterAmountInterfaceOpen()) {
			player.getInterfaceState().closeEnterAmountInterface(amount);
		}
	}

}
