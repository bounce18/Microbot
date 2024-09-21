package net.runelite.client.plugins.microbot.qualityoflife.scripts;

import net.runelite.api.*;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.NpcChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.widgets.Widget;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.qualityoflife.QoLConfig;
import net.runelite.client.plugins.microbot.qualityoflife.QoLPlugin;
import net.runelite.client.plugins.microbot.qualityoflife.enums.WintertodtActions;
import net.runelite.client.plugins.microbot.util.Global;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.menu.NewMenuEntry;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

public class WintertodtScript extends Script {
    public static QoLConfig config;
    public static GameObject unlitBrazier;
    public static GameObject brokenBrazier;
    public static NPC pyromancer;
    public static NPC incapitatedPyromancer;
    public static boolean helpedIncapitatedPyromancer = false;
    public static boolean isWintertodtAlive = false;
    public static int wintertodtHp = -1;
    @Inject
    private QoLPlugin qolPlugin;

    public static boolean isInWintertodtRegion() {
        return Rs2Player.getWorldLocation().getRegionID() == 6462;
    }

    public boolean run(QoLConfig config) {
        WintertodtScript.config = config;
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn() || !super.run() || !isInWintertodtRegion()) {
                    return;
                }

                Widget wintertodtHealthbar = Rs2Widget.getWidget(25952276);
                isWintertodtAlive = Rs2Widget.hasWidget("Wintertodt's Energy");
                if (wintertodtHealthbar != null && isWintertodtAlive) {
                    String widgetText = wintertodtHealthbar.getText();
                    wintertodtHp = Integer.parseInt(widgetText.split("\\D+")[1]);
                } else {
                    wintertodtHp = -1;
                }
                brokenBrazier = Rs2GameObject.getGameObjects(ObjectID.BRAZIER_29313).stream().findFirst().orElse(null);
                unlitBrazier = Rs2GameObject.getGameObjects(ObjectID.BRAZIER_29312).stream().findFirst().orElse(null);

                if (!config.interrupted())
                    return;

