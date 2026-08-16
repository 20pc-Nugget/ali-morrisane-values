package com.alimorrisanevalues;

import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Varbits;
import net.runelite.api.annotations.Varbit;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
        name = "Ali Morrisane Values",
        description = "Tracks configurations and dynamic internal inventory valuations for baseline runes.",
        tags = {"runes", "ali", "overlay", "panel", "valuation"}
)
public class AliMorrisaneValuesPlugin extends Plugin
{
    @Inject private Client client;
    @Inject private OverlayManager overlayManager;
    @Inject private AliMorrisaneValuesOverlay inventoryOverlay;
    @Inject private ClientToolbar clientToolbar;
    @Inject private AliMorrisaneValuesPanel visualPanel;
    @Inject private AliMorrisaneValuesConfig activeConfig;
    @Inject private ItemManager itemManager;

    public static final Map<Integer, Integer> ALI_RUNE_PRICES = new HashMap<>();
    static {
        ALI_RUNE_PRICES.put(554, 2);    // Fire
        ALI_RUNE_PRICES.put(555, 2);    // Water
        ALI_RUNE_PRICES.put(556, 2);    // Air
        ALI_RUNE_PRICES.put(557, 2);    // Earth
        ALI_RUNE_PRICES.put(558, 1);    // Mind
        ALI_RUNE_PRICES.put(559, 2);    // Body
        ALI_RUNE_PRICES.put(562, 45);   // Chaos
        ALI_RUNE_PRICES.put(560, 90);   // Death
        ALI_RUNE_PRICES.put(561, 90);   // Nature
        ALI_RUNE_PRICES.put(563, 120);  // Law
        ALI_RUNE_PRICES.put(564, 25);   // Cosmic
        ALI_RUNE_PRICES.put(565, 200);  // Blood
        ALI_RUNE_PRICES.put(566, 150);  // Soul
    }

    private NavigationButton sidebarNavigationButton;

    @Provides
    AliMorrisaneValuesConfig provideConfig(final ConfigManager configManager)
    {
        return configManager.getConfig(AliMorrisaneValuesConfig.class);
    }

    @Override
    protected void startUp() throws Exception
    {
        overlayManager.add(inventoryOverlay);

        final BufferedImage lawRuneIcon = itemManager.getImage(563);

        sidebarNavigationButton = NavigationButton.builder()
                .tooltip("Ali Morrisane Values")
                .icon(lawRuneIcon != null ? lawRuneIcon : new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB))
                .priority(5)
                .panel(visualPanel)
                .build();

        if (activeConfig.showSidebarPanel())
        {
            clientToolbar.addNavigation(sidebarNavigationButton);
        }
    }

    @Override
    protected void shutDown() throws Exception
    {
        overlayManager.remove(inventoryOverlay);
        clientToolbar.removeNavigation(sidebarNavigationButton);
    }

    @Subscribe
    public void onGameTick(GameTick gameTick)
    {
        updateAllRuneValues();
    }

    private void updateAllRuneValues()
    {
        final ItemContainer inventoryContainer = client.getItemContainer(InventoryID.INVENTORY);
        final ItemContainer lootingBagContainer = client.getItemContainer(516);
        final ItemContainer divineRunePouchContainer = client.getItemContainer(641);

        final Map<Integer, Long> aggregatedCounts = new HashMap<>();
        for (int itemId : ALI_RUNE_PRICES.keySet())
        {
            aggregatedCounts.put(itemId, 0L);
        }

        // 1. Scan standard Inventory bag slots
        if (inventoryContainer != null)
        {
            for (Item item : inventoryContainer.getItems())
            {
                if (aggregatedCounts.containsKey(item.getId()))
                {
                    aggregatedCounts.put(item.getId(), aggregatedCounts.get(item.getId()) + item.getQuantity());
                }
            }
        }

        // 2. Scan Looting Bag contents
        if (lootingBagContainer != null)
        {
            for (Item item : lootingBagContainer.getItems())
            {
                if (aggregatedCounts.containsKey(item.getId()))
                {
                    aggregatedCounts.put(item.getId(), aggregatedCounts.get(item.getId()) + item.getQuantity());
                }
            }
        }

        // 3. Scan 4-slot upgraded Divine Rune Pouch container
        if (divineRunePouchContainer != null)
        {
            for (Item item : divineRunePouchContainer.getItems())
            {
                if (aggregatedCounts.containsKey(item.getId()))
                {
                    aggregatedCounts.put(item.getId(), aggregatedCounts.get(item.getId()) + item.getQuantity());
                }
            }
        }

        // 4. Scan 3-slot standard pouch via native Varbit values
        tallyStandardPouchVarbits(aggregatedCounts);

        // 5. Update independent user panel nodes
        long cumulativeGrandTotalGpValue = 0;
        for (int itemId : ALI_RUNE_PRICES.keySet())
        {
            long totalQuantity = aggregatedCounts.get(itemId);
            visualPanel.refreshRuneMetrics(itemId, totalQuantity);

            long unitPrice = ALI_RUNE_PRICES.get(itemId);
            cumulativeGrandTotalGpValue += (totalQuantity * unitPrice);
        }

        visualPanel.refreshTotalLumpSum(cumulativeGrandTotalGpValue);
    }

    private void tallyStandardPouchVarbits(Map<Integer, Long> aggregatedCounts)
    {
        @Varbit int[] typeVarbits = {Varbits.RUNE_POUCH_RUNE1, Varbits.RUNE_POUCH_RUNE2, Varbits.RUNE_POUCH_RUNE3};
        @Varbit int[] amountVarbits = {Varbits.RUNE_POUCH_AMOUNT1, Varbits.RUNE_POUCH_AMOUNT2, Varbits.RUNE_POUCH_AMOUNT3};

        for (int i = 0; i < typeVarbits.length; i++)
        {
            int varbitValue = client.getVarbitValue(typeVarbits[i]);
            int amount = client.getVarbitValue(amountVarbits[i]);

            if (amount > 0)
            {
                int trueGlobalItemId = convertVarbitToRuneItemId(varbitValue);
                if (aggregatedCounts.containsKey(trueGlobalItemId))
                {
                    aggregatedCounts.put(trueGlobalItemId, aggregatedCounts.get(trueGlobalItemId) + amount);
                }
            }
        }
    }

    /**
     * Official OSRS Internal Client Cache Rune Pouch Varbit Mapping Index
     */
    private int convertVarbitToRuneItemId(int varbitValue)
    {
        switch (varbitValue)
        {
            case 1: return 556;   // Air rune
            case 2: return 555;   // Water rune
            case 3: return 557;   // Earth rune
            case 4: return 554;   // Fire rune
            case 5: return 558;   // Mind rune
            case 6: return 562;   // Chaos rune
            case 7: return 560;   // Death rune
            case 8: return 565;   // Blood rune
            case 9: return 564;   // Cosmic rune
            case 10: return 561;  // Nature rune
            case 11: return 563;  // Law rune
            case 12: return 559;  // Body rune
            case 13: return 566;  // Soul rune
            default: return -1;
        }
    }
}
