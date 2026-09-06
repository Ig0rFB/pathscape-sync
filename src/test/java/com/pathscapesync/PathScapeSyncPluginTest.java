package com.pathscapesync;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class PathScapeSyncPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(PathScapeSyncPlugin.class);
		RuneLite.main(args);
	}
}
