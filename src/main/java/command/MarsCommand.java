package command;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.javacord.api.*;
import org.javacord.api.entity.*;
import org.javacord.api.event.*;
import org.javacord.api.listener.*;
import org.javacord.api.entity.message.*;
import org.javacord.api.event.message.*; // MessageCreateEvent
import org.javacord.api.listener.message.*; // MessageCreateListener
import org.javacord.api.entity.channel.*;
import org.javacord.api.entity.server.*;
import org.javacord.api.entity.user.*;
import org.javacord.api.entity.message.embed.*;

import java.util.Date;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.channels.FileChannel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.FileReader;
import java.io.File;
import java.io.FileOutputStream;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import java.lang.StringBuilder;

import java.awt.Color;

class InsightMetadata
{
	private long time_cached;

	public long getTimeCached() { return this.time_cached; }
	public void setTimeCached(long time) { this.time_cached = time; }
}

public class MarsCommand implements MessageCreateListener
{
	private ApiToken apiToken = null;
	private static final String CMD = "!wm.mars";

	private static long timeLastRequest = 0;
	private long minDelay = 4000; // milliseconds
	private long cacheRefreshInterval = (6 * 3600000); // 6 hours in milliseconds

	private static final String nasaAccessFileName = "src/main/resources/nasa_access.json";
	private static final String insightMetadataFileName = "src/main/resources/insight_metadata.json";
	private static final String insightDataFileName = "src/main/resources/insight_data.json";

	private static long timeCached = 0;
	private static boolean haveCached = false;
	byte[] cachedJSONData = null;

	/*
	 * We are limited to 1000 requests per hour.
	 */
	public MarsCommand()
	{
		try
		{
			ObjectMapper mapper = new ObjectMapper();
			byte[] data = Files.readAllBytes(Paths.get(nasaAccessFileName));

			apiToken = mapper.readValue(data, ApiToken.class);

			getCachedData();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(1);
		}
	}

	private boolean canSendRequest()
	{
		return ((System.currentTimeMillis() - timeLastRequest) >= minDelay);
	}

	private boolean shouldUseCachedData()
	{
		return haveCached && (System.currentTimeMillis() - timeCached) < cacheRefreshInterval;
	}

