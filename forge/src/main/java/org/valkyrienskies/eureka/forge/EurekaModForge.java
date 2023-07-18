package org.valkyrienskies.eureka.forge;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.common.data.BlockTagsProvider;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.DeferredRegister;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.valkyrienskies.core.impl.config.VSConfigClass;
import org.valkyrienskies.eureka.EurekaBlockEntities;
import org.valkyrienskies.eureka.EurekaConfig;
import org.valkyrienskies.eureka.EurekaItems;
import org.valkyrienskies.eureka.EurekaMod;
import org.valkyrienskies.eureka.block.WoodType;
import org.valkyrienskies.eureka.blockentity.renderer.ShipHelmBlockEntityRenderer;
import org.valkyrienskies.eureka.blockentity.renderer.WheelModels;
import org.valkyrienskies.mod.compat.clothconfig.VSClothConfig;

@Mod(EurekaMod.MOD_ID)
public class EurekaModForge {
    private boolean handledClientSetup = false;
    static IEventBus MOD_BUS;

    public EurekaModForge() {
        // Submit our event bus to let architectury register our content on the right time
        MOD_BUS = FMLJavaModLoadingContext.get().getModEventBus();

        final boolean isClient = FMLEnvironment.dist.isClient();

        if (isClient) {
            MOD_BUS.addListener(this::clientSetup);
            MOD_BUS.addListener(this::onModelRegistry);
            MOD_BUS.addListener(this::onModelBaked);
            MOD_BUS.addListener(this::entityRenderers);
        }

        MOD_BUS.addListener(this::onGatherData);

        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((Minecraft client, Screen parent) ->
                        VSClothConfig.createConfigScreenFor(parent,
                                VSConfigClass.Companion.getRegisteredConfig(EurekaConfig.class)))
        );

        EurekaMod.init();

        carryOnModSupport();

        DeferredRegister<CreativeModeTab> deferredRegister = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, EurekaMod.MOD_ID);
        deferredRegister.register("general", EurekaItems.INSTANCE::createCreativeTab);
        deferredRegister.register(MOD_BUS);
    }

    private static void carryOnModSupport() {
        if (ModList.get().isLoaded("carryon")) {

            final Logger LOGGER = LogManager.getLogger(EurekaMod.MOD_ID);

            LOGGER.info(EurekaMod.MOD_ID + ": Carry On was detected, sending blacklist.");

            final var exclusions = new String[] {
                "vs_eureka:oak_ship_helm",
                "vs_eureka:spruce_ship_helm",
                "vs_eureka:birch_ship_helm",
                "vs_eureka:jungle_ship_helm",
                "vs_eureka:acacia_ship_helm",
                "vs_eureka:dark_oak_ship_helm",
                "vs_eureka:crimson_ship_helm",
                "vs_eureka:warped_ship_helm"
            };

            for (final String item : exclusions) {
                InterModComms.sendTo("carryon", "blacklistBlock", () -> {
                    LOGGER.debug("carryon->blacklistBlock->" + item);
                    return item;
                });
            }
        }
    }

    void clientSetup(final FMLClientSetupEvent event) {
        if (handledClientSetup) return;
        handledClientSetup = true;

        EurekaMod.initClient();
    }

    void entityRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                EurekaBlockEntities.INSTANCE.getSHIP_HELM().get(),
                ShipHelmBlockEntityRenderer::new
        );
    }

    void onModelRegistry(final ModelEvent.RegisterAdditional event) {
        for (WoodType woodType : WoodType.values()) {
            event.register(new ResourceLocation(EurekaMod.MOD_ID, "block/" + woodType.getResourceName() + "_ship_helm_wheel"));
        }
    }

    void onModelBaked(final ModelEvent.BakingCompleted event) {
        WheelModels.INSTANCE.setModelGetter(woodType -> event.getModelBakery().getBakedTopLevelModels()
                .getOrDefault(
                        new ResourceLocation(EurekaMod.MOD_ID, "block/" + woodType.getResourceName() + "_ship_helm_wheel"),
                        Minecraft.getInstance().getModelManager().getMissingModel()
                ));
    }

    void onGatherData(final GatherDataEvent event) {
        final BlockTagsProvider blockTagProvider = new EurekaBlockTagsProvider(
                event.getGenerator().getPackOutput(),
                event.getLookupProvider(),
                EurekaMod.MOD_ID,
                event.getExistingFileHelper()
        );
        event.getGenerator().addProvider(event.includeServer(), blockTagProvider);
    }
}
