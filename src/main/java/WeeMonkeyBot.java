import command.*;
import watcher.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.javacord.api.*;
import org.javacord.api.entity.*;
import org.javacord.api.event.*;
import org.javacord.api.listener.*;
import org.javacord.api.entity.message.*;
import org.javacord.api.event.message.*; // MessageCreateEvent
import org.javacord.api.listener.message.*; // MessageCreateListener
import org.javacord.api.entity.server.*;
import org.javacord.api.entity.user.*;
import org.javacord.api.entity.message.embed.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.io.File;

import java.nio.file.Files;
import java.nio.file.Paths;

import java.awt.Color;

class AToken
{
	private String token;

	public String getToken() { return this.token; }
}

public class WeeMonkeyBot
{
	private static final String BOTNAME = "Wee Monkey";
	private static final String IMAGE_BOT = "src/main/resources/selfie.jpg";

	private final String accessToken;

	private ArrayList<String> fellowBots = null;
	private static Random rand = null;
	private DiscordApi api = null;

	private String getAccessToken()
	{
		AToken tok = null;

		try
		{
			byte[] data = Files.readAllBytes(Paths.get("src/main/resources/access.json"));
			ObjectMapper mapper = new ObjectMapper();

			tok = mapper.readValue(data, AToken.class);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(1);
		}

		return tok.getToken();
	}

	public WeeMonkeyBot()
	{
		Locale locale = new Locale("UK");
		Locale.setDefault(locale);

		accessToken = getAccessToken();
		assert(null != accessToken);

		try
		{
			OUT("Connecting to Discord...");

			api = new DiscordApiBuilder()
				.setToken(accessToken)
				.login()
				.join();
		}
		catch (Exception e)
		{
			System.err.println("Failed to connect to Discord...");
			e.printStackTrace();
		}

		OUT("Connected!");

		api.addMessageCreateListener(new FeedCommand());
		api.addMessageCreateListener(new ClearChannelCommand());
		api.addMessageCreateListener(new ChannelTrashCleaner(api));
		api.addMessageCreateListener(new WeatherCommand());
		api.addMessageCreateListener(new MarsCommand());
	}

	private void OUT(String msg)
	{
		System.out.println(msg);
	}

	public static void main(String[] argv)
	{
		new WeeMonkeyBot();
	}
}
