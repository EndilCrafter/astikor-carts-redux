package de.mennomax.astikorcarts;

import de.mennomax.astikorcarts.client.ClientInitializer;
import de.mennomax.astikorcarts.entity.CargoCartEntity;
import de.mennomax.astikorcarts.entity.MobCartEntity;
import de.mennomax.astikorcarts.entity.PlowCartEntity;
import de.mennomax.astikorcarts.entity.PostilionEntity;
import de.mennomax.astikorcarts.inventory.container.PlowCartContainer;
import de.mennomax.astikorcarts.item.CartItem;
import de.mennomax.astikorcarts.network.NetBuilder;
import de.mennomax.astikorcarts.network.serverbound.ActionKeyMessage;
import de.mennomax.astikorcarts.network.serverbound.OpenCargoCartMessage;
import de.mennomax.astikorcarts.network.serverbound.ToggleSlowMessage;
import de.mennomax.astikorcarts.network.clientbound.CartingJukeboxMessage;
import de.mennomax.astikorcarts.network.clientbound.UpdateDrawnMessage;
import de.mennomax.astikorcarts.server.ServerInitializer;
import de.mennomax.astikorcarts.util.DefRegister;
import de.mennomax.astikorcarts.util.RegObject;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.stats.IStatFormatter;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.registry.Registry;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.extensions.IForgeContainerType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Supplier;

@Mod(AstikorCarts.ID)
public final class AstikorCarts {
    public static final String ID = "astikorcarts";

    public static final SimpleChannel CHANNEL = new NetBuilder(new ResourceLocation(ID, "main"))
        .version(1).optionalServer().requiredClient()
        .serverbound(ActionKeyMessage::new).consumer(() -> ActionKeyMessage::handle)
        .serverbound(ToggleSlowMessage::new).consumer(() -> ToggleSlowMessage::handle)
        .clientbound(UpdateDrawnMessage::new).consumer(() -> new UpdateDrawnMessage.Handler())
        .clientbound(CartingJukeboxMessage::new).consumer(() -> new CartingJukeboxMessage.Handler())
        .serverbound(OpenCargoCartMessage::new).consumer(() -> OpenCargoCartMessage::handle)
        .build();

    private static final DefRegister REG = new DefRegister(ID);

    public static final class Items {
        private Items() {
        }

        private static final DefRegister.Forge<Item> R = REG.of(ForgeRegistries.ITEMS);

        public static final RegObject<Item, Item> WHEEL, CARGO_CART, PLOW_CART, MOB_CART;

        static {
            WHEEL = R.make("wheel", () -> new Item(new Item.Properties().group(ItemGroup.MATERIALS)));
            final Supplier<Item> cart = () -> new CartItem(new Item.Properties().maxStackSize(1).group(ItemGroup.TRANSPORTATION));
            CARGO_CART = R.make("cargo_cart", cart);
            PLOW_CART = R.make("plow_cart", cart);
            MOB_CART = R.make("mob_cart", cart);
        }
    }

    public static final class EntityTypes {
        private EntityTypes() {
        }

        private static final DefRegister.Forge<EntityType<?>> R = REG.of(ForgeRegistries.ENTITIES);

        public static final RegObject<EntityType<?>, EntityType<CargoCartEntity>> CARGO_CART;
        public static final RegObject<EntityType<?>, EntityType<PlowCartEntity>> PLOW_CART;
        public static final RegObject<EntityType<?>, EntityType<MobCartEntity>> MOB_CART;
        public static final RegObject<EntityType<?>, EntityType<PostilionEntity>> POSTILION;

        static {
            CARGO_CART = R.make("cargo_cart", () -> EntityType.Builder.create(CargoCartEntity::new, EntityClassification.MISC)
                .size(1.5F, 1.4F)
                .build(ID + ":cargo_cart"));
            PLOW_CART = R.make("plow_cart", () -> EntityType.Builder.create(PlowCartEntity::new, EntityClassification.MISC)
                .size(1.3F, 1.4F)
                .build(ID + ":plow_cart"));
            MOB_CART = R.make("mob_cart", () -> EntityType.Builder.create(MobCartEntity::new, EntityClassification.MISC)
                .size(1.3F, 1.4F)
                .build(ID + ":mob_cart"));
            POSTILION = R.make("postilion", () -> EntityType.Builder.create(PostilionEntity::new, EntityClassification.MISC)
                .size(0.25F, 0.25F)
                .disableSummoning()
                .disableSerialization()
                .build(ID + ":postilion"));
        }
    }

    public static final class SoundEvents {
        private SoundEvents() {
        }

        private static final DefRegister.Forge<SoundEvent> R = REG.of(ForgeRegistries.SOUND_EVENTS);

        public static final RegObject<SoundEvent, SoundEvent> CART_ATTACHED = R.make("entity.cart.attach", SoundEvent::new);
        public static final RegObject<SoundEvent, SoundEvent> CART_DETACHED = R.make("entity.cart.detach", SoundEvent::new);
        public static final RegObject<SoundEvent, SoundEvent> CART_PLACED = R.make("entity.cart.place", SoundEvent::new);
    }

    public static final class Stats {
        private Stats() {
        }

        private static final DefRegister.Vanilla<ResourceLocation, IStatFormatter> R = REG.of(Registry.CUSTOM_STAT, net.minecraft.stats.Stats.CUSTOM::get, rl -> IStatFormatter.DEFAULT);

        public static final ResourceLocation CART_ONE_CM = R.make("cart_one_cm", rl -> rl, rl -> IStatFormatter.DISTANCE);
    }

    public static final class ContainerTypes {
        private ContainerTypes() {
        }

        private static final DefRegister.Forge<ContainerType<?>> R = REG.of(ForgeRegistries.CONTAINERS);

        public static final RegObject<ContainerType<?>, ContainerType<PlowCartContainer>> PLOW_CART = R.make("plow_cart", () -> IForgeContainerType.create(PlowCartContainer::new));
    }

    public AstikorCarts() {
        final Initializer.Context ctx = new InitContext();
        DistExecutor.runForDist(() -> ClientInitializer::new, () -> ServerInitializer::new).init(ctx);
        REG.registerAll(ctx.modBus(), Items.R, EntityTypes.R, SoundEvents.R, ContainerTypes.R, Stats.R);
    }

    private static class InitContext implements Initializer.Context {
        @Override
        public ModLoadingContext context() {
            return ModLoadingContext.get();
        }

        @Override
        public IEventBus bus() {
            return MinecraftForge.EVENT_BUS;
        }

        @Override
        public IEventBus modBus() {
            return FMLJavaModLoadingContext.get().getModEventBus();
        }
    }
}
