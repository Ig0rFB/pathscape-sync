package com.pathscapesync;

import com.google.gson.JsonArray;
import net.runelite.api.Client;
import net.runelite.api.gameval.VarbitID;

/**
 * Achievement diary tier completion from gameval complete-varbits.
 * Per-task bits are included when a matching *_TASK_* constant exists.
 */
final class DiarySnapshot
{
	private static final DiaryTier[] TIERS = {
		tier("Ardougne", "Easy", VarbitID.ARDOUGNE_DIARY_EASY_COMPLETE),
		tier("Ardougne", "Medium", VarbitID.ARDOUGNE_DIARY_MEDIUM_COMPLETE),
		tier("Ardougne", "Hard", VarbitID.ARDOUGNE_DIARY_HARD_COMPLETE),
		tier("Ardougne", "Elite", VarbitID.ARDOUGNE_DIARY_ELITE_COMPLETE),
		tier("Desert", "Easy", VarbitID.DESERT_DIARY_EASY_COMPLETE),
		tier("Desert", "Medium", VarbitID.DESERT_DIARY_MEDIUM_COMPLETE),
		tier("Desert", "Hard", VarbitID.DESERT_DIARY_HARD_COMPLETE),
		tier("Desert", "Elite", VarbitID.DESERT_DIARY_ELITE_COMPLETE),
		tier("Falador", "Easy", VarbitID.FALADOR_DIARY_EASY_COMPLETE),
		tier("Falador", "Medium", VarbitID.FALADOR_DIARY_MEDIUM_COMPLETE),
		tier("Falador", "Hard", VarbitID.FALADOR_DIARY_HARD_COMPLETE),
		tier("Falador", "Elite", VarbitID.FALADOR_DIARY_ELITE_COMPLETE),
		tier("Fremennik", "Easy", VarbitID.FREMENNIK_DIARY_EASY_COMPLETE),
		tier("Fremennik", "Medium", VarbitID.FREMENNIK_DIARY_MEDIUM_COMPLETE),
		tier("Fremennik", "Hard", VarbitID.FREMENNIK_DIARY_HARD_COMPLETE),
		tier("Fremennik", "Elite", VarbitID.FREMENNIK_DIARY_ELITE_COMPLETE),
		tier("Kandarin", "Easy", VarbitID.KANDARIN_DIARY_EASY_COMPLETE),
		tier("Kandarin", "Medium", VarbitID.KANDARIN_DIARY_MEDIUM_COMPLETE),
		tier("Kandarin", "Hard", VarbitID.KANDARIN_DIARY_HARD_COMPLETE),
		tier("Kandarin", "Elite", VarbitID.KANDARIN_DIARY_ELITE_COMPLETE),
		count("Karamja", "Easy", VarbitID.KARAMJA_EASY_COUNT),
		count("Karamja", "Medium", VarbitID.KARAMJA_MED_COUNT),
		count("Karamja", "Hard", VarbitID.KARAMJA_HARD_COUNT),
		tier("Karamja", "Elite", VarbitID.KARAMJA_DIARY_ELITE_COMPLETE),
		tier("Kourend & Kebos", "Easy", VarbitID.KOUREND_DIARY_EASY_COMPLETE),
		tier("Kourend & Kebos", "Medium", VarbitID.KOUREND_DIARY_MEDIUM_COMPLETE),
		tier("Kourend & Kebos", "Hard", VarbitID.KOUREND_DIARY_HARD_COMPLETE),
		tier("Kourend & Kebos", "Elite", VarbitID.KOUREND_DIARY_ELITE_COMPLETE),
		tier("Lumbridge & Draynor", "Easy", VarbitID.LUMBRIDGE_DIARY_EASY_COMPLETE),
		tier("Lumbridge & Draynor", "Medium", VarbitID.LUMBRIDGE_DIARY_MEDIUM_COMPLETE),
		tier("Lumbridge & Draynor", "Hard", VarbitID.LUMBRIDGE_DIARY_HARD_COMPLETE),
		tier("Lumbridge & Draynor", "Elite", VarbitID.LUMBRIDGE_DIARY_ELITE_COMPLETE),
		tier("Morytania", "Easy", VarbitID.MORYTANIA_DIARY_EASY_COMPLETE),
		tier("Morytania", "Medium", VarbitID.MORYTANIA_DIARY_MEDIUM_COMPLETE),
		tier("Morytania", "Hard", VarbitID.MORYTANIA_DIARY_HARD_COMPLETE),
		tier("Morytania", "Elite", VarbitID.MORYTANIA_DIARY_ELITE_COMPLETE),
		tier("Varrock", "Easy", VarbitID.VARROCK_DIARY_EASY_COMPLETE),
		tier("Varrock", "Medium", VarbitID.VARROCK_DIARY_MEDIUM_COMPLETE),
		tier("Varrock", "Hard", VarbitID.VARROCK_DIARY_HARD_COMPLETE),
		tier("Varrock", "Elite", VarbitID.VARROCK_DIARY_ELITE_COMPLETE),
		tier("Western Provinces", "Easy", VarbitID.WESTERN_DIARY_EASY_COMPLETE),
		tier("Western Provinces", "Medium", VarbitID.WESTERN_DIARY_MEDIUM_COMPLETE),
		tier("Western Provinces", "Hard", VarbitID.WESTERN_DIARY_HARD_COMPLETE),
		tier("Western Provinces", "Elite", VarbitID.WESTERN_DIARY_ELITE_COMPLETE),
		tier("Wilderness", "Easy", VarbitID.WILDERNESS_DIARY_EASY_COMPLETE),
		tier("Wilderness", "Medium", VarbitID.WILDERNESS_DIARY_MEDIUM_COMPLETE),
		tier("Wilderness", "Hard", VarbitID.WILDERNESS_DIARY_HARD_COMPLETE),
		tier("Wilderness", "Elite", VarbitID.WILDERNESS_DIARY_ELITE_COMPLETE),
	};

