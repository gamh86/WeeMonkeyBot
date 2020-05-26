package watcher;

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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.Set;

import java.lang.StringBuilder;

/**
 * Listen for bot messages posted in channels other
 * than the one dedicated for interaction with bots.
 *
 * Delete any offending message and post in dedicated
 * botchan the message ID that was deleted and remind
 * users that communication with bots should take place
 * there.
 */
public class ChannelTrashCleaner implements MessageCreateListener
{
	private Map<String,String> botExceptions = null;
	private final String botChan = "bot-commands";

	public ChannelTrashCleaner(DiscordApi api)
	{
		botExceptions = new HashMap<String,String>();

	/*
	 * XXX
	 *	Put exceptions in a JSON file and
	 *	parse it here instead of hardcoding.
	 */
		botExceptions.put("MEE6", "beauty-area");
	}

	@Override
	public void onMessageCreate(MessageCreateEvent event)
	{
		Message message = event.getMessage();
		Optional<ServerChannel> oServChan = event.getChannel().asServerChannel();
		Optional<TextChannel> otxtChan = event.getChannel().asTextChannel();
		Optional<User> oUser = message.getUserAuthor();

		final TextChannel txtChan; // final to access from lambda expression
		User user;
		String userName;
		String chanName;

		if (!message.isServerMessage())
			return;

		if (!oUser.isPresent())
			return;

		if (!oServChan.isPresent())
			return;

		if (!otxtChan.isPresent())
			return;

		user = oUser.get();
		txtChan = otxtChan.get();

		userName = user.getName();
		chanName = oServChan.get().getName();

		if (user.isYourself())
			return;

		if (user.isBot())
		{
			if (chanName.equals(botChan))
				return;

			String exception = botExceptions.get(userName);

			if (null != exception)
			{
				if (exception.equals(chanName))
					return;
			}

			long mId = event.getMessage().getId();
			DiscordApi api = event.getApi();

			Collection<Channel> colChans = api.getChannelsByName(botChan);
			Object[] chans;
			StringBuilder sBuilder = new StringBuilder();

		/*
		 *	EmbedBuilder eBuilder = new EmbedBuilder();
		 *	ebuilder
		 *		.setTitle("⛔ Deleted message ⛔")
		 		.setDescription("Bot posted in non-bot channel")
				.addField("Botname", userName)
				.addInlineField("Message ID", "" + mId)
				.setFooter("✅ Please interact with bots in " + botChan)
				.setColor(Color.RED);
		 */
			sBuilder
				.append(
					"⛔ Deleted message by " + userName + " ⛔\n\n" +
					"*Message ID*: `" + mId + "`\n" +
					"   *Channel*: `" + chanName + "`\n\n" +
					"✅ Please interact with bots in **" + botChan + "**");

			Set<Map.Entry<String,String>> eSet = botExceptions.entrySet();

			if (!eSet.isEmpty())
				sBuilder.append("\n\n__Exceptions__:\n```");

			for (Map.Entry<String,String> entry : eSet)
			{
				sBuilder.append(String.format("%s in %s", entry.getKey(), entry.getValue()));
			}

			if (!eSet.isEmpty())
				sBuilder.append("```");

			if (!colChans.isEmpty())
			{
				chans = colChans.toArray();
				Channel chan = (Channel)chans[0];
				Optional<TextChannel> oBotChannel = chan.asTextChannel();

				if (oBotChannel.isPresent())
					oBotChannel.get().sendMessage(sBuilder.toString()).join();
			}

			txtChan.deleteMessages(event.getMessage());

		/*
		 * Assume the reason a bot posted in a channel
		 * is because of a user-issued command. So
		 * delete the last two messages in the channel.
		 *
			txtChan
				.getMessages(2)
				.thenAcceptAsync(mSet -> {
					txtChan.bulkDelete(mSet).join();
				});

			CompletableFuture<MessageSet> futureMessageSet = txtChan.getMessages(2);
			MessageSet mSet = futureMessageSet.get();

			if (!mSet.isEmpty())
				txtChan.bulkDelete(mSet).join();
		*/
		}
	}
}
