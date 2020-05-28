package command;

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

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import java.awt.Color;

public class ClearChannelCommand implements MessageCreateListener
{
	private static final String CMD = "o?clear";

	@Override
	public void onMessageCreate(MessageCreateEvent event)
	{
		MessageAuthor author = event.getMessage().getAuthor();
		Server server = null;

		if (!event.getMessageContent().equals(CMD))
			return;

		try
		{
			Optional<Server> oServer = event.getMessage().getServer();

			if (oServer.isPresent())
				server = oServer.get();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		if (null == server)
			return;

		if (!author.isServerAdmin())
		{
			event.getChannel().sendMessage("🔐 Only the Emperor/Empress can clear channels! 🔐");
		}
		else
		{
			try
			{
				Optional<TextChannel> oTxtChannel = event.getChannel().asTextChannel();
				CompletableFuture<MessageSet> futureMessageSet;
				MessageSet mSet = null;

				if (!oTxtChannel.isPresent())
					return;

				TextChannel txtChannel = oTxtChannel.get();
				Optional<ServerChannel> serverChan = txtChannel.asServerChannel();
				int nrDeleted = 0;

				String chanName;
				if (serverChan.isPresent())
					chanName = serverChan.get().getName();
				else
					chanName = "none";

				while (true)
				{
					futureMessageSet = txtChannel.getMessages(100);
					mSet = futureMessageSet.get();
					if (mSet.isEmpty() || mSet.size() < 2)
						break;

					nrDeleted += mSet.size();
					txtChannel.bulkDelete(mSet).join();
				}

				Optional<User> oUser = author.asUser();

				if (oUser.isPresent())
				{
					EmbedBuilder eBuilder = new EmbedBuilder();

					eBuilder
						.setTitle("Cleared Messages")
						.addInlineField("Messages", "" + nrDeleted)
						.addInlineField("Channel", chanName)
						.addInlineField("ChannelID", String.format("0x%x", txtChannel.getId()))
						.setThumbnail("https://i0.wp.com/cdnimg.webstaurantstore.com/images/products/extra_large/72222/871507.jpg")
						.setColor(Color.ORANGE);

					oUser.get().sendMessage(eBuilder).join();
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
				System.exit(1);
			}
		}
	}
}
