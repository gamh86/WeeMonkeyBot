package command;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

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
import java.util.TimeZone;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

import java.nio.file.Files;
import java.nio.file.Paths;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.FileReader;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import java.lang.StringBuilder;

import java.awt.Color;

class ApiToken
{
	private String token;

	public String getToken() { return this.token; }
}

@JsonIgnoreProperties(ignoreUnknown = true)
class Coordinates
{
	@JsonProperty
	private double lon;
	@JsonProperty
	private double lat;

	public double getLongitude() { return this.lon; }
	public double getLatitude() { return this.lat; }
}

@JsonIgnoreProperties(ignoreUnknown = true)
class Weather
{
	@JsonProperty
	private int id;
	@JsonProperty
	private String main;
	@JsonProperty
	private String description;
	@JsonProperty
	private String icon;

	public int getId() { return this.id; }
	public String getMain() { return this.main; }
	public String getDescription() { return this.description; }
	public String getIcon() { return this.icon; }
}

@JsonIgnoreProperties(ignoreUnknown = true)
class wMain
{
	@JsonProperty
	double temp;
	@JsonProperty
	double feels_like;
	@JsonProperty
	double temp_min;
	@JsonProperty
	double temp_max;
	@JsonProperty
	int pressure;
	@JsonProperty
	int humidity;

	public double getTemp() { return this.temp; }
	public double getFeelsLike() { return this.feels_like; }
	public double getMinTemp() { return this.temp_min; }
	public double getMaxTemp() { return this.temp_max; }
	public int getPressure() { return this.pressure; }
	public int getHumidity() { return this.humidity; }
}

@JsonIgnoreProperties(ignoreUnknown = true)
class Wind
{	@JsonProperty
	double speed;
	@JsonProperty
	int deg;

	public double getSpeed() { return this.speed; }
	public int getDeg() { return this.deg; }
}

@JsonIgnoreProperties(ignoreUnknown = true)
class Clouds
{
	@JsonProperty
	int all;

	public int getAll() { return this.all; }
}

@JsonIgnoreProperties(ignoreUnknown = true)
class Sys
{
	@JsonProperty
	int type;
	@JsonProperty
	int id;
	@JsonProperty
	String country;
	@JsonProperty
	long sunrise;
	@JsonProperty
	long sunset;

	public int getType() { return this.type; }
	public int getId() { return this.id; }
	public String getCountry() { return this.country; }
	public long getSunrise() { return this.sunrise; }
	public long getSunset() { return this.sunset; }
}

@JsonIgnoreProperties(ignoreUnknown = true)
class WeatherData
{
	@JsonProperty
	Coordinates coord;
	@JsonProperty
	Weather[] weather;
	@JsonProperty
	String base;
	@JsonProperty
	wMain main;
	@JsonProperty
	int visibility;
	@JsonProperty
	Wind wind;
	@JsonProperty
	Clouds clouds;
	@JsonProperty
	long dt;
	@JsonProperty
	Sys sys;
	@JsonProperty
	int timezone;
	@JsonProperty
	int id;
	@JsonProperty
	String name;
	@JsonProperty
	int cod;

	public Coordinates getCoordinates() { return this.coord; }
	public Weather[] getWeather() { return this.weather; }
	public String getBase() { return this.base; }
	public wMain getMain() { return this.main; }
	public int getVisibility() { return this.visibility; }
	public Wind getWind() { return this.wind; }
	public Clouds getClouds() { return this.clouds; }
	public long getDt() { return this.dt; }
	public Sys getSys() { return this.sys; }
	public int getTimezone() { return this.timezone; }
	public int getId() { return this.id; }
	public String getName() { return this.name; }
	public int getCode() { return this.cod; }
}

class CachedWeatherData
{
	private WeatherData data;
	private long timeCached;

	public WeatherData getWeatherData() { return this.data; }
	public long getTimeCached() { return this.timeCached; }

	public void setWeatherData(WeatherData d) { this.data = d; }
	public void setTimeCached(long t) { this.timeCached = t; }
}

/**
 * Cache weather entries for 30 minutes. Any requests
 * for said data will be fulfilled. Any other queries
 * are subject to 1 request per minute (as per
 * openweathermap API rules).
 */
public class WeatherCommand implements MessageCreateListener
{
	private ApiToken openWeatherApiToken = null;
	private static final String CMD = "o?weather";
	private static final double KELVIN_CELSIUS = 273.15;
	private static long timeLastRequest = 0;
	private long minDelay = 60000; // milliseconds
	private final int CACHE_LIFETIME = (60000 * 30); // thirty minutes
	private Map<String,CachedWeatherData> cache;
	Logger log = LogManager.getLogger(WeatherCommand.class.getName());

	public WeatherCommand()
	{
		try
		{
			ObjectMapper mapper = new ObjectMapper();
			byte[] data = Files.readAllBytes(Paths.get("src/main/resources/openweather.json"));

			openWeatherApiToken = mapper.readValue(data, ApiToken.class);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(1);
		}

		cache = new HashMap<String,CachedWeatherData>();
	}

	private boolean canSendRequest()
	{
		if ((System.currentTimeMillis() - timeLastRequest) >= minDelay)
			return true;

		return false;
	}

