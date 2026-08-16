package net.runelite.client.plugins.alimorrisanevalues;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.ui.overlay.WidgetItemOverlay;
import net.runelite.client.ui.overlay.components.TextComponent;
import net.runelite.client.util.QuantityFormatter;

public class AliMorrisaneValuesOverlay extends WidgetItemOverlay
{
    private final Client client;
    private final AliMorrisaneValuesConfig config;

    @Inject
    public AliMorrisaneValuesOverlay(final Client client, final AliMorrisaneValuesConfig config)
    {
        this.client = client;
        this.config = config;
        showOnInventory();
    }

    @Override
    public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem widgetItem)
    {
        if (!config.showInventoryOverlay())
        {
            return;
        }

        if (AliMorrisaneValuesPlugin.ALI_RUNE_PRICES.containsKey(itemId))
        {
            final int unitPrice = AliMorrisaneValuesPlugin.ALI_RUNE_PRICES.get(itemId);
            final long cumulativeValue = (long) unitPrice * widgetItem.getQuantity();

            final Rectangle bounds = widgetItem.getCanvasBounds();
            final TextComponent textComponent = new TextComponent();

            textComponent.setText(QuantityFormatter.quantityToStackSize(cumulativeValue));
            textComponent.setColor(Color.YELLOW);
            textComponent.setPosition(new Point(bounds.x, bounds.y + bounds.height - 2));
            textComponent.render(graphics);
        }
    }
}