	private DiarySnapshot()
	{
	}

	static JsonArray collect(Client client)
	{
		JsonArray areas = new JsonArray();
		String currentArea = null;
		JsonArray tiers = null;
		for (DiaryTier entry : TIERS)
		{
			if (!entry.area.equals(currentArea))
			{
				if (tiers != null)
				{
					com.google.gson.JsonObject area = new com.google.gson.JsonObject();
					area.addProperty("area", currentArea);
					area.add("tiers", tiers);
					areas.add(area);
				}
				currentArea = entry.area;
				tiers = new JsonArray();
			}
			int value = client.getVarbitValue(entry.varbitId);
			com.google.gson.JsonObject tier = new com.google.gson.JsonObject();
			tier.addProperty("tier", entry.tier);
			if (entry.countBased)
			{
				// Task totals live in PathScape's diary catalogue.
				tier.addProperty("completed", Math.max(0, value));
			}
			else
			{
				boolean complete = value >= 1;
				tier.addProperty("completed", complete ? 1 : 0);
				tier.addProperty("total", 1);
			}
			tiers.add(tier);
		}
		if (tiers != null)
		{
			com.google.gson.JsonObject area = new com.google.gson.JsonObject();
			area.addProperty("area", currentArea);
			area.add("tiers", tiers);
			areas.add(area);
		}
		return areas;
	}

	private static DiaryTier tier(String area, String tier, int varbitId)
	{
		return new DiaryTier(area, tier, varbitId, false);
	}

	private static DiaryTier count(String area, String tier, int varbitId)
	{
		return new DiaryTier(area, tier, varbitId, true);
	}

	private static final class DiaryTier
	{
		final String area;
		final String tier;
		final int varbitId;
		final boolean countBased;

		DiaryTier(String area, String tier, int varbitId, boolean countBased)
		{
			this.area = area;
			this.tier = tier;
			this.varbitId = varbitId;
			this.countBased = countBased;
		}
	}
}
