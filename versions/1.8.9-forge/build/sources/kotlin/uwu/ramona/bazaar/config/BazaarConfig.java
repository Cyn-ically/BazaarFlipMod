package uwu.ramona.bazaar.config;

import cc.polyfrost.oneconfig.config.core.OneColor;
import uwu.ramona.bazaar.ExampleMod;
import cc.polyfrost.oneconfig.config.Config;
import cc.polyfrost.oneconfig.config.annotations.*;
import cc.polyfrost.oneconfig.config.data.Mod;
import cc.polyfrost.oneconfig.config.data.ModType;
import cc.polyfrost.oneconfig.config.data.OptionSize;

public class BazaarConfig extends Config {

    @Switch(name = "Enable Monitoring", size = OptionSize.SINGLE)
    public static boolean enableMonitoring = false;

    @Text(name = "API Key", placeholder = "Enter your Hypixel API key here")
    public static String apiKey = "";

    @Slider(name = "Update Interval (Seconds)", min = 10, max = 60, step = 1)
    public static int updateInterval = 20;

    @Slider(name = "GUI X Position", min = 0, max = 1920, step = 1)
    public static int guiX = 100;

    @Slider(name = "GUI Y Position", min = 0, max = 1080, step = 1)
    public static int guiY = 100;

    @Slider(name = "GUI Scale", min = 1, max = 5, step = 1)
    public static int guiScale = 3;

    @Slider(
            name = "Minimum Buy Volume",
            min = 100000,
            max = 2000000,
            step = 100000,
            description = "Minimum buy volume required to show an item"
    )
    public static int minBuyVolume = 700000;

    @Slider(
            name = "Minimum Sell Volume",
            min = 100000,
            max = 2000000,
            step = 100000,
            description = "Minimum sell volume required to show an item"
    )
    public static int minSellVolume = 700000;

    @Color(name = "Text Color")
    public static OneColor textColor = new OneColor(255, 255, 255);

    @Slider(name = "Text Opacity", min = 0, max = 255, step = 1)
    public static int textOpacity = 255;

    @Color(name = "Background Color")
    public static OneColor backgroundColor = new OneColor(0, 0, 0);

    @Slider(name = "Background Opacity", min = 0, max = 255, step = 1)
    public static int backgroundOpacity = 224;

    public BazaarConfig() {
        super(new Mod(ExampleMod.NAME, ModType.UTIL_QOL), ExampleMod.MODID + ".json");
        initialize();
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
}