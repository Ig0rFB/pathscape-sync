package com.pathscapesync;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * HTTPS client for the PathScape ingest Edge Function. Never talks to Postgres.
 */
@Slf4j
final class PathScapeSyncClient
{
	static final String INGEST_URL = "https://eczmozhetrhgdlesbkqp.supabase.co/functions/v1/pathscape-sync";

	private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
	private static final int MAX_BODY_CHARS = 4096;

	private final OkHttpClient http;
	private final Gson gson;

	PathScapeSyncClient(OkHttpClient http, Gson gson)
	{
		this.http = http;
		this.gson = gson;
	}

	void pair(String apiKey, String codeOrToken, String claimedRsn, Consumer<PairResult> callback)
	{
		JsonObject body = new JsonObject();
		body.addProperty("action", "pair");
		// Link tokens are 64 hex chars; legacy codes are short uppercase. Edge Function routes both.
		String trimmed = codeOrToken == null ? "" : codeOrToken.trim();
		if (trimmed.matches("(?i)[0-9a-f]{64}"))
		{
			body.addProperty("token", trimmed.toLowerCase());
			body.addProperty("code", trimmed.toLowerCase());
		}
		else
		{
			body.addProperty("code", trimmed.toUpperCase());
		}
		body.addProperty("claimedRsn", claimedRsn);
		enqueue(apiKey, null, body, (ok, json, error) ->
		{
			if (!ok)
			{
				callback.accept(PairResult.fail(error));
				return;
			}
			String token = json.has("token") && !json.get("token").isJsonNull()
				? json.get("token").getAsString()
				: "";
			if (token.isEmpty())
			{
				callback.accept(PairResult.fail("Pairing response had no token"));
				return;
			}
			callback.accept(PairResult.ok(token));
		});
	}

	void postSnapshot(String apiKey, String deviceToken, JsonObject payload, Consumer<String> errorOrNull)
	{
		payload.addProperty("action", "snapshot");
		enqueue(apiKey, deviceToken, payload, (ok, json, error) ->
		{
			if (!ok)
			{
				errorOrNull.accept(error);
				return;
			}
			errorOrNull.accept(null);
		});
	}

	private void enqueue(String apiKey, String deviceToken, JsonObject body, ResponseHandler handler)
	{
		String json = gson.toJson(body);
		Request.Builder builder = new Request.Builder()
			.url(INGEST_URL)
			.post(RequestBody.create(JSON, json))
			.header("Content-Type", "application/json");

		if (apiKey != null && !apiKey.isEmpty())
		{
			builder.header("apikey", apiKey);
		}

		// Gateway often wants a JWT-shaped Authorization; the device token is the real proof.
		if (deviceToken != null && !deviceToken.isEmpty())
		{
			builder.header("Authorization", "Bearer " + (apiKey != null && !apiKey.isEmpty() ? apiKey : deviceToken));
			builder.header("X-Pathscape-Token", deviceToken);
		}
		else if (apiKey != null && !apiKey.isEmpty())
		{
			builder.header("Authorization", "Bearer " + apiKey);
		}

		http.newCall(builder.build()).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.debug("PathScape Sync request failed: {}", e.toString());
				handler.handle(false, null, e.getMessage() != null ? e.getMessage() : "Network error");
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				try (ResponseBody responseBody = response.body())
				{
					String text = responseBody != null ? responseBody.string() : "";
					if (text.length() > MAX_BODY_CHARS)
					{
						text = text.substring(0, MAX_BODY_CHARS);
					}
					JsonObject parsed = parseObject(gson, text);
					if (!response.isSuccessful())
					{
						String error = messageFrom(parsed, "HTTP " + response.code());
						log.debug("PathScape Sync HTTP {}: {}", response.code(), error);
						handler.handle(false, parsed, error);
						return;
					}
					handler.handle(true, parsed, null);
				}
				catch (IOException e)
				{
					handler.handle(false, null, e.getMessage() != null ? e.getMessage() : "Read error");
				}
			}
		});
	}

	private static JsonObject parseObject(Gson gson, String text)
	{
		if (text == null || text.isEmpty())
		{
			return new JsonObject();
		}
		try
		{
			JsonObject parsed = gson.fromJson(text, JsonObject.class);
			return parsed != null ? parsed : new JsonObject();
		}
		catch (RuntimeException e)
		{
			return new JsonObject();
		}
	}

	private static String messageFrom(JsonObject json, String fallback)
	{
		if (json.has("error") && !json.get("error").isJsonNull())
		{
			return json.get("error").getAsString();
		}
		return fallback;
	}

	interface ResponseHandler
	{
		void handle(boolean ok, JsonObject json, String error);
	}

	static final class PairResult
	{
		final boolean ok;
		final String token;
		final String error;

		private PairResult(boolean ok, String token, String error)
		{
			this.ok = ok;
			this.token = token;
			this.error = error;
		}

		static PairResult ok(String token)
		{
			return new PairResult(true, token, null);
		}

		static PairResult fail(String error)
		{
			return new PairResult(false, null, error);
		}
	}
}