                NewMenuEntry actionToResume = config.wintertodtActions().getMenuEntry();
                if (config.wintertodtActions().equals(WintertodtActions.FEED)) {
                    GameObject fireBrazier = Rs2GameObject.getGameObjects(ObjectID.BURNING_BRAZIER_29314).stream().findFirst().orElse(null);
                    if (fireBrazier != null && fireBrazier.getWorldLocation().distanceTo2D(Rs2Player.getWorldLocation()) < 5) {
                        if (!Rs2Inventory.contains(ItemID.BRUMA_ROOT) && !Rs2Inventory.contains(ItemID.BRUMA_KINDLING)) {
                            qolPlugin.updateLastWinthertodtAction(WintertodtActions.NONE);
                            qolPlugin.updateWintertodtInterupted(false);
                            Microbot.log("No resources found in inventory, cancelling action.");
                            return;
                        }
                        Microbot.doInvoke(actionToResume, new java.awt.Rectangle(1, 1));
                        qolPlugin.updateWintertodtInterupted(false);
                    }
                }
                if (config.wintertodtActions().equals(WintertodtActions.FLETCH)) {
                    if (!Rs2Inventory.contains(ItemID.BRUMA_ROOT)) {
                        qolPlugin.updateLastWinthertodtAction(WintertodtActions.NONE);
                        qolPlugin.updateWintertodtInterupted(false);
                        return;
                    }

                    Microbot.doInvoke(actionToResume, new java.awt.Rectangle(1, 1));
                    WintertodtActions.fletchBrumaRootsOnClicked();
                    qolPlugin.updateWintertodtInterupted(false);
                }

            } catch (Exception ignore) {

            }
        }, 0, 100, TimeUnit.MILLISECONDS);
        return true;
    }

    // shutdown
    @Override
    public void shutdown() {
        super.shutdown();
        Microbot.log("Winterdotd script shutting down");
    }

    public void onNpcChanged(NpcChanged event) {
        if (event.getNpc().getId() == 7372) {
            incapitatedPyromancer = event.getNpc();

        }
        if (event.getNpc().getId() == 7371) {
            pyromancer = event.getNpc();
            incapitatedPyromancer = null;
            if (helpedIncapitatedPyromancer) {
                if (config.lightUnlitBrazier()) {
                    if (Rs2Equipment.hasEquipped(ItemID.BRUMA_TORCH) || Rs2Equipment.hasEquipped(ItemID.BRUMA_TORCH_OFFHAND) || Rs2Inventory.hasItem(ItemID.TINDERBOX)) {
                        Microbot.getClientThread().invokeLater(() -> Rs2GameObject.interact(unlitBrazier, "Light"));
                    }
                }
            }
            helpedIncapitatedPyromancer = false;
        }
    }

    public void onNpcSpawned(NpcSpawned event) {
        if (event.getNpc().getId() == 7372) {
            if (incapitatedPyromancer != null) {
                if (incapitatedPyromancer.getWorldLocation().distanceTo2D(Rs2Player.getWorldLocation()) > event.getNpc().getWorldLocation().distanceTo2D(Rs2Player.getWorldLocation())) {
                    incapitatedPyromancer = event.getNpc();
                    return;
                }
            }
            incapitatedPyromancer = event.getNpc();
        }
        if (event.getNpc().getId() == 7371) {
            if (pyromancer != null) {
                if (pyromancer.getWorldLocation().distanceTo2D(Rs2Player.getWorldLocation()) > event.getNpc().getWorldLocation().distanceTo2D(Rs2Player.getWorldLocation())) {
                    pyromancer = event.getNpc();
                    return;
                }
            }
            pyromancer = event.getNpc();
        }
    }

    public void onNpcDespawned(NpcDespawned event) {
        if (event.getNpc().equals(incapitatedPyromancer)) {
            incapitatedPyromancer = Microbot.getClientThread().runOnClientThread(() -> Rs2Npc.getNpc("Incapacitated Pyromancer"));
        }
        if (event.getNpc().equals(pyromancer)) {
            pyromancer = Microbot.getClientThread().runOnClientThread(() -> Rs2Npc.getNpc("Pyromancer"));
        }
    }

    public void onChatMessage(ChatMessage chatMessage) {
        var message = chatMessage.getMessage();
        if (message.contains("The brazier is broken and shrapnel")) {
            brokenBrazier = Microbot.getClientThread().runOnClientThread(() -> Rs2GameObject.getGameObjects(ObjectID.BRAZIER_29313).stream().filter(gameObject -> gameObject.getWorldLocation().distanceTo2D(Rs2Player.getWorldLocation()) < 5).findFirst().orElse(null));
            Microbot.log("Broken brazier: " + brokenBrazier);
            if (config.fixBrokenBrazier()) {
                qolPlugin.updateWintertodtInterupted(false);
                Global.sleepUntilOnClientThread(() -> (brokenBrazier != null && brokenBrazier.getWorldLocation().distanceTo2D(Rs2Player.getWorldLocation()) < 5), 1000);
                Microbot.getClientThread().invokeLater(() -> Rs2GameObject.interact(brokenBrazier, "Fix"));
            }
        }

        if (message.startsWith("The brazier has gone out")) {
            unlitBrazier = Microbot.getClientThread().runOnClientThread(() -> Rs2GameObject.getGameObjects(ObjectID.BRAZIER_29312).stream().filter(gameObject -> gameObject.getWorldLocation().distanceTo2D(Rs2Player.getWorldLocation()) < 5).findFirst().orElse(null));
            if (incapitatedPyromancer != null) {
                if (!config.healPyromancer())
                    return;
                if (Rs2Npc.interact(incapitatedPyromancer, "Help")) {
                    helpedIncapitatedPyromancer = true;
                }
            } else {
                if (config.lightUnlitBrazier()) {
                    if (Rs2Equipment.hasEquipped(ItemID.BRUMA_TORCH) || Rs2Equipment.hasEquipped(ItemID.BRUMA_TORCH_OFFHAND) || Rs2Inventory.hasItem(ItemID.TINDERBOX)) {
                        Global.sleepUntilOnClientThread(() -> unlitBrazier != null && unlitBrazier.getWorldLocation().distanceTo2D(Rs2Player.getWorldLocation()) < 5, 1000);
                        Microbot.getClientThread().invokeLater(() -> Rs2GameObject.interact(unlitBrazier, "Light"));
                    }
                }
            }
        }

        if (message.startsWith("You fix the brazier")) {
            unlitBrazier = Microbot.getClientThread().runOnClientThread(() -> Rs2GameObject.getGameObjects(ObjectID.BRAZIER_29312).stream().filter(gameObject -> gameObject.getWorldLocation().distanceTo2D(Rs2Player.getWorldLocation()) < 5).findFirst().orElse(null));
            if (incapitatedPyromancer != null) {
                if (!config.healPyromancer())
                    return;
                if (Rs2Npc.interact(incapitatedPyromancer, "Help")) {
                    helpedIncapitatedPyromancer = true;
                }
            } else {
                if (config.lightUnlitBrazier()) {
                    if (Rs2Equipment.hasEquipped(ItemID.BRUMA_TORCH) || Rs2Equipment.hasEquipped(ItemID.BRUMA_TORCH_OFFHAND) || Rs2Inventory.hasItem(ItemID.TINDERBOX)) {
                        Global.sleepUntil(() -> unlitBrazier.getWorldLocation().distanceTo2D(Rs2Player.getWorldLocation()) < 5, 1000);
                        Microbot.getClientThread().invokeLater(() -> Rs2GameObject.interact(unlitBrazier, "Light"));
                    }
                }
            }
        }

        if (message.startsWith("Heal the Pyromancer")) {
            if (incapitatedPyromancer != null) {
                if (!config.healPyromancer())
                    return;
                if (Rs2Npc.interact(incapitatedPyromancer, "Help")) {
                    helpedIncapitatedPyromancer = true;
                }
            }
        }

        if (message.startsWith("You light the brazier")) {
            if (config.wintertodtActions().equals(WintertodtActions.NONE)) {
                return;
            }
            qolPlugin.updateWintertodtInterupted(true);
        }

        if (message.startsWith("You have gained a supply crate")) {
            qolPlugin.updateWintertodtInterupted(false);
            qolPlugin.updateLastWinthertodtAction(WintertodtActions.NONE);
        }

        if (message.startsWith("You did not earn enough points")) {
            qolPlugin.updateWintertodtInterupted(false);
            qolPlugin.updateLastWinthertodtAction(WintertodtActions.NONE);
        }

        if (message.startsWith("You have run out of bruma roots")) {
            qolPlugin.updateWintertodtInterupted(false);
            qolPlugin.updateLastWinthertodtAction(WintertodtActions.NONE);
        }
    }

    private void broadcastMessage(String message) {
        Microbot.getChatMessageManager().queue(QueuedMessage.builder()
                .runeLiteFormattedMessage(
                        new ChatMessageBuilder()
                                .append(ChatColorType.NORMAL)
                                .append("[QoL Wintertodt] ")
                                .append(ChatColorType.HIGHLIGHT)
                                .append(message)
                                .build()
                )
                .type(ChatMessageType.BROADCAST)
                .build());
    }
}
