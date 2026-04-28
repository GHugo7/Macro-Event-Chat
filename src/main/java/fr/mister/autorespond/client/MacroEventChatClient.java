package fr.mister.autorespond.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fabricmc.fabric.api.client.rendering.v1.hud.*;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MacroEventChatClient implements ClientModInitializer {

	private Integer calcul = null;
	private long calculTime = 0;
	private static final Logger LOGGER = LoggerFactory.getLogger("macroeventchat");

	@Override
	public void onInitializeClient() {
		// This entrypoint is suitable for setting up client-specific logic, such as rendering.

		HudElementRegistry.addLast(
				Identifier.of("macroeventchat", "result_overlay"),
				(context, tickCounter) -> {
					if (calcul != null && System.currentTimeMillis() - calculTime < 10000) {
						context.drawText(
								MinecraftClient.getInstance().textRenderer,
								"Réponse : " + calcul,
								10, 10,
								0xFFFFFFFF,
								true
						);
					} else if (calcul != null) {
						LOGGER.info("Timer expiré, vidage du clipboard");
						// Après le timer de 10s -> vide le clipboard
						MinecraftClient.getInstance().keyboard.setClipboard(" ");
						calcul = null;
					}
				}
		);

		ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
			String text = message.getString();

			//LOGGER.info("CHAT: " + message.getString());

			if (traiterMessage(text)) {
				if (calcul != null) {
					MinecraftClient.getInstance().keyboard.setClipboard(calcul.toString());
				}
			}
		});


		ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
			String text = message.getString();

			//LOGGER.info("GAME: " + message.getString());

			if (traiterMessage(text)) {
				if (calcul != null) {
					MinecraftClient.getInstance().keyboard.setClipboard(calcul.toString());
				}
			}
		});

	}

	private boolean traiterMessage(String text) {
		boolean find = false;

		// Calculer
		// Pattern regex
		Pattern regex = Pattern.compile("(\\d+) ([x+/-]) (\\d+)");

		Matcher match = regex.matcher(text);
		if (match.find()) {
			String num1 = match.group(1);
			String ope = match.group(2);
			String num2 = match.group(3);

			Integer int1 = Integer.parseInt(num1);
			Integer int2 = Integer.parseInt(num2);

			switch (ope) {
				case "+": calcul = int1 + int2; break;
				case "-": calcul = int1 - int2; break;
				case "x": calcul = int1 * int2; break;
				case "/": calcul = int1 / int2; break;
				default: calcul = 0; break;
			}
			calculTime = System.currentTimeMillis();
			find = true;
		}

		// Mot à recopier
		Pattern regexWord = Pattern.compile("Soyez le premier à taper (\\S+)");
		Matcher matchWord = regexWord.matcher(text.replaceAll("§[0-9a-fk-or]", "").trim());

		if (matchWord.find()) {
			String word = matchWord.group(1);

			MinecraftClient.getInstance().keyboard.setClipboard(word);
			find = true;
		}

		// Auto BIENVENUE
		Pattern regexBvn = Pattern.compile("fait ses premiers pas sur le serveur");
		Matcher matchBvn = regexBvn.matcher(text);
		if (matchBvn.find()) {
			MinecraftClient.getInstance().player.networkHandler.sendChatCommand("bvn");
		}
		return find;
	}


}