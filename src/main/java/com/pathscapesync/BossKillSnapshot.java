package com.pathscapesync;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.runelite.api.Client;
import net.runelite.api.gameval.VarPlayerID;

/**
 * Live boss kill counts from varplayers, keyed to Jagex hiscores activity names.
 */
final class BossKillSnapshot
{
	private static final BossVarp[] BOSSES = {
		boss("Abyssal Sire", VarPlayerID.TOTAL_ABYSSALSIRE_KILLS),
		boss("Alchemical Hydra", VarPlayerID.TOTAL_HYDRABOSS_KILLS),
		boss("Amoxliatl", VarPlayerID.TOTAL_AMOXLIATL_KILLS),
		boss("Araxxor", VarPlayerID.TOTAL_ARAXXOR_KILLS),
		boss("Artio", VarPlayerID.TOTAL_ARTIO_KILLS),
		boss("Barrows Chests", VarPlayerID.TOTAL_BARROWS_CHESTS),
		boss("Bryophyta", VarPlayerID.TOTAL_BRYOPHYTA_KILLS),
		boss("Callisto", VarPlayerID.TOTAL_CALLISTO_KILLS),
		boss("Calvar'ion", VarPlayerID.TOTAL_CALVARION_KILLS),
		boss("Cerberus", VarPlayerID.TOTAL_CERBERUS_KILLS),
		boss("Chambers of Xeric", VarPlayerID.TOTAL_COMPLETED_XERICCHAMBERS),
		boss("Chambers of Xeric: Challenge Mode", VarPlayerID.TOTAL_COMPLETED_XERICCHAMBERS_CHALLENGE),
		boss("Chaos Elemental", VarPlayerID.TOTAL_CHAOSELE_KILLS),
		boss("Chaos Fanatic", VarPlayerID.TOTAL_CHAOSFANATIC_KILLS),
		boss("Commander Zilyana", VarPlayerID.TOTAL_SARADOMIN_KILLS),
		boss("Corporeal Beast", VarPlayerID.TOTAL_CORP_KILLS),
		boss("Crazy Archaeologist", VarPlayerID.TOTAL_CRAZYARCHAEOLOGIST_KILLS),
		boss("Dagannoth Prime", VarPlayerID.TOTAL_PRIME_KILLS),
		boss("Dagannoth Rex", VarPlayerID.TOTAL_REX_KILLS),
		boss("Dagannoth Supreme", VarPlayerID.TOTAL_SUPREME_KILLS),
		boss("Deranged Archaeologist", VarPlayerID.TOTAL_DERANGEDARCHAEOLOGIST_KILLS),
		boss("Duke Sucellus", VarPlayerID.TOTAL_DUKE_SUCELLUS_KILLS),
		boss("General Graardor", VarPlayerID.TOTAL_BANDOS_KILLS),
		boss("Giant Mole", VarPlayerID.TOTAL_MOLE_KILLS),
		boss("Grotesque Guardians", VarPlayerID.TOTAL_GARGBOSS_KILLS),
		boss("Hespori", VarPlayerID.TOTAL_HESPORI_KILLS),
		boss("Kalphite Queen", VarPlayerID.TOTAL_KALPHITE_KILLS),
		boss("King Black Dragon", VarPlayerID.TOTAL_KBD_KILLS),
		boss("Kraken", VarPlayerID.TOTAL_KRAKEN_BOSS_KILLS),
		boss("Kree'Arra", VarPlayerID.TOTAL_ARMADYL_KILLS),
		boss("K'ril Tsutsaroth", VarPlayerID.TOTAL_ZAMORAK_KILLS),
		boss("Nex", VarPlayerID.TOTAL_NEX_KILLS),
		boss("Nightmare", VarPlayerID.TOTAL_NIGHTMARE_KILLS),
		boss("Phosani's Nightmare", VarPlayerID.TOTAL_NIGHTMARE_CHALLENGE_KILLS),
		boss("Obor", VarPlayerID.TOTAL_HILLGIANT_BOSS_KILLS),
		boss("Phantom Muspah", VarPlayerID.TOTAL_MUSPAH_KILLS),
		boss("Sarachnis", VarPlayerID.TOTAL_SARACHNIS_KILLS),
		boss("Scorpia", VarPlayerID.TOTAL_SCORPIA_KILLS),
		boss("Scurrius", VarPlayerID.TOTAL_RAT_BOSS_KILLS),
		boss("Shellbane Gryphon", VarPlayerID.TOTAL_GRYPHON_BOSS_KILLS),
		boss("Skotizo", VarPlayerID.TOTAL_CATA_BOSS_KILLS),
		boss("Sol Heredit", VarPlayerID.TOTAL_SOL_KILLS),
		boss("Spindel", VarPlayerID.TOTAL_SPINDEL_KILLS),
		boss("Tempoross", VarPlayerID.TOTAL_TEMPOROSS_KILLS),
		boss("The Gauntlet", VarPlayerID.TOTAL_COMPLETED_GAUNTLET),
		boss("The Corrupted Gauntlet", VarPlayerID.TOTAL_COMPLETED_GAUNTLET_HM),
		boss("The Hueycoatl", VarPlayerID.TOTAL_HUEY_KILLS),
		boss("The Leviathan", VarPlayerID.TOTAL_LEVIATHAN_KILLS),
		boss("The Royal Titans", VarPlayerID.TOTAL_ROYAL_TITAN_KILLS),
		boss("The Whisperer", VarPlayerID.TOTAL_WHISPERER_KILLS),
		boss("Theatre of Blood", VarPlayerID.TOTAL_COMPLETED_THEATREOFBLOOD),
		boss("Theatre of Blood: Hard Mode", VarPlayerID.TOTAL_COMPLETED_THEATREOFBLOOD_HARD),
		boss("Thermonuclear Smoke Devil", VarPlayerID.TOTAL_THERMY_KILLS),
		boss("Tombs of Amascut", VarPlayerID.TOTAL_COMPLETED_TOMBSOFAMASCUT),
		boss("Tombs of Amascut: Expert Mode", VarPlayerID.TOTAL_COMPLETED_TOMBSOFAMASCUT_EXPERT),
		boss("TzKal-Zuk", VarPlayerID.TOTAL_ZUK_KILLS),
		boss("TzTok-Jad", VarPlayerID.TOTAL_JAD_KILLS),
		boss("Vardorvis", VarPlayerID.TOTAL_VARDORVIS_KILLS),
		boss("Venenatis", VarPlayerID.TOTAL_VENENATIS_KILLS),
		boss("Vet'ion", VarPlayerID.TOTAL_VETION_KILLS),
		boss("Vorkath", VarPlayerID.TOTAL_VORKATH_KILLS),
		boss("Wintertodt", VarPlayerID.TOTAL_WINTERTODT_KILLS),
		boss("Yama", VarPlayerID.TOTAL_YAMA_KILLS),
		boss("Zalcano", VarPlayerID.TOTAL_ZALCANO_KILLS),
		boss("Zulrah", VarPlayerID.TOTAL_SNAKEBOSS_KILLS),
		boss("Mimic", VarPlayerID.TOTAL_MIMIC_KILLS),
		boss("Maggot King", VarPlayerID.TOTAL_MAGGOT_KING_KILLS),
	};

	private BossKillSnapshot()
	{
	}

	static JsonArray collect(Client client)
	{
		JsonArray rows = new JsonArray();
		for (BossVarp boss : BOSSES)
		{
			int score = client.getVarpValue(boss.varpId);
			JsonObject row = new JsonObject();
			row.addProperty("name", boss.hiscoresName);
			row.addProperty("score", Math.max(0, score));
			rows.add(row);
		}
		return rows;
	}

	private static BossVarp boss(String hiscoresName, int varpId)
	{
		return new BossVarp(hiscoresName, varpId);
	}

	private static final class BossVarp
	{
		final String hiscoresName;
		final int varpId;

		BossVarp(String hiscoresName, int varpId)
		{
			this.hiscoresName = hiscoresName;
			this.varpId = varpId;
		}
	}
}