	private void sendErrorMessage(MessageCreateEvent event, String loc, int code)
	{
		EmbedBuilder eBuilder = new EmbedBuilder();

		log.trace("No weather data for \"" + log + "\"");
		eBuilder
			.setTitle("⚠ An Error Occurred ⚠")
			.setDescription("No weather information for \"" + loc + "\" (" + code + ")")
			.setThumbnail("https://upload.wikimedia.org/wikipedia/commons/thumb/f/fc/Emoji_u1f914.svg/256px-Emoji_u1f914.svg.png")
			.setColor(Color.RED);

		event.getChannel().sendMessage(eBuilder);

		return;
	}

	private void sendToChannel(MessageCreateEvent event, BufferedReader buf)
	{
		String line = null;
		StringBuilder sBuilder = new StringBuilder();

		try
		{
			while (true)
			{
				line = buf.readLine();
				if (null == line)
					break;

				sBuilder.append(line);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(1);
		}

		event.getChannel().sendMessage(sBuilder.toString());
	}

	private boolean dataOK(CachedWeatherData cached)
	{
		return null != cached && (System.currentTimeMillis() - cached.getTimeCached()) < CACHE_LIFETIME;
	}

	private void sendData(MessageCreateEvent event, String location, WeatherData data)
	{
		Coordinates coords = data.getCoordinates();
		Weather[] weather = data.getWeather();
		wMain wmain = data.getMain();
		Sys sys = data.getSys();
		long timezoneOffsetMillis = data.getTimezone() * 1000;

		SimpleDateFormat dateSunrise = new SimpleDateFormat();
		SimpleDateFormat dateSunset = new SimpleDateFormat();

		dateSunrise.setTimeZone(TimeZone.getTimeZone("UTC"));
		dateSunset.setTimeZone(TimeZone.getTimeZone("UTC"));

		double temp = wmain.getTemp() - KELVIN_CELSIUS;
		double feelsLike = wmain.getFeelsLike() - KELVIN_CELSIUS;
		double minTemp = wmain.getMinTemp() - KELVIN_CELSIUS;
		double maxTemp = wmain.getMaxTemp() - KELVIN_CELSIUS;

		long timeSunrise = sys.getSunrise() * 1000;
		long timeSunset = sys.getSunset() * 1000;

		timeSunrise += timezoneOffsetMillis;
		timeSunset += timezoneOffsetMillis;

		DecimalFormat dFmt = new DecimalFormat("#.00");

		event.getChannel().sendMessage(new EmbedBuilder()
			.setTitle(
				"Weather for " + location +
				" [" + coords.getLatitude() + " : " + coords.getLongitude() + "]")
			.setDescription(weather[0].getDescription())
			.addInlineField("Current", dFmt.format(temp) + "C")
			.addInlineField("Feels Like", dFmt.format(feelsLike) + "C")
			.addInlineField("Min/Max", dFmt.format(minTemp) + "C/" + dFmt.format(maxTemp) + "C")
			.addInlineField("Humidity", "" + wmain.getHumidity() + "%")
			.addInlineField("Sunrise at", dateSunrise.format(new Date(timeSunrise)).substring(10))
			.addInlineField("Sunset at", dateSunset.format(new Date(timeSunset)).substring(10))
			.setColor(Color.ORANGE)
			.setThumbnail("http://openweathermap.org/img/wn/" + weather[0].getIcon() + "@2x.png"));

		return;
	}

	@Override
	public void onMessageCreate(MessageCreateEvent event)
	{
		String messageContent = event.getMessageContent();

		if (messageContent.length() > CMD.length())
		{
			String sub = messageContent.substring(0, CMD.length());

			if (sub.equals(CMD))
			{
				String location = messageContent.substring(CMD.length()+1);
				CachedWeatherData cData = cache.get(location);

				if (dataOK(cData))
				{
					log.trace("Fulfilling request with data from cache");
					sendData(event, location, cData.getWeatherData());
					return;
				}

				if (!canSendRequest())
				{
					event.getChannel().sendMessage("Only one weather request per minute is currently permitted!");
					return;
				}

				try
				{
					log.trace("Querying weather for \"" + location + "\"");

					URL url = new URL(
						"https://api.openweathermap.org/data/2.5/weather?q=" +
						location +
						"&appid=" +
						openWeatherApiToken.getToken());

					HttpURLConnection conn = (HttpURLConnection)url.openConnection();
					conn.setRequestMethod("GET");
					conn.setRequestProperty("Accept", "application/json");

					timeLastRequest = System.currentTimeMillis();

					int code = conn.getResponseCode();

					if (200 != code)
						sendErrorMessage(event, location, code);

					BufferedReader buf = new BufferedReader(new InputStreamReader(conn.getInputStream()));
					//BufferedReader buf = new BufferedReader(new FileReader("src/main/resources/sample.json"));

					StringBuilder sBuilder = new StringBuilder();
					String line;

					while ((line = buf.readLine()) != null)
					{
						sBuilder.append(line);
					}

					ObjectMapper mapper = new ObjectMapper();
					byte[] jsonData = sBuilder.substring(0).getBytes("UTF-8");
					WeatherData weatherData = mapper.readValue(jsonData, WeatherData.class);

					CachedWeatherData cached = new CachedWeatherData();
					cached.setWeatherData(weatherData);
					cached.setTimeCached(System.currentTimeMillis());

					log.trace("Caching weather data for \"" + location + "\"");
					cache.put(location, cached);

					sendData(event, location, weatherData);
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		}
	}
}
