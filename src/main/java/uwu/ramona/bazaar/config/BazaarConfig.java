package uwu.ramona.bazaar.config;

import cc.polyfrost.oneconfig.config.Config;
import cc.polyfrost.oneconfig.config.annotations.*;
import cc.polyfrost.oneconfig.config.data.Mod;
import cc.polyfrost.oneconfig.config.data.ModType;
import cc.polyfrost.oneconfig.config.data.OptionSize;
import cc.polyfrost.oneconfig.config.core.OneColor;
import uwu.ramona.bazaar.BazaarFlip;

public class BazaarConfig extends Config {

    public static int ALERT_MODE_NONE = 0;
    public static int ALERT_MODE_CHAT = 1;
    public static int ALERT_MODE_DISCORD = 2;
    public static int ALERT_MODE_BOTH = 3;

    @Switch(name = "Enable Monitoring", size = OptionSize.SINGLE)
    public static boolean enableMonitoring = false;

    @Slider(name = "Update Interval (Seconds)", min = 10, max = 60, step = 1)
    public static int updateInterval = 20;

    @Slider(name = "GUI X Position", min = 0, max = 1920, step = 1)
    public static int guiX = 100;

    @Slider(name = "GUI Y Position", min = 0, max = 1080, step = 1)
    public static int guiY = 100;

    @Slider(name = "Maximum Spend Limit", min = 100000, max = 100000000, step = 100000)
    public static int maxSpendLimit = 100000000;

    @Slider(name = "GUI Scale", min = 1, max = 5, step = 1)
    public static int guiScale = 3;

    @Slider(name = "Minimum Buy Volume", min = 100000, max = 2000000, step = 100000, description = "Minimum buy volume required to show an item")
    public static int minBuyVolume = 700000;

    @Slider(name = "Minimum Sell Volume", min = 100000, max = 2000000, step = 100000, description = "Minimum sell volume required to show an item")
    public static int minSellVolume = 700000;

    @Slider(name = "Bazaar GUI - Numbers of Items by Percentage", min = 1, max = 10, step = 1)
    public static int bazaarGuiTopItemsByPercentageCount = 5;

    @Slider(name = "Bazaar GUI - Numbers of Items by Total Profit", min = 1, max = 10, step = 1)
    public static int bazaarGuiTopItemsByMoneyCount = 5;

    @Color(name = "Text Color")
    public static OneColor textColor = new OneColor(255, 255, 255);

    @Slider(name = "Text Opacity", min = 0, max = 255, step = 1)
    public static int textOpacity = 255;

    @Color(name = "Background Color")
    public static OneColor backgroundColor = new OneColor(0, 0, 0);

    @Slider(name = "Background Opacity", min = 0, max = 255, step = 1)
    public static int backgroundOpacity = 224;

    @Switch(name = "Enable Background Monitoring", size = OptionSize.SINGLE, description = "Allows monitoring even when Bazaar GUI is not open")
    public static boolean enableBackgroundMonitoring = false;

    @Slider(name = "Background Monitoring Interval (Seconds)", min = 60, max = 600, step = 30, description = "Time between background monitoring checks")
    public static int backgroundMonitoringInterval = 180;

    @Slider(name = "Minimum Profit Percentage for Background Alert", min = 10, max = 100, step = 1, description = "Minimum profit percentage to trigger an alert")
    public static int backgroundAlertThreshold = 33;

    @Slider(name = "B-Monitoring - Numbers of Items by Percentage", min = 1, max = 10, step = 1)
    public static int backgroundTopItemsByPercentageCount = 3;

    @Slider(name = "B-Monitoring - Numbers of Items by Total Profit", min = 1, max = 10, step = 1)
    public static int backgroundTopItemsByMoneyCount = 3;

    @Dropdown(name = "Alert Mode", options = {"None", "Chat", "Discord", "Both"})
    public static int alertMode = ALERT_MODE_BOTH;

    @Text(name = "Webhook URL", size = OptionSize.DUAL, description = "URL for sending alerts via webhook")
    public static String webhookUrl = "";

    public BazaarConfig() {
        super(new Mod(BazaarFlip.NAME, ModType.UTIL_QOL), BazaarFlip.MODID + ".json");
        initialize();

        addDependency("webhookUrl", () -> alertMode != ALERT_MODE_NONE);
        addDependency("updateInterval", () -> enableMonitoring);
        addDependency("guiX", () -> enableMonitoring);
        addDependency("guiY", () -> enableMonitoring);
        addDependency("maxSpendLimit", () -> enableMonitoring);
        addDependency("guiScale", () -> enableMonitoring);
        addDependency("minBuyVolume", () -> enableMonitoring);
        addDependency("minSellVolume", () -> enableMonitoring);
        addDependency("bazaarGuiTopItemsByPercentageCount", () -> enableMonitoring);
        addDependency("bazaarGuiTopItemsByMoneyCount", () -> enableMonitoring);
        addDependency("textColor", () -> enableMonitoring);
        addDependency("textOpacity", () -> enableMonitoring);
        addDependency("backgroundColor", () -> enableMonitoring);
        addDependency("backgroundOpacity", () -> enableMonitoring);
        addDependency("backgroundMonitoringInterval", () -> enableBackgroundMonitoring);
        addDependency("backgroundAlertThreshold", () -> enableBackgroundMonitoring);
        addDependency("backgroundTopItemsByPercentageCount", () -> enableBackgroundMonitoring);
        addDependency("backgroundTopItemsByMoneyCount", () -> enableBackgroundMonitoring);
        addDependency("alertMode", () -> enableBackgroundMonitoring);
        addDependency("webhookUrl", () -> enableBackgroundMonitoring);
    }

    public static int getBackgroundColor() {
        return (backgroundOpacity << 24) |
                (backgroundColor.getRed() << 16) |
                (backgroundColor.getGreen() << 8) |
                backgroundColor.getBlue();
    }

    public static int getTextColor() {
        return (textOpacity << 24) |
                (textColor.getRed() << 16) |
                (textColor.getGreen() << 8) |
                textColor.getBlue();
    }

    public static boolean isDiscordAlertEnabled() {
        return alertMode == ALERT_MODE_DISCORD || alertMode == ALERT_MODE_BOTH;
    }

    public static boolean isChatAlertEnabled() {
        return alertMode == ALERT_MODE_CHAT || alertMode == ALERT_MODE_BOTH;
    }
}
