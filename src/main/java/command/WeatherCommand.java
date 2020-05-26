package command;

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
import org.javacord.api.entity.channel.*;
import org.javacord.api.entity.server.*;
import org.javacord.api.entity.user.*;
import org.javacord.api.entity.message.embed.*;

import java.nio.file.Files;
import java.nio.file.Paths;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import java.lang.StringBuilder;

class ApiToken
{
	private String token;

	public String getToken() { return this.token; }
}

public class WeatherCommand implements MessageCreateListener
{
	private ApiToken openWeatherApiToken = null;
	private static final String CMD = "!wm.weather";

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
	}

	private String parseLocation(String msg)
	{
		byte[] rawMsg = null;
		byte[] rawLocation = null;
		int pos = 0;

		try
		{
			rawMsg = msg.getBytes("UTF-8");

			while (rawMsg[pos] != (byte)' ' && pos < rawMsg.length)
				++pos;

			rawLocation = new byte[pos];
			System.arraycopy(rawMsg, 0, rawLocation, 0, pos);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(1);
		}

		return new String(rawLocation);
	}

	private void sendErrorMessage()
	{
		// TODO
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

	@Override
	public void onMessageCreate(MessageCreateEvent event)
	{
		String messageContent = event.getMessageContent();

		if (messageContent.length() > CMD.length())
		{
			String sub = messageContent.substring(0, CMD.length());
			if (sub.equals(CMD))
			{
				String location = parseLocation(messageContent.substring(CMD.length()));

				try
				{
					URL url = new URL(
						"https://api.openweathermap.org/data/2.5/weather?q=" +
						location +
						"&appid=" +
						openWeatherApiToken.getToken());

					HttpURLConnection conn = (HttpURLConnection)url.openConnection();
					conn.setRequestMethod("GET");
					conn.setRequestProperty("Accept", "application/json");

					if (200 != conn.getResponseCode())
					{
						sendErrorMessage();
						return;
					}

					BufferedReader buf = new BufferedReader(new InputStreamReader(conn.getInputStream()));

					sendToChannel(event, buf);
				}
				catch (Exception e)
				{
				}
			}
		}
	}
}
