package net.runelite.client.plugins.alimorrisanevalues;

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

    // 1. Embedded explicit OSRS client mapping enum table requested by user
    public enum PouchRune {
        NONE(0, -1),
        AIR(1, 556),
        WATER(2, 555),
        EARTH(3, 557),
        FIRE(4, 554),
        MIND(5, 558),
        CHAOS(6, 562),
        DEATH(7, 560),
        BLOOD(8, 565),
        COSMIC(9, 564),
        NATURE(10, 561),
        LAW(11, 563),
        SOUL(12, 566),
        ASTRAL(13, 9075),
        WRATH(14, 21880),
        MIST(15, 4695),
        MUD(16, 4698),
        DUST(17, 4696),
        LAVA(18, 4699),
        STEAM(19, 4694),
        SMOKE(20, 4697);

        private final int varbitValue;
        private final int itemId;

        PouchRune(int varbitValue, int itemId) {
            this.varbitValue = varbitValue;
            this.itemId = itemId;
        }

        public static int getItemIdByVarbit(int value) {
            for (PouchRune rune : values()) {
                if (rune.varbitValue == value) return rune.itemId;
            }
            return -1;
        }
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

        // 2. Initialize baseline tracking collections
        final Map<Integer, Long> aggregatedCounts = new HashMap<>();
        for (int itemId : ALI_RUNE_PRICES.keySet())
        {
            aggregatedCounts.put(itemId, 0L);
        }

        // Tally inventory items and extract configuration context properties
        int activePouchSlots = 0;

        if (inventoryContainer != null)
        {
            for (Item item : inventoryContainer.getItems())
            {
                int canonicalId = itemManager.canonicalize(item.getId());

                // Track down the exact pouch capacity limit rules
                if (canonicalId == 12791 || canonicalId == 24416) // Standard/Imbued 3-Slot
                {
                    activePouchSlots = 3;
                }
                else if (canonicalId == 27281 || canonicalId == 27509) // Standard/Imbued Divine 4-Slot
                {
                    activePouchSlots = 4;
                }

                if (aggregatedCounts.containsKey(canonicalId))
                {
                    aggregatedCounts.put(canonicalId, aggregatedCounts.get(canonicalId) + item.getQuantity());
                }
            }
        }

        // 3. Complete Varbit data frame traversal using the user-defined Enum data table
        if (activePouchSlots > 0)
        {
            @Varbit int[] typeVarbits = {Varbits.RUNE_POUCH_RUNE1, Varbits.RUNE_POUCH_RUNE2, Varbits.RUNE_POUCH_RUNE3, Varbits.RUNE_POUCH_RUNE4};
            @Varbit int[] amountVarbits = {Varbits.RUNE_POUCH_AMOUNT1, Varbits.RUNE_POUCH_AMOUNT2, Varbits.RUNE_POUCH_AMOUNT3, Varbits.RUNE_POUCH_AMOUNT4};

            for (int i = 0; i < activePouchSlots; i++)
            {
                int varbitValue = client.getVarbitValue(typeVarbits[i]);
                int amount = client.getVarbitValue(amountVarbits[i]);

                if (amount > 0)
                {
                    // Query the exact PouchRune mapping method
                    int trueGlobalItemId = PouchRune.getItemIdByVarbit(varbitValue);
                    if (aggregatedCounts.containsKey(trueGlobalItemId))
                    {
                        aggregatedCounts.put(trueGlobalItemId, aggregatedCounts.get(trueGlobalItemId) + amount);
                    }
                }
            }
        }

        // 4. Calculate final values and flush to UI
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
}
