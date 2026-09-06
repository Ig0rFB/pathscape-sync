package com.pathscapesync;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.runelite.api.Client;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.client.game.ItemManager;

/**
 * Builds a schemaVersion 1 snapshot from the logged-in local player only.
 */
final class SnapshotCollector
{
	private static final int[] CA_TASK_VARPS = {
		VarPlayerID.CA_TASK_COMPLETED_0,
		VarPlayerID.CA_TASK_COMPLETED_1,
		VarPlayerID.CA_TASK_COMPLETED_2,
		VarPlayerID.CA_TASK_COMPLETED_3,
		VarPlayerID.CA_TASK_COMPLETED_4,
		VarPlayerID.CA_TASK_COMPLETED_5,
		VarPlayerID.CA_TASK_COMPLETED_6,
		VarPlayerID.CA_TASK_COMPLETED_7,
		VarPlayerID.CA_TASK_COMPLETED_8,
		VarPlayerID.CA_TASK_COMPLETED_9,
		VarPlayerID.CA_TASK_COMPLETED_10,
		VarPlayerID.CA_TASK_COMPLETED_11,
		VarPlayerID.CA_TASK_COMPLETED_12,
		VarPlayerID.CA_TASK_COMPLETED_13,
		VarPlayerID.CA_TASK_COMPLETED_14,
		VarPlayerID.CA_TASK_COMPLETED_15,
		VarPlayerID.CA_TASK_COMPLETED_16,
		VarPlayerID.CA_TASK_COMPLETED_17,
		VarPlayerID.CA_TASK_COMPLETED_18,
		VarPlayerID.CA_TASK_COMPLETED_19,
		VarPlayerID.CA_TASK_COMPLETED_20,
	};

	private final Client client;
	private final ItemManager itemManager;

	SnapshotCollector(Client client, ItemManager itemManager)
	{
		this.client = client;
		this.itemManager = itemManager;
	}

	JsonObject collect(String rsn)
	{
		JsonObject root = new JsonObject();
		root.addProperty("schemaVersion", 1);
		root.addProperty("rsn", rsn);

		JsonArray kinds = new JsonArray();
		kinds.add("skills");
		kinds.add("quests");
		kinds.add("diaries");
		kinds.add("combatAchievements");
		kinds.add("bossKc");
		kinds.add("equipment");
		root.add("kinds", kinds);

		root.add("skills", collectSkills());
		root.add("quests", collectQuests());
		root.add("diaries", DiarySnapshot.collect(client));
		root.add("combatAchievements", collectCombatAchievements());
		root.add("bossKc", BossKillSnapshot.collect(client));
		root.add("equipment", collectEquipment());
		return root;
	}

	private JsonArray collectSkills()
	{
		JsonArray skills = new JsonArray();
		for (Skill skill : Skill.values())
		{
			if (skill == Skill.OVERALL)
			{
				continue;
			}
			JsonObject row = new JsonObject();
			row.addProperty("name", skill.getName());
			row.addProperty("level", client.getRealSkillLevel(skill));
			row.addProperty("xp", client.getSkillExperience(skill));
			skills.add(row);
		}
		return skills;
	}

	private JsonArray collectQuests()
	{
		JsonArray quests = new JsonArray();
		for (Quest quest : Quest.values())
		{
			QuestState state = quest.getState(client);
			JsonObject row = new JsonObject();
			row.addProperty("id", quest.getId());
			row.addProperty("name", quest.getName());
			row.addProperty("state", toRuneProfileState(state));
			quests.add(row);
		}
		return quests;
	}

	private JsonObject collectCombatAchievements()
	{
		JsonArray ids = new JsonArray();
		for (int varpIndex = 0; varpIndex < CA_TASK_VARPS.length; varpIndex++)
		{
			int value = client.getVarpValue(CA_TASK_VARPS[varpIndex]);
			for (int bit = 0; bit < 32; bit++)
			{
				if (((value >> bit) & 1) == 1)
				{
					ids.add(varpIndex * 32 + bit);
				}
			}
		}
		JsonObject wrap = new JsonObject();
		wrap.add("completedTaskIds", ids);
		return wrap;
	}

	private JsonObject collectEquipment()
	{
		JsonObject equipment = new JsonObject();
		ItemContainer container = client.getItemContainer(InventoryID.EQUIPMENT);
		addSlot(equipment, container, EquipmentInventorySlot.HEAD, "head");
		addSlot(equipment, container, EquipmentInventorySlot.CAPE, "cape");
		addSlot(equipment, container, EquipmentInventorySlot.AMULET, "amulet");
		addSlot(equipment, container, EquipmentInventorySlot.WEAPON, "weapon");
		addSlot(equipment, container, EquipmentInventorySlot.BODY, "body");
		addSlot(equipment, container, EquipmentInventorySlot.SHIELD, "shield");
		addSlot(equipment, container, EquipmentInventorySlot.LEGS, "legs");
		addSlot(equipment, container, EquipmentInventorySlot.GLOVES, "gloves");
		addSlot(equipment, container, EquipmentInventorySlot.BOOTS, "boots");
		addSlot(equipment, container, EquipmentInventorySlot.RING, "ring");
		addSlot(equipment, container, EquipmentInventorySlot.AMMO, "ammo");
		return equipment;
	}

	private void addSlot(JsonObject equipment, ItemContainer container, EquipmentInventorySlot slot, String id)
	{
		if (container == null)
		{
			return;
		}
		Item item = container.getItem(slot.getSlotIdx());
		if (item == null || item.getId() <= 0)
		{
			return;
		}
		JsonObject row = new JsonObject();
		row.addProperty("id", item.getId());
		row.addProperty("name", itemManager.getItemComposition(item.getId()).getName());
		row.addProperty("quantity", item.getQuantity());
		equipment.add(id, row);
	}

	private static String toRuneProfileState(QuestState state)
	{
		if (state == QuestState.FINISHED)
		{
			return "finished";
		}
		if (state == QuestState.IN_PROGRESS)
		{
			return "in_progress";
		}
		return "not_started";
	}
}
