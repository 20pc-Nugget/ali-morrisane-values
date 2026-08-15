package com.alimorrisanevalues;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(AliMorrisaneValuesConfig.GROUP_NAME)
public interface AliMorrisaneValuesConfig extends Config
{
    String GROUP_NAME = "aliMorrisaneValues";

    @ConfigItem(
            keyName = "showInventoryOverlay",
            name = "Show inventory overlay",
            description = "Renders total store buy-back valuations directly on top of item stacks.",
            position = 1
    )
    default boolean showInventoryOverlay()
    {
        return true;
    }

    @ConfigItem(
            keyName = "showSidebarPanel",
            name = "Show navigation panel",
            description = "Integrates a dedicated inventory readout navigation view into the sidebar.",
            position = 2
    )
    default boolean showSidebarPanel()
    {
        return true;
    }
}
