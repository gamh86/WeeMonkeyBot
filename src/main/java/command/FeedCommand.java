package command;

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
import java.util.Random;

class Response
{
	private String message;
	private String URL;

	public Response(String msg, String url)
	{
		this.message = msg;
		this.URL = url;
	}

	public String getMessage()
	{
		return this.message;
	}

	public String getURL()
	{
		return this.URL;
	}
}

public class FeedCommand implements MessageCreateListener
{
	private static final String CMD = "!wm.feed";
	private static final String BOTNAME = "Wee Monkey";

	private Random rand = null;
	private ArrayList<Response> CMD_responses = null;

/*
 * XXX
 *	Need to store this information in
 *	a JSON file in src/main/resources.
 */
	public FeedCommand()
	{
		rand = new Random();

		CMD_responses = new ArrayList<Response>();

		CMD_responses.add(new Response(BOTNAME + " found some crumbs for you!",
			"https://cdn.discordapp.com/attachments/714242110525669516/714597298935562350/crumbs.jpg"));
		CMD_responses.add(new Response(BOTNAME + " treated you to some fine sushi!",
			"https://cdn.discordapp.com/attachments/714242110525669516/714598939176861767/sushi.jpg"));
		CMD_responses.add(new Response(BOTNAME + " said No!!!",
			"https://cdn.discordapp.com/attachments/714242110525669516/714598920520728616/nothing.jpg"));
	}

	@Override
	public void onMessageCreate(MessageCreateEvent event)
	{
		if (!event.getMessageContent().equals(CMD))
			return;

		int choice = rand.nextInt(CMD_responses.size());
		Response res = CMD_responses.get(choice);

		new MessageBuilder()
			.append(res.getMessage(), MessageDecoration.ITALICS)
			.append("\n" + res.getURL())
			.send(event.getChannel());
	}
}
