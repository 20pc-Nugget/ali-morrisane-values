package com.alimorrisanevalues;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.QuantityFormatter;

public class AliMorrisaneValuesPanel extends PluginPanel
{
    private final ItemManager itemManager;
    private final JLabel totalValueLabel = new JLabel("0 GP");
    private final JLabel coinImageLabel = new JLabel();
    private final Map<Integer, JLabel> qtyLabels = new HashMap<>();
    private final Map<Integer, JLabel> valLabels = new HashMap<>();

    @Inject
    public AliMorrisaneValuesPanel(ItemManager itemManager)
    {
        this.itemManager = itemManager;

        setBorder(new EmptyBorder(10, 10, 10, 10));
        setLayout(new BorderLayout());

        JPanel runeListPanel = new JPanel();
        runeListPanel.setLayout(new GridLayout(0, 1, 0, 5));

        for (int itemId : AliMorrisaneValuesPlugin.ALI_RUNE_PRICES.keySet())
        {
            JPanel row = new JPanel(new BorderLayout());
            JLabel iconLabel = new JLabel();
            itemManager.getImage(itemId).addTo(iconLabel);

            JLabel qtyLabel = new JLabel("0", JLabel.LEFT);
            qtyLabel.setForeground(Color.WHITE);
            qtyLabels.put(itemId, qtyLabel);

            JLabel valLabel = new JLabel("0 GP", JLabel.RIGHT);
            valLabel.setForeground(ColorScheme.BRAND_ORANGE);
            valLabels.put(itemId, valLabel);

            JPanel leftSide = new JPanel(new BorderLayout());
            leftSide.add(iconLabel, BorderLayout.WEST);
            leftSide.add(qtyLabel, BorderLayout.CENTER);

            row.add(leftSide, BorderLayout.WEST);
            row.add(valLabel, BorderLayout.EAST);
            runeListPanel.add(row);
        }

        JPanel summaryPanel = new JPanel(new BorderLayout());
        summaryPanel.setBorder(new EmptyBorder(15, 0, 0, 0));

        JPanel totalBox = new JPanel(new BorderLayout());
        totalBox.add(coinImageLabel, BorderLayout.WEST);
        totalBox.add(totalValueLabel, BorderLayout.CENTER);

        summaryPanel.add(new JLabel("Total Buy-back Value:"), BorderLayout.NORTH);
        summaryPanel.add(totalBox, BorderLayout.CENTER);

        add(runeListPanel, BorderLayout.CENTER);
        add(summaryPanel, BorderLayout.SOUTH);
    }

    public void refreshRuneMetrics(int itemId, long quantity)
    {
        JLabel qtyLabel = qtyLabels.get(itemId);
        JLabel valLabel = valLabels.get(itemId);

        if (qtyLabel != null && valLabel != null)
        {
            qtyLabel.setText(" " + QuantityFormatter.formatNumber(quantity));
            long totalPrice = quantity * AliMorrisaneValuesPlugin.ALI_RUNE_PRICES.get(itemId);
            valLabel.setText(QuantityFormatter.formatNumber(totalPrice) + " GP");
        }
    }

    public void refreshTotalLumpSum(long netWorthSum)
    {
        int cleanAmountValue = (netWorthSum > Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int) netWorthSum;

        coinImageLabel.setIcon(null);
        itemManager.getImage(995, cleanAmountValue, false).addTo(coinImageLabel);

        totalValueLabel.setText(" " + QuantityFormatter.formatNumber(netWorthSum) + " GP");
    }
}
