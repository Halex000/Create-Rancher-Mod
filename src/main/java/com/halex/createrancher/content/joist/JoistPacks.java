package com.halex.createrancher.content.joist;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;

import com.google.common.base.Suppliers;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.halex.createrancher.CreateRancher;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.AddPackFindersEvent;

// Makes the joists' models, lang, loot, recipes and tags
public final class JoistPacks {

    private static final int RECIPE_COUNT = 12;

    private JoistPacks() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(JoistPacks::addPacks);
    }

    private static void addPacks(AddPackFindersEvent event) {
        PackType type = event.getPackType();
        if (type == PackType.CLIENT_RESOURCES)
            event.addRepositorySource(packs -> packs.accept(GeneratedPack.create(CreateRancher.MOD_ID + "_joists",
                    "Create Rancher joists", type, Suppliers.memoize(JoistPacks::assets))));
        else if (type == PackType.SERVER_DATA)
            event.addRepositorySource(packs -> packs.accept(GeneratedPack.create(CreateRancher.MOD_ID + "_joists",
                    "Create Rancher joists", type, Suppliers.memoize(JoistPacks::data))));
    }

    // The textures a joist uses
    public record Textures(String log, String logEnd, String wood, String planks) {
    }

    public static Textures textures(JoistWood wood) {
        ResourceLocation log = wood.strippedLog();
        ResourceLocation planks = wood.planks();
        String logSide = blockTexture(log, "side").orElse(log.getNamespace() + ":block/" + log.getPath());
        String logEnd = blockTexture(log, "end").orElse(log.getNamespace() + ":block/" + log.getPath() + "_top");
        String planksTexture = blockTexture(planks, "all").orElse(planks.getNamespace() + ":block/"
                + planks.getPath());
        String woodTexture = strippedWood(log).flatMap(stripped -> blockTexture(stripped, "side"))
                .orElse(logSide);
        return new Textures(logSide, logEnd, woodTexture, planksTexture);
    }

    private static Optional<ResourceLocation> strippedWood(ResourceLocation log) {
        String path = log.getPath();
        String wood = path.endsWith("_log") ? path.substring(0, path.length() - 4) + "_wood"
                : path.endsWith("_stem") ? path.substring(0, path.length() - 5) + "_hyphae" : null;
        if (wood == null)
            return Optional.empty();
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(log.getNamespace(), wood);
        return BuiltInRegistries.BLOCK.containsKey(id) ? Optional.of(id) : Optional.empty();
    }

    // A texture from a block's model
    private static Optional<String> blockTexture(ResourceLocation block, String key) {
        Optional<JsonObject> blockstate = ModResources.readFirst("assets/" + block.getNamespace() + "/blockstates/"
                + block.getPath() + ".json");
        Optional<String> model = blockstate.flatMap(JoistPacks::firstModel);
        return model.flatMap(name -> modelTexture(ResourceLocation.tryParse(name), key, 0));
    }

    private static Optional<String> firstModel(JsonObject blockstate) {
        if (blockstate.has("variants")) {
            JsonObject variants = blockstate.getAsJsonObject("variants");
            String chosen = variants.has("axis=y") ? "axis=y" : variants.keySet()
                    .stream()
                    .findFirst()
                    .orElse(null);
            if (chosen == null)
                return Optional.empty();
            JsonElement variant = variants.get(chosen);
            if (variant.isJsonArray())
                variant = variant.getAsJsonArray()
                        .get(0);
            return variant.isJsonObject() && variant.getAsJsonObject()
                    .has("model") ? Optional.of(variant.getAsJsonObject()
                    .get("model")
                    .getAsString()) : Optional.empty();
        }
        return Optional.empty();
    }

    // A texture from a model
    private static Optional<String> modelTexture(@Nullable ResourceLocation model, String key, int depth) {
        if (model == null || depth > 6)
            return Optional.empty();
        Optional<JsonObject> json = ModResources.readFirst("assets/" + model.getNamespace() + "/models/"
                + model.getPath() + ".json");
        if (json.isEmpty())
            return Optional.empty();
        JsonObject textures = json.get()
                .has("textures") ? json.get()
                .getAsJsonObject("textures") : new JsonObject();
        for (int i = 0; i < 8 && textures.has(key); i++) {
            String value = textures.get(key)
                    .getAsString();
            if (!value.startsWith("#"))
                return Optional.of(value.contains(":") ? value : "minecraft:" + value);
            key = value.substring(1);
        }
        if (!json.get()
                .has("parent"))
            return Optional.empty();
        return modelTexture(ResourceLocation.tryParse(json.get()
                .get("parent")
                .getAsString()), key, depth + 1);
    }

    private static Map<String, JsonElement> assets() {
        Map<String, JsonElement> files = new LinkedHashMap<>();
        JsonObject lang = new JsonObject();
        for (JoistWood wood : JoistWoods.all()) {
            String id = wood.id()
                    .getPath();
            String models = "assets/" + CreateRancher.MOD_ID + "/models/";
            Textures textures = textures(wood);
            files.put("assets/" + CreateRancher.MOD_ID + "/blockstates/" + id + ".json", blockstate(id));
            files.put(models + "block/" + id + "/pole.json", model(textures, pole()));
            files.put(models + "block/" + id + "/beam.json", model(textures, beam()));
            files.put(models + "block/" + id + "/top.json", model(textures, plate(true)));
            files.put(models + "block/" + id + "/bottom.json", model(textures, plate(false)));
            for (String side : new String[] {"north", "south", "east", "west"})
                files.put(models + "block/" + id + "/bracket_" + side + ".json", model(textures, bracket(side)));
            JsonObject item = model(textures, concat(beam(), plate(true), plate(false)));
            item.addProperty("parent", "minecraft:block/block");
            files.put(models + "item/" + id + ".json", item);
            lang.addProperty("block." + CreateRancher.MOD_ID + "." + id, wood.name() + " Joist");
        }
        files.put("assets/" + CreateRancher.MOD_ID + "/lang/en_us.json", lang);
        return files;
    }

    // The joist's blockstate
    private static JsonObject blockstate(String id) {
        String model = CreateRancher.MOD_ID + ":block/" + id + "/";
        JsonArray parts = new JsonArray();
        parts.add(part(model + "pole", 0, "x", "false", "z", "false"));
        parts.add(part(model + "beam", 0, "x", "true"));
        parts.add(part(model + "beam", 90, "z", "true"));
        for (String plate : new String[] {"top", "bottom"}) {
            parts.add(part(model + plate, 0, plate, "true", "x", "true", "z", "false"));
            parts.add(part(model + plate, 0, plate, "true", "x", "false", "z", "true"));
            parts.add(part(model + plate, 0, "x", "true", "z", "true"));
        }
        JsonObject blockstate = new JsonObject();
        blockstate.add("multipart", parts);
        return blockstate;
    }

    private static JsonObject part(String model, int y, String... when) {
        JsonObject condition = new JsonObject();
        for (int i = 0; i < when.length; i += 2)
            condition.addProperty(when[i], when[i + 1]);
        JsonObject apply = new JsonObject();
        apply.addProperty("model", model);
        if (y != 0)
            apply.addProperty("y", y);
        JsonObject part = new JsonObject();
        part.add("when", condition);
        part.add("apply", apply);
        return part;
    }

    private static JsonObject model(Textures textures, JsonArray elements) {
        JsonObject tex = new JsonObject();
        tex.addProperty("log", textures.log());
        tex.addProperty("log_end", textures.logEnd());
        tex.addProperty("wood", textures.wood());
        tex.addProperty("planks", textures.planks());
        tex.addProperty("particle", textures.planks());
        JsonObject model = new JsonObject();
        model.add("textures", tex);
        model.add("elements", elements);
        return model;
    }

    private static JsonArray concat(JsonArray... arrays) {
        JsonArray out = new JsonArray();
        for (JsonArray array : arrays)
            out.addAll(array);
        return out;
    }

    // The pole model
    private static JsonArray pole() {
        JsonArray out = new JsonArray();
        out.add(element(new double[] {4, 0, 4}, new double[] {12, 16, 12}, "#log", "#log", "#log_end"));
        return out;
    }

    // The beam model
    private static JsonArray beam() {
        JsonArray out = new JsonArray();
        out.add(element(new double[] {0, 4, 6}, new double[] {16, 12, 10}, "#planks", "#planks", null));
        for (double y : new double[] {2, 12}) {
            JsonObject flange = element(new double[] {0, y, 4}, new double[] {16, y + 2, 12}, "#log", "#log_end",
                    "#log");
            JsonObject faces = flange.getAsJsonObject("faces");
            faces.add("north", face("#log", new double[] {7, 0, 9, 16}, 90));
            faces.add("south", face("#log", new double[] {7, 0, 9, 16}, 90));
            faces.add("up", face("#log", new double[] {4, 0, 12, 16}, 90));
            faces.add("down", face("#log", new double[] {4, 0, 12, 16}, 90));
            out.add(flange);
        }
        return out;
    }

    // The plate model
    private static JsonArray plate(boolean top) {
        JsonArray out = new JsonArray();
        out.add(top ? element(new double[] {3, 11, 3}, new double[] {13, 16, 13}, "#wood", "#wood", "#wood")
                : element(new double[] {3, 0, 3}, new double[] {13, 5, 13}, "#wood", "#wood", "#wood"));
        return out;
    }

    // The bracket model
    private static JsonArray bracket(String side) {
        double[][] box = switch (side) {
            case "north" -> new double[][] {{2.95, 0.95, -2}, {13.05, 15.05, 1}};
            case "south" -> new double[][] {{2.95, 0.95, 15}, {13.05, 15.05, 18}};
            case "east" -> new double[][] {{15, 0.95, 2.95}, {18, 15.05, 13.05}};
            default -> new double[][] {{-2, 0.95, 2.95}, {1, 15.05, 13.05}};
        };
        JsonArray out = new JsonArray();
        out.add(element(box[0], box[1], "#planks", "#planks", "#planks"));
        return out;
    }

    // A box element for a model
    private static JsonObject element(double[] from, double[] to, @Nullable String northSouth,
                                      @Nullable String eastWest, @Nullable String upDown) {
        JsonObject faces = new JsonObject();
        double x1 = from[0], y1 = from[1], z1 = from[2], x2 = to[0], y2 = to[1], z2 = to[2];
        if (northSouth != null) {
            faces.add("north", face(northSouth, uv(16 - x2, 16 - y2, 16 - x1, 16 - y1), 0));
            faces.add("south", face(northSouth, uv(x1, 16 - y2, x2, 16 - y1), 0));
        }
        if (eastWest != null) {
            faces.add("east", face(eastWest, uv(16 - z2, 16 - y2, 16 - z1, 16 - y1), 0));
            faces.add("west", face(eastWest, uv(z1, 16 - y2, z2, 16 - y1), 0));
        }
        if (upDown != null) {
            faces.add("up", face(upDown, uv(x1, z1, x2, z2), 0));
            faces.add("down", face(upDown, uv(x1, 16 - z2, x2, 16 - z1), 0));
        }
        JsonObject element = new JsonObject();
        element.add("from", array(from));
        element.add("to", array(to));
        element.add("faces", faces);
        return element;
    }

    // Keeps a UV on the texture
    private static double[] uv(double u1, double v1, double u2, double v2) {
        double[] u = onTexture(u1, u2), v = onTexture(v1, v2);
        return new double[] {u[0], v[0], u[1], v[1]};
    }

    private static double[] onTexture(double a, double b) {
        if (a >= 0 && b <= 16)
            return new double[] {a, b};
        double width = b - a;
        return a < 0 ? new double[] {0, width} : new double[] {16 - width, 16};
    }

    private static JsonObject face(String texture, double[] uv, int rotation) {
        JsonObject face = new JsonObject();
        face.add("uv", array(uv));
        if (rotation != 0)
            face.addProperty("rotation", rotation);
        face.addProperty("texture", texture);
        return face;
    }

    private static JsonArray array(double[] values) {
        JsonArray out = new JsonArray();
        for (double value : values)
            out.add(value == Math.rint(value) ? new JsonPrimitive((long) value) : new JsonPrimitive(value));
        return out;
    }

    private static Map<String, JsonElement> data() {
        Map<String, JsonElement> files = new LinkedHashMap<>();
        JsonArray all = new JsonArray();
        String ns = CreateRancher.MOD_ID;
        for (JoistWood wood : JoistWoods.all()) {
            String id = wood.id()
                    .getPath();
            all.add(wood.id()
                    .toString());
            files.put("data/" + ns + "/loot_table/blocks/" + id + ".json", lootTable(wood.id()));
            Item log = BuiltInRegistries.ITEM.get(wood.strippedLog());
            Item planks = BuiltInRegistries.ITEM.get(wood.planks());
            if (log != Items.AIR && planks != Items.AIR)
                files.put("data/" + ns + "/recipe/" + id + ".json", recipe(wood.id(), BuiltInRegistries.ITEM.getKey(log),
                        BuiltInRegistries.ITEM.getKey(planks)));
        }
        files.put("data/minecraft/tags/block/mineable/axe.json", tag(all));
        files.put("data/" + ns + "/tags/block/joists.json", tag(all));
        files.put("data/" + ns + "/tags/item/joists.json", tag(all));
        return files;
    }

    private static JsonObject tag(JsonArray values) {
        JsonObject tag = new JsonObject();
        tag.addProperty("replace", false);
        tag.add("values", values);
        return tag;
    }

    private static JsonObject lootTable(ResourceLocation joist) {
        JsonObject entry = new JsonObject();
        entry.addProperty("type", "minecraft:item");
        entry.addProperty("name", joist.toString());
        JsonArray entries = new JsonArray();
        entries.add(entry);
        JsonObject survives = new JsonObject();
        survives.addProperty("condition", "minecraft:survives_explosion");
        JsonArray conditions = new JsonArray();
        conditions.add(survives);
        JsonObject pool = new JsonObject();
        pool.addProperty("rolls", 1);
        pool.addProperty("bonus_rolls", 0);
        pool.add("entries", entries);
        pool.add("conditions", conditions);
        JsonArray pools = new JsonArray();
        pools.add(pool);
        JsonObject table = new JsonObject();
        table.addProperty("type", "minecraft:block");
        table.add("pools", pools);
        table.addProperty("random_sequence", joist.getNamespace() + ":blocks/" + joist.getPath());
        return table;
    }

    private static JsonObject recipe(ResourceLocation joist, ResourceLocation log, ResourceLocation planks) {
        JsonObject recipe = new JsonObject();
        recipe.addProperty("type", "minecraft:crafting_shaped");
        recipe.addProperty("category", "building");
        recipe.addProperty("group", CreateRancher.MOD_ID + ":joists");
        JsonObject key = new JsonObject();
        JsonObject l = new JsonObject();
        l.addProperty("item", log.toString());
        JsonObject p = new JsonObject();
        p.addProperty("item", planks.toString());
        key.add("L", l);
        key.add("P", p);
        recipe.add("key", key);
        JsonArray pattern = new JsonArray();
        pattern.add("LLL");
        pattern.add("PPP");
        recipe.add("pattern", pattern);
        JsonObject result = new JsonObject();
        result.addProperty("count", RECIPE_COUNT);
        result.addProperty("id", joist.toString());
        recipe.add("result", result);
        return recipe;
    }
}