	private void cacheData(String data)
	{
		File mFile = new File(insightMetadataFileName);
		File dFile = new File(insightDataFileName);
		FileOutputStream dOut = null;
		FileOutputStream mOut = null;

		try
		{
			// if already exists, does nothing
			mFile.createNewFile();
			dFile.createNewFile();

			mOut = new FileOutputStream(mFile, false);
			dOut = new FileOutputStream(dFile, false);

			FileChannel mChan = mOut.getChannel();
			FileChannel dChan = dOut.getChannel();

			mChan.truncate(0);
			dChan.truncate(0);

			ObjectMapper mapper = new ObjectMapper();
			InsightMetadata meta = new InsightMetadata();

			timeCached = System.currentTimeMillis();
			haveCached = true;

			meta.setTimeCached(timeCached);

			mapper.writeValue(mFile, meta);
			dOut.write(data.getBytes("UTF-8"));
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	private void getCachedData()
	{
		try
		{
		/*
		 * Parse the time we received this cached data
		 * and determine whether it is stale or not.
		 * If it is not stale, read in the cached
		 * JSON data and we will satisfy requests with that.
		 */
			InsightMetadata meta = new InsightMetadata();
			byte[] data = Files.readAllBytes(Paths.get(insightMetadataFileName));
			ObjectMapper mapper = new ObjectMapper();

			meta = mapper.readValue(data, InsightMetadata.class);
			long time_cached = meta.getTimeCached();

			System.out.println("Got data cached " + (System.currentTimeMillis() - time_cached) + " milliseconds ago");

			if ((System.currentTimeMillis() - time_cached) >= cacheRefreshInterval)
			{
				cachedJSONData = null;
				haveCached = false;
				timeCached = 0;
			}
			else
			{
				cachedJSONData = Files.readAllBytes(Paths.get(insightDataFileName));
				haveCached = true;
				timeCached = time_cached;
			}
		}
		catch (Exception e)
		{
			return;
		}
	}

	@Override
	public void onMessageCreate(MessageCreateEvent event)
	{
		String messageContent = event.getMessageContent();

		if (messageContent.equals(CMD))
		{
			if (!canSendRequest())
			{
				event.getChannel().sendMessage("Min delay between requests == " + minDelay + " seconds");
				return;
			}

			try
			{
				BufferedReader buf = null;
				boolean isFreshData = false;
				byte[] jsonData = null;

				if (shouldUseCachedData())
				{
					jsonData = cachedJSONData;
					System.out.println("Using cached JSON data");
				}
				else
				{
					URL url = new URL(
						"https://api.nasa.gov/insight_weather/?api_key=" +
						apiToken.getToken() +
						"&feedtype=json&ver=1.0");

					System.out.println(
						"Sending GET request for:\n" +
						"https://api.nasa.gov/insight_weather/?api_key=" + apiToken.getToken() +
						"&feedtype=json&ver=1.0");

					HttpURLConnection conn = (HttpURLConnection)url.openConnection();
					conn.setRequestMethod("GET");
					conn.setRequestProperty("Accept", "application/json");

					System.out.println("Response code: " + conn.getResponseCode());

					if (200 != conn.getResponseCode())
					{
						System.err.println("Code: " + conn.getResponseCode());
						//sendErrorMessage();
						return;
					}

					timeLastRequest = System.currentTimeMillis();

					buf = new BufferedReader(new InputStreamReader(conn.getInputStream()));

					StringBuilder sBuilder = new StringBuilder();
					String line;

					while ((line = buf.readLine()) != null)
					{
						sBuilder.append(line);
					}

					cacheData(sBuilder.substring(0));
					jsonData = sBuilder.substring(0).getBytes("UTF-8");
				}

				ObjectMapper mapper = new ObjectMapper();
				JsonNode root = mapper.readTree(jsonData);

				System.out.println("Total JSON data: " + jsonData.length + " bytes");

				JsonNode sols = root.at("/validity_checks/sols_checked");
				Iterator<JsonNode> iter = sols.iterator();

				String sol = null;
				String season = null;

			/*
			 * Some sols don't have the data we're looking for, so
			 * test it using SEASON. Iterate through the sols until
			 * we successfully get some data. Then proceed to
			 * extract the other data we want for this sol.
			 */
				while ((null == season || "" == season) && iter.hasNext())
				{
					sol = iter.next().asText();
					season = root.at("/" + sol + "/Season").asText();
				}

				if (null == season || "" == season)
				{
					event.getChannel().sendMessage(
						"Failed to get insight data...");

					return;
				}

				String avTemp = root.at("/" + sol + "/AT/av").asText();
				String minTemp = root.at("/" + sol + "/AT/mn").asText();
				String maxTemp = root.at("/" + sol + "/AT/mx").asText();

				String avPre = root.at("/" + sol + "/PRE/av").asText();
				String minPre = root.at("/" + sol + "/PRE/mn").asText();
				String maxPre = root.at("/" + sol + "/PRE/mx").asText();

				String fromTime = root.at("/" + sol + "/First_UTC").asText();
				String toTime = root.at("/" + sol + "/Last_UTC").asText();

				EmbedBuilder eBuilder = new EmbedBuilder();

				event.getChannel().sendMessage(new EmbedBuilder()
					.setTitle("Insight Weather for Mars (Sol " + sol + ")")
					.setDescription("**" + fromTime + "** to **" + toTime + "** (UTC)")
					.addInlineField("Season", season)
					.addInlineField("Av. Temp", avTemp + "C")
					.addInlineField("Min/Max", minTemp + "C/" + maxTemp + "C")
					.addInlineField("Av. Pressure", avPre)
					.addInlineField("Min/Max", minPre + "/" + maxPre)
					.setThumbnail("https://upload.wikimedia.org/wikipedia/commons/thumb/0/02/OSIRIS_Mars_true_color.jpg/550px-OSIRIS_Mars_true_color.jpg")
					.setColor(Color.ORANGE));
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}		
	}
}
