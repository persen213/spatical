package dev.spatial.inversion;

import com.mojang.logging.LogUtils;
import io.github.pinchan4273.reacademycraft.api.AbilityId;
import io.github.pinchan4273.reacademycraft.api.ActivationKind;
import io.github.pinchan4273.reacademycraft.api.CategoryDefinition;
import io.github.pinchan4273.reacademycraft.api.Presentation;
import io.github.pinchan4273.reacademycraft.api.SkillDefinition;
import io.github.pinchan4273.reacademycraft.api.SkillRequirement;
import io.github.pinchan4273.reacademycraft.api.TreePosition;
import io.github.pinchan4273.reacademycraft.api.forge.RegisterAbilitiesEvent;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

/**
 * Spatial Inversion: an official Level 5 Esper add-on category for Re:AcademyCraft 1.5.0.
 */
@Mod(SpatialInversionMod.MOD_ID)
public final class SpatialInversionMod {
    public static final String MOD_ID = "spatialinversion";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);

    /** The factor item used in the Ability Developer to obtain the Spatial Inversion category. */
    public static final RegistryObject<Item> FACTOR = ITEMS.register("spatial_inversion_factor",
            () -> new Item(new Item.Properties().stacksTo(16)));

    static AbilityId id(String path) {
        return AbilityId.parse(MOD_ID + ":" + path);
    }

    public static final AbilityId CATEGORY = id("spatial_inversion"),
            SPATIAL_LINK = id("spatial_link"),
            VECTOR_REDIRECT = id("vector_redirect"),
            SPATIAL_EXCHANGE = id("spatial_exchange"),
            DISTORTION_FIELD = id("distortion_field"),
            INVERSION_STRIKE = id("inversion_strike"),
            FOLD_STEP = id("fold_step"),
            SPATIAL_CAGE = id("spatial_cage"),
            ABSOLUTE_INVERSION = id("absolute_inversion");

    public SpatialInversionMod() {
        var bus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(bus);
        bus.addListener(this::setup);
        bus.addListener(this::registerAbilities);
        bus.addListener(this::creativeTab);
    }

    private void setup(final FMLCommonSetupEvent event) {
        event.enqueueWork(SpatialNetwork::init);
    }

    private void creativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(FACTOR);
        }
    }

    private static Presentation presentation(AbilityId skill) {
        return new Presentation(MOD_ID + ".skill." + skill.path(), MOD_ID + ".skill." + skill.path() + ".desc", null);
    }

    private static SkillDefinition skill(AbilityId id, int level, List<SkillRequirement> requires, ActivationKind kind, float x, float y, @Nullable AbilityId parent) {
        return new SkillDefinition(id, CATEGORY, level, requires, kind, presentation(id), new TreePosition(x, y, parent));
    }

    private static SkillRequirement req(AbilityId skill, float proficiency) {
        return new SkillRequirement(skill, proficiency);
    }

    private void registerAbilities(RegisterAbilitiesEvent event) {
        var abilities = event.registrar(MOD_ID);

        // Register Category: 8 skills distributed across Level 1..5: (1, 2, 2, 2, 1)
        abilities.category(new CategoryDefinition(
                CATEGORY,
                id("spatial_inversion_factor"),
                List.of(1, 2, 2, 2, 1),
                new Presentation(MOD_ID + ".ability.spatial_inversion", null, null)
        ));

        // Level 1 (1 skill)
        abilities.skill(skill(SPATIAL_LINK, 1, List.of(), ActivationKind.INSTANT, 20, 55, null));

        // Level 2 (2 skills)
        abilities.skill(skill(VECTOR_REDIRECT, 2, List.of(req(SPATIAL_LINK, 0.25f)), ActivationKind.INSTANT, 60, 25, SPATIAL_LINK));
        abilities.skill(skill(FOLD_STEP, 2, List.of(req(SPATIAL_LINK, 0.25f)), ActivationKind.INSTANT, 60, 85, SPATIAL_LINK));

        // Level 3 (2 skills)
        abilities.skill(skill(SPATIAL_EXCHANGE, 3, List.of(req(VECTOR_REDIRECT, 0.35f)), ActivationKind.INSTANT, 105, 25, VECTOR_REDIRECT));
        abilities.skill(skill(DISTORTION_FIELD, 3, List.of(req(FOLD_STEP, 0.35f)), ActivationKind.INSTANT, 105, 85, FOLD_STEP));

        // Level 4 (2 skills)
        abilities.skill(skill(INVERSION_STRIKE, 4, List.of(req(SPATIAL_EXCHANGE, 0.45f)), ActivationKind.INSTANT, 150, 30, SPATIAL_EXCHANGE));
        abilities.skill(skill(SPATIAL_CAGE, 4, List.of(req(DISTORTION_FIELD, 0.45f)), ActivationKind.INSTANT, 150, 80, DISTORTION_FIELD));

        // Level 5 (1 skill - Ultimate)
        abilities.skill(skill(ABSOLUTE_INVERSION, 5, List.of(req(INVERSION_STRIKE, 0.55f), req(SPATIAL_CAGE, 0.55f)), ActivationKind.INSTANT, 200, 55, INVERSION_STRIKE));

        // Register Behaviors
        var behaviors = event.behaviors(MOD_ID);
        behaviors.behavior(SPATIAL_LINK, new SpatialLinkSkill());
        behaviors.behavior(VECTOR_REDIRECT, new VectorRedirectSkill());
        behaviors.behavior(FOLD_STEP, new FoldStepSkill());
        behaviors.behavior(SPATIAL_EXCHANGE, new SpatialExchangeSkill());
        behaviors.behavior(DISTORTION_FIELD, new DistortionFieldSkill());
        behaviors.behavior(INVERSION_STRIKE, new InversionStrikeSkill());
        behaviors.behavior(SPATIAL_CAGE, new SpatialCageSkill());
        behaviors.behavior(ABSOLUTE_INVERSION, new AbsoluteInversionSkill());
    }
}
