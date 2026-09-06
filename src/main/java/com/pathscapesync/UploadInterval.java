package com.pathscapesync;

/**
 * How often a full snapshot is posted after the login snapshot.
 */
public enum UploadInterval
{
	ONE_MINUTE(1),
	FIVE_MINUTES(5),
	TEN_MINUTES(10),
	SIXTY_MINUTES(60);

	private final int minutes;

	UploadInterval(int minutes)
	{
		this.minutes = minutes;
	}

	public int getMinutes()
	{
		return minutes;
	}

	@Override
	public String toString()
	{
		return minutes == 1 ? "1 minute" : minutes + " minutes";
	}
}
