package uwu.ramona.bazaar;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import uwu.ramona.bazaar.config.BazaarConfig;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.world.World;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.event.ClickEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import net.minecraftforge.fml.common.versioning.DefaultArtifactVersion;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Mouse;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Mod(modid = BazaarFlip.MODID, name = BazaarFlip.NAME, version = BazaarFlip.VERSION)
public class BazaarFlip {
    public static final String MODID = "BazaarFlipMod";
    public static final String NAME = "BazaarFlip";
    public static final String VERSION = "1.0.4";
    public static BazaarConfig config;

    private static BazaarMonitor bazaarMonitor;
    private static boolean hasCheckedForUpdate = false;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        config = new BazaarConfig();
        bazaarMonitor = new BazaarMonitor();
        MinecraftForge.EVENT_BUS.register(bazaarMonitor);
    }

    @SideOnly(Side.CLIENT)
    public static class BazaarMonitor {
        private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        private static boolean isMonitoring = false;
        private static List<BazaarItem> topItemsByPercentage = new ArrayList<>();
        private static List<BazaarItem> topItemsByMoney = new ArrayList<>();
        private static boolean isBazaarGuiOpen = false;
        private static String lastGuiTitle = "";
        private static long lastGuiCheck = 0;
        private static final long GUI_CHECK_DELAY = 500;

        private static int guiX = BazaarConfig.guiX;
        private static int guiY = BazaarConfig.guiY;
        private static boolean isDragging = false;
        private static int dragOffsetX = 0;
        private static int dragOffsetY = 0;
        private static int guiScale = BazaarConfig.guiScale;

        public BazaarMonitor() {
            startMonitoring();
        }


        private static class BazaarItem {
            String id;
            double sellPrice, buyPrice, profit, profitMargin, totalBuyCost, totalSellValue, totalProfit;

            BazaarItem(String id, double sellPrice, double buyPrice, double profit, double profitMargin,
                       double totalBuyCost, double totalSellValue, double totalProfit) {
                this.id = id;
                this.sellPrice = sellPrice;
                this.buyPrice = buyPrice;
                this.profit = profit;
                this.profitMargin = profitMargin;
                this.totalBuyCost = totalBuyCost;
                this.totalSellValue = totalSellValue;
                this.totalProfit = totalProfit;
            }
        }

        private void startMonitoring() {
            if (isMonitoring || !BazaarConfig.enableMonitoring) return;
            isMonitoring = true;
            scheduler.scheduleAtFixedRate(() -> {
                try {
                    performBazaarCheck();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, 0, BazaarConfig.updateInterval, TimeUnit.SECONDS);
        }



        @SubscribeEvent
        public void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.thePlayer == null || mc.theWorld == null) return;

            long currentTime = System.currentTimeMillis();
            if (currentTime - lastGuiCheck < GUI_CHECK_DELAY) return;
            lastGuiCheck = currentTime;

            if (mc.currentScreen instanceof GuiChest) {
                GuiChest chest = (GuiChest) mc.currentScreen;
                if (chest.inventorySlots instanceof ContainerChest) {
                    ContainerChest containerChest = (ContainerChest) chest.inventorySlots;
                    String guiTitle = containerChest.getLowerChestInventory().getDisplayName().getUnformattedText();

                    if (!guiTitle.equals(lastGuiTitle)) {
                        lastGuiTitle = guiTitle;
                        if (guiTitle.toLowerCase().contains("bazaar")) {
                            if (!isBazaarGuiOpen) {
                                isBazaarGuiOpen = true;
                                mc.thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "Bazaar monitoring active"));
                            }
                        }
                    }
                }
            } else {
                if (isBazaarGuiOpen) {
                    isBazaarGuiOpen = false;
                    lastGuiTitle = "";
                }
            }
        }


        private void performBazaarCheck() {
            try {
                URL url = new URL("https://api.hypixel.net/skyblock/bazaar");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                if (conn.getResponseCode() == 200) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }

                        JsonParser parser = new JsonParser();
                        JsonObject data = parser.parse(response.toString()).getAsJsonObject();
                        JsonObject products = data.getAsJsonObject("products");

                        List<BazaarItem> allItems = new ArrayList<>();
                        for (Map.Entry<String, com.google.gson.JsonElement> entry : products.entrySet()) {
                            String productId = entry.getKey();
                            JsonObject product = entry.getValue().getAsJsonObject();
                            JsonObject quickStatus = product.getAsJsonObject("quick_status");

                            double buyPrice = quickStatus.get("buyPrice").getAsDouble();
                            double sellPrice = quickStatus.get("sellPrice").getAsDouble();
                            double buyVolume = quickStatus.get("buyVolume").getAsDouble();
                            double sellVolume = quickStatus.get("sellVolume").getAsDouble();

                            if (buyVolume > BazaarConfig.minBuyVolume &&
                                    sellVolume > BazaarConfig.minSellVolume &&
                                    sellPrice > 0) {
                                double profit = buyPrice - sellPrice;
                                double profitMargin = (profit / sellPrice) * 100;
                                double totalBuyCost = sellPrice * 71680;
                                double totalSellValue = buyPrice * 71680;
                                double totalProfit = totalSellValue - totalBuyCost;

                                if (totalBuyCost <= BazaarConfig.maxSpendLimit) {
                                    if (profitMargin >= 33) {
                                        allItems.add(new BazaarItem(productId, sellPrice, buyPrice, profit,
                                                profitMargin, totalBuyCost, totalSellValue, totalProfit));
                                    }
                                }
                            }
                        }

                        synchronized (this) {
                            topItemsByPercentage = new ArrayList<>(allItems);
                            topItemsByMoney = new ArrayList<>(allItems);

                            topItemsByPercentage.sort((a, b) -> Double.compare(b.profitMargin, a.profitMargin));
                            topItemsByMoney.sort((a, b) -> Double.compare(b.totalProfit, a.totalProfit));

                            if (topItemsByPercentage.size() > 5) {
                                topItemsByPercentage = topItemsByPercentage.subList(0, 5);
                            }
                            if (topItemsByMoney.size() > 5) {
                                topItemsByMoney = topItemsByMoney.subList(0, 5);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        @SubscribeEvent
        public void onRenderGui(GuiScreenEvent.DrawScreenEvent.Post event) {
            if (!isBazaarGuiOpen) return;

            Minecraft mc = Minecraft.getMinecraft();
            ScaledResolution scaled = new ScaledResolution(mc);

            int mouseX = Mouse.getX() * event.gui.width / mc.displayWidth;
            int mouseY = event.gui.height - Mouse.getY() * event.gui.height / mc.displayHeight - 1;

            float scaleFactor = getScaleFactor();
            int scaledX = (int) (BazaarConfig.guiX / scaleFactor);
            int scaledY = (int) (BazaarConfig.guiY / scaleFactor);

            if (Mouse.isButtonDown(0)) {
                if (!isDragging) {
                    boolean inTitleBar = mouseX >= BazaarConfig.guiX &&
                            mouseX <= BazaarConfig.guiX + 400 &&
                            mouseY >= BazaarConfig.guiY &&
                            mouseY <= BazaarConfig.guiY + 20;

                    if (inTitleBar) {
                        isDragging = true;
                        dragOffsetX = mouseX - BazaarConfig.guiX;
                        dragOffsetY = mouseY - BazaarConfig.guiY;
                    }
                } else {
                    BazaarConfig.guiX = mouseX - dragOffsetX;
                    BazaarConfig.guiY = mouseY - dragOffsetY;
                }
            } else {
                isDragging = false;
            }

            GlStateManager.pushMatrix();
            GlStateManager.scale(scaleFactor, scaleFactor, 1);

            int guiWidth = 400;
            int guiHeight = 20 + (Math.max(topItemsByPercentage.size(), topItemsByMoney.size()) * 100);

            drawRect(scaledX, scaledY, scaledX + guiWidth, scaledY + guiHeight + 20, BazaarConfig.getBackgroundColor());

            mc.fontRendererObj.drawStringWithShadow("§l§6Bazaar Flips (Profit %)", scaledX + 5, scaledY + 5, BazaarConfig.getTextColor());
            mc.fontRendererObj.drawStringWithShadow("§l§6Bazaar Flips (Total Profit)", scaledX + 205, scaledY + 5, BazaarConfig.getTextColor());
            synchronized (this) {
                int y = scaledY + 25;
                for (int i = 0; i < 5; i++) {
                    if (i < topItemsByPercentage.size()) {
                        BazaarItem item = topItemsByPercentage.get(i);
                        drawItemInfo(mc, item, scaledX + 5, y, true);
                    }
                    if (i < topItemsByMoney.size()) {
                        BazaarItem item = topItemsByMoney.get(i);
                        drawItemInfo(mc, item, scaledX + 205, y, false);
                    }
                    y += 100;
                }
            }

            GlStateManager.popMatrix();
        }


        private String formatNumber(double value) {
            if (value >= 1_000_000_000) {
                return String.format("%.2fB", value / 1_000_000_000);
            } else if (value >= 1_000_000) {
                return String.format("%.2fM", value / 1_000_000);
            } else if (value >= 1_000) {
                return String.format("%.2fK", value / 1_000);
            } else {
                return String.format("%.2f", value);
            }
        }


        private void drawItemInfo(Minecraft mc, BazaarItem item, int x, int y, boolean isPercentage) {
            int profitColor = getProfitColor(item.profitMargin);
            int normalTextColor = BazaarConfig.getTextColor();
            int profitColorWithOpacity = (profitColor & 0x00FFFFFF) | ((BazaarConfig.textOpacity & 0xFF) << 24);

            int lineHeight = 10;
            int initialY = y;
            mc.fontRendererObj.drawStringWithShadow("§l" + item.id, x, y, profitColorWithOpacity);
            y += lineHeight + 5;
            mc.fontRendererObj.drawStringWithShadow(
                    String.format("Buy: %s  |  Sell: %s", formatNumber(item.sellPrice), formatNumber(item.buyPrice)),
                    x, y, normalTextColor);
            y += lineHeight;
            if (isPercentage) {
                mc.fontRendererObj.drawStringWithShadow(
                        String.format("Profit: %s (%.2f%%)", formatNumber(item.profit), item.profitMargin),
                        x, y, profitColorWithOpacity);
            } else {
                mc.fontRendererObj.drawStringWithShadow(
                        String.format("Total Profit: %s", formatNumber(item.totalProfit)),
                        x, y, profitColorWithOpacity);
            }
            y += lineHeight;
            mc.fontRendererObj.drawStringWithShadow(
                    String.format("Total Buy Cost: %s", formatNumber(item.totalBuyCost)),
                    x, y, normalTextColor);
            y += lineHeight;
            mc.fontRendererObj.drawStringWithShadow(
                    String.format("Total Sell Value: %s", formatNumber(item.totalSellValue)),
                    x, y, normalTextColor);
            y += lineHeight;
            int totalHeight = y - initialY;
            if (totalHeight < 100) {
                y += (100 - totalHeight);
            }
        }





        private static int getProfitColor(double profitMargin) {
            if (profitMargin >= 10000) return 0xFF0000;
            if (profitMargin >= 7500) return 0xFF5555;
            if (profitMargin >= 5000) return 0xFF5555;
            if (profitMargin >= 2500) return 0x55FFFF;
            if (profitMargin >= 100) return 0xFFFF55;
            if (profitMargin >= 33) return 0x55FF55;
            return BazaarConfig.textColor.getRGB();
        }

        private static float getScaleFactor() {
            switch (BazaarConfig.guiScale) {
                case 1: return 0.5f;
                case 2: return 0.75f;
                case 3: return 1.0f;
                case 4: return 1.25f;
                case 5: return 1.5f;
                default: return 1.0f;
            }
        }

        private void drawRect(int left, int top, int right, int bottom, int color) {
            if (left > right) { int temp = left; left = right; right = temp; }
            if (top > bottom) { int temp = top; top = bottom; bottom = temp; }

            GlStateManager.enableBlend();
            GlStateManager.disableTexture2D();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            GlStateManager.color((color >> 16 & 255) / 255.0F, (color >> 8 & 255) / 255.0F, (color & 255) / 255.0F, (color >> 24 & 255) / 255.0F);

            Tessellator.getInstance().getWorldRenderer().begin(7, DefaultVertexFormats.POSITION);
            Tessellator.getInstance().getWorldRenderer().pos(left, bottom, 0.0D).endVertex();
            Tessellator.getInstance().getWorldRenderer().pos(right, bottom, 0.0D).endVertex();
            Tessellator.getInstance().getWorldRenderer().pos(right, top, 0.0D).endVertex();
            Tessellator.getInstance().getWorldRenderer().pos(left, top, 0.0D).endVertex();
            Tessellator.getInstance().draw();

            GlStateManager.enableTexture2D();
            GlStateManager.disableBlend();
        }

    }
}