package net.runelite.client.plugins.alimorrisanevalues;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.util.QuantityFormatter;

public class AliMorrisaneValuesPanel extends PluginPanel {
    private final ItemManager itemManager;
    private final Map<Integer, JLabel> valueLabels = new HashMap<>();
    private final JLabel totalValueLabel = new JLabel("0 GP", JLabel.RIGHT);

    // Dynamic Coins Graphics Component
    private final JLabel coinImageLabel = new JLabel();

    private static final int[] ELEMENTAL_RUNES = {556, 555, 557, 554}; // Air, Water, Earth, Fire
    private static final int[] CATALYTIC_RUNES = {558, 559, 562, 560, 565, 566, 563, 564, 561}; // Mind, Body, Chaos, Death, Blood, Soul, Law, Cosmic, Nature

    @Inject
    public AliMorrisaneValuesPanel(final ItemManager itemManager) {
        super();
        this.itemManager = itemManager;

        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(10, 10, 10, 10));
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        final JPanel container = new JPanel(new GridBagLayout());
        container.setBackground(ColorScheme.DARK_GRAY_COLOR);

        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 5, 0);

        // 1. Core Header
        final JLabel mainHeaderTitle = new JLabel("Ali Morrisane Values");
        mainHeaderTitle.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        mainHeaderTitle.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        mainHeaderTitle.setBorder(new EmptyBorder(0, 0, 10, 0));
        container.add(mainHeaderTitle, gbc);
        gbc.gridy++;

        // 2. Elemental Section Header
        addSectionHeader(container, "Elemental Runes", gbc);
        for (int itemId : ELEMENTAL_RUNES) {
            addRuneRow(container, itemManager, itemId, gbc);
        }

        // 3. Catalytic Section Header
        gbc.insets = new Insets(15, 0, 5, 0);
        addSectionHeader(container, "Catalytic Runes", gbc);
        gbc.insets = new Insets(0, 0, 5, 0);
        for (int itemId : CATALYTIC_RUNES) {
            addRuneRow(container, itemManager, itemId, gbc);
        }

        add(container, BorderLayout.NORTH);

        // 4. Professional Summary Footer Card Layout (Swapping text out for Coins)
        final JPanel summaryFooterPanel = new JPanel(new BorderLayout());
        summaryFooterPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        summaryFooterPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Initial default 0gp single coin asset load placement bounds configuration
        itemManager.getImage(995, 0, false).addTo(coinImageLabel);

        totalValueLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        totalValueLabel.setForeground(ColorScheme.BRAND_ORANGE);

        summaryFooterPanel.add(coinImageLabel, BorderLayout.WEST);
        summaryFooterPanel.add(totalValueLabel, BorderLayout.CENTER);

        add(summaryFooterPanel, BorderLayout.SOUTH);
    }

    private void addSectionHeader(JPanel container, String title, GridBagConstraints gbc) {
        final JLabel sectionLabel = new JLabel(title);
        sectionLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        sectionLabel.setForeground(ColorScheme.BRAND_ORANGE);
        sectionLabel.setBorder(new EmptyBorder(5, 0, 2, 0));
        container.add(sectionLabel, gbc);
        gbc.gridy++;
    }

    private void addRuneRow(JPanel container, ItemManager itemManager, int itemId, GridBagConstraints gbc) {
        final JPanel entryRow = new JPanel(new BorderLayout());
        entryRow.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        entryRow.setBorder(new EmptyBorder(4, 6, 4, 6));

        final JLabel graphicWrapper = new JLabel();
        itemManager.getImage(itemId).addTo(graphicWrapper);

        final JLabel evaluationLabel = new JLabel("0 GP", JLabel.RIGHT);
        evaluationLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        evaluationLabel.setForeground(ColorScheme.PROGRESS_COMPLETE_COLOR);

        entryRow.add(graphicWrapper, BorderLayout.WEST);
        entryRow.add(evaluationLabel, BorderLayout.CENTER);

        container.add(entryRow, gbc);
        valueLabels.put(itemId, evaluationLabel);
        gbc.gridy++;
    }

    public void refreshRuneMetrics(final int itemId, final long quantity) {
        final JLabel metricLabel = valueLabels.get(itemId);
        if (metricLabel != null) {
            final long flatRatePrice = AliMorrisaneValuesPlugin.ALI_RUNE_PRICES.getOrDefault(itemId, 0);
            metricLabel.setText(QuantityFormatter.formatNumber(flatRatePrice * quantity) + " GP");
        }
    }

    public void refreshTotalLumpSum(final long netWorthSum)
    {
        int cleanAmountValue = (netWorthSum > Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int) netWorthSum;

        coinImageLabel.setIcon(null);
        itemManager.getImage(995, cleanAmountValue, false).addTo(coinImageLabel);

        totalValueLabel.setText(QuantityFormatter.formatNumber(netWorthSum) + " GP");
    }

}