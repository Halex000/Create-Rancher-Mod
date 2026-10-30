package com.halex.createrancher.content.herder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import javax.annotation.Nullable;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

import com.halex.createrancher.compat.sable.SubLevels;
import com.halex.createrancher.content.herder.MechanicalHerderBlockEntity.AgeFilter;
import com.halex.createrancher.content.herder.MechanicalHerderBlockEntity.DropOffStatus;
import com.halex.createrancher.index.BlockRegistry;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.gui.widget.SelectionScrollInput;

import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.gui.AbstractSimiScreen;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.createmod.catnip.gui.element.ScreenElement;
import net.createmod.catnip.gui.widget.AbstractSimiWidget;
import net.createmod.catnip.lang.FontHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

public class MechanicalHerderScreen extends AbstractSimiScreen {

    private static final int MOB_ROW = 21;
    private static final int AGE_ROW = 45;
    private static final int AREA_ROW = 69;
    private static final int ROW_HEIGHT = 24;
    private static final int DROP_OFF_LABEL_WIDTH = 84;
    private static final int TITLE_COLOR = 0x592424;
    private static final int INVALID_COLOR = 0xFF6B6B;

    private static final int AREA_PREVIEW_TICKS = 60;

    private static final int SUGGESTION_ROWS = 6;
    private static final int SUGGESTION_HEIGHT = 12;
    private static final int SUGGESTION_WIDTH = 228;

    private final AllGuiTextures background = AllGuiTextures.THRESHOLD_SWITCH;
    private final int backgroundHeight = background.getHeight() + ROW_HEIGHT;
    private final ItemStack renderedItem = BlockRegistry.MECHANICAL_HERDER.asStack();
    private final MechanicalHerderBlockEntity be;
    private final List<EntityType<?>> creatures;

    private EditBox mobField;
    private SelectionScrollInput ageInput;
    private SelectionScrollInput areaInput;

    @Nullable
    private EntityType<?> selectedMob;
    private boolean mobValid = true;

    private List<EntityType<?>> suggestions = List.of();
    private boolean showSuggestions;
    private int suggestionIndex;
    private int suggestionScroll;

    @Nullable
    private LivingEntity previewEntity;
    @Nullable
    private EntityType<?> previewType;
    private int previewAge = -1;

    public MechanicalHerderScreen(MechanicalHerderBlockEntity be) {
        super(Component.translatable("createrancher.gui.mechanical_herder.title"));
        this.be = be;
        Level level = Minecraft.getInstance().level;
        creatures = level == null ? List.of() : HerderCreatures.all(level);
    }

    @Override
    protected void init() {
        setWindowSize(background.getWidth(), backgroundHeight);
        setWindowOffset(-20, 0);
        super.init();

        int x = guiLeft;
        int y = guiTop;

        ResourceLocation filter = be.getMobFilter();
        String mobText = mobField != null ? mobField.getValue() : filter == null ? "" : filter.toString();
        int ageState = ageInput != null ? ageInput.getState() : be.getAgeFilter()
                .ordinal();
        int areaSize = areaInput != null ? getSelectedAreaSize() : be.getAreaSize();

        mobField = new EditBox(font, x + 50, y + MOB_ROW + 6, 100, 10,
                Component.translatable("createrancher.gui.mechanical_herder.mob_filter"));
        mobField.setBordered(false);
        mobField.setMaxLength(128);
        mobField.setTextColor(0xFFFFFF);
        mobField.setHint(Component.translatable("createrancher.gui.mechanical_herder.any_mob")
                .withStyle(ChatFormatting.GRAY));
        mobField.setValue(mobText);
        mobField.moveCursorToStart(false);
        resolveMob(mobField.getValue());
        mobField.setResponder(this::onMobTextChanged);

        ageInput = (SelectionScrollInput) new QuietScrollInput(x + 48, y + AGE_ROW + 2, 103, 18)
                .forOptions(List.of(AgeFilter.values())
                        .stream()
                        .map(age -> (Component) Component.translatable(age.getTranslationKey()))
                        .toList())
                .titled(Component.translatable("createrancher.gui.mechanical_herder.age_filter"))
                .setState(ageState);

        List<Component> areaNames = new ArrayList<>();
        int selectedArea = 0;
        int allowedAreas = 0;
        for (int i = 0; i < MechanicalHerderBlockEntity.AREA_SIZES.length; i++) {
            int size = MechanicalHerderBlockEntity.AREA_SIZES[i];
            areaNames.add(areaName(size));
            if (size == areaSize)
                selectedArea = i;
            if (MechanicalHerderBlockEntity.checkDropOff(be.getLevel(), be.getBlockPos(), be.getFacing(), size, be.getDropOff())
                    != DropOffStatus.IN_AREA)
                allowedAreas = i + 1;
        }
        allowedAreas = Math.max(allowedAreas, selectedArea + 1);
        areaInput = (SelectionScrollInput) new QuietScrollInput(x + 48, y + AREA_ROW + 2, 103, 18)
                .forOptions(areaNames)
                .withRange(0, allowedAreas)
                .titled(Component.translatable("createrancher.gui.mechanical_herder.area_size"))
                .calling(state -> MechanicalHerderClient.showAreaPreview(be,
                        MechanicalHerderBlockEntity.AREA_SIZES[state], AREA_PREVIEW_TICKS));
        if (allowedAreas < areaNames.size())
            areaInput.addHint(Component.translatable("createrancher.gui.mechanical_herder.area_limited"));
        areaInput.setState(selectedArea);

        IconButton confirmButton =
                new QuietIconButton(x + background.getWidth() - 33, y + backgroundHeight - 24, AllIcons.I_CONFIRM);
        confirmButton.withCallback(this::onClose);

        IconButton previewButton =
                new QuietIconButton(x + background.getWidth() - 62, y + backgroundHeight - 24, AllIcons.I_TARGET);
        previewButton.setToolTip(Component.translatable("createrancher.gui.mechanical_herder.show_area"));
        previewButton.withCallback(() -> {
            int previewSize = getSelectedAreaSize();
            onClose();
            MechanicalHerderClient.showAreaPreview(be, previewSize, 100);
        });

        addRenderableWidget(mobField);
        addRenderableWidgets(ageInput, areaInput);
        addRenderableWidgets(confirmButton, previewButton);
    }

    private void onMobTextChanged(String text) {
        resolveMob(text);
        suggestions = findSuggestions(text);
        suggestionIndex = 0;
        suggestionScroll = 0;
        boolean exactMatch = mobValid && selectedMob != null && suggestions.size() == 1
                && suggestions.get(0) == selectedMob;
        showSuggestions = !suggestions.isEmpty() && !exactMatch;
        updateGhostText(text);
    }

    // Takes a mob id or name
    private void resolveMob(String text) {
        String query = text.trim();
        mobValid = true;
        selectedMob = null;
        if (query.isEmpty()) {
            mobField.setTextColor(0xFFFFFF);
            return;
        }

        ResourceLocation id = ResourceLocation.tryParse(query.toLowerCase(Locale.ROOT));
        Optional<EntityType<?>> match = creatures.stream()
                .filter(type -> EntityType.getKey(type)
                        .equals(id))
                .findFirst();
        if (match.isEmpty()) {
            List<EntityType<?>> byName = creatures.stream()
                    .filter(type -> type.getDescription()
                            .getString()
                            .equalsIgnoreCase(query))
                    .toList();
            if (byName.size() == 1)
                match = Optional.of(byName.get(0));
        }

        selectedMob = match.orElse(null);
        mobValid = match.isPresent();
        mobField.setTextColor(mobValid ? 0xFFFFFF : INVALID_COLOR);
    }

    // Sorts search matches
    private List<EntityType<?>> findSuggestions(String text) {
        String query = text.trim()
                .toLowerCase(Locale.ROOT);
        if (query.isEmpty())
            return List.of();
        record Match(EntityType<?> type, int rank, String name) {
        }
        List<Match> matches = new ArrayList<>();
        for (EntityType<?> type : creatures) {
            String name = type.getDescription()
                    .getString()
                    .toLowerCase(Locale.ROOT);
            ResourceLocation id = EntityType.getKey(type);
            String fullId = id.toString();
            if (name.startsWith(query) || id.getPath()
                    .startsWith(query) || fullId.startsWith(query))
                matches.add(new Match(type, 0, name));
            else if (name.contains(query) || fullId.contains(query))
                matches.add(new Match(type, 1, name));
        }
        return matches.stream()
                .sorted(Comparator.comparingInt(Match::rank)
                        .thenComparing(Match::name))
                .limit(50)
                .<EntityType<?>>map(Match::type)
                .toList();
    }

    private void updateGhostText(String text) {
        String completion = null;
        if (showSuggestions && !text.isEmpty()) {
            String id = EntityType.getKey(suggestions.get(suggestionIndex))
                    .toString();
            String name = suggestions.get(suggestionIndex)
                    .getDescription()
                    .getString();
            if (id.startsWith(text))
                completion = id.substring(text.length());
            else if (name.toLowerCase(Locale.ROOT)
                    .startsWith(text.toLowerCase(Locale.ROOT)))
                completion = name.substring(text.length());
        }
        mobField.setSuggestion(completion);
    }

    private void acceptSuggestion(EntityType<?> type) {
        mobField.setValue(EntityType.getKey(type)
                .toString());
        showSuggestions = false;
        mobField.setSuggestion(null);
    }

    private void moveSuggestionSelection(int delta) {
        suggestionIndex = Mth.clamp(suggestionIndex + delta, 0, suggestions.size() - 1);
        if (suggestionIndex < suggestionScroll)
            suggestionScroll = suggestionIndex;
        if (suggestionIndex >= suggestionScroll + SUGGESTION_ROWS)
            suggestionScroll = suggestionIndex - SUGGESTION_ROWS + 1;
        updateGhostText(mobField.getValue());
    }

    private boolean suggestionsVisible() {
        return showSuggestions && mobField.isFocused() && !suggestions.isEmpty();
    }

    // Lines up with the mob field
    private int suggestionsX() {
        return Math.max(1, Math.min(guiLeft + 48, width - SUGGESTION_WIDTH - 2));
    }

    private int suggestionsY() {
        return guiTop + MOB_ROW + 20;
    }

    private int visibleSuggestionCount() {
        return Math.min(SUGGESTION_ROWS, suggestions.size());
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (mobField.isFocused()) {
            if (suggestionsVisible()) {
                switch (keyCode) {
                    case GLFW.GLFW_KEY_DOWN -> {
                        moveSuggestionSelection(1);
                        return true;
                    }
                    case GLFW.GLFW_KEY_UP -> {
                        moveSuggestionSelection(-1);
                        return true;
                    }
                    case GLFW.GLFW_KEY_TAB, GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                        acceptSuggestion(suggestions.get(suggestionIndex));
                        return true;
                    }
                    case GLFW.GLFW_KEY_ESCAPE -> {
                        showSuggestions = false;
                        mobField.setSuggestion(null);
                        return true;
                    }
                    default -> {
                    }
                }
            } else if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                mobField.setFocused(false);
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (suggestionsVisible()) {
            int row = suggestionRowAt(mouseX, mouseY);
            if (row != -1) {
                acceptSuggestion(suggestions.get(row));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (suggestionsVisible() && suggestionRowAt(mouseX, mouseY) != -1) {
            int maxScroll = Math.max(0, suggestions.size() - SUGGESTION_ROWS);
            suggestionScroll = Mth.clamp(suggestionScroll - (int) Math.signum(scrollY), 0, maxScroll);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private int suggestionRowAt(double mouseX, double mouseY) {
        int x = suggestionsX();
        int y = suggestionsY();
        if (mouseX < x || mouseX >= x + SUGGESTION_WIDTH || mouseY < y)
            return -1;
        int row = (int) (mouseY - y) / SUGGESTION_HEIGHT;
        return row < visibleSuggestionCount() ? suggestionScroll + row : -1;
    }

    @Override
    protected void renderWindow(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        int x = guiLeft;
        int y = guiTop;

        renderBackgroundTexture(graphics, x, y);
        graphics.drawString(font, title, x + background.getWidth() / 2 - font.width(title) / 2, y + 4, TITLE_COLOR,
                false);

        AgeFilter selectedAge = AgeFilter.values()[ageInput.getState()];
        int areaSize = getSelectedAreaSize();
        BlockPos dropOff = be.getDropOff();
        DropOffStatus status = MechanicalHerderBlockEntity.checkDropOff(be.getLevel(), be.getBlockPos(), be.getFacing(), areaSize,
                dropOff);

        for (int row : new int[] {MOB_ROW, AGE_ROW, AREA_ROW})
            AllGuiTextures.THRESHOLD_SWITCH_MISC_INPUTS.render(graphics, x + 44, y + row);
        drawInputText(graphics, Component.translatable(selectedAge.getTranslationKey()), x + 53, y + AGE_ROW + 7,
                0xFFFFFF);
        drawInputText(graphics, areaName(areaSize), x + 53, y + AREA_ROW + 7,
                status == DropOffStatus.IN_AREA ? INVALID_COLOR : 0xFFFFFF);
        graphics.renderItem(mobIcon(), x + 24, y + MOB_ROW + 3);
        graphics.renderItem(ageIcon(selectedAge), x + 24, y + AGE_ROW + 3);
        graphics.renderItem(new ItemStack(Items.OAK_FENCE), x + 24, y + AREA_ROW + 3);

        int slotX = x + 13;
        int slotY = y + backgroundHeight - 23;
        graphics.renderItem(dropOffIcon(dropOff), slotX, slotY);
        int labelColor = status == DropOffStatus.TOO_FAR || status == DropOffStatus.IN_AREA
                || status == DropOffStatus.INVALID_TARGET ? 0xB23434 : AllGuiTextures.FONT_COLOR;
        List<FormattedCharSequence> label = font.split(dropOffLabel(dropOff, status), DROP_OFF_LABEL_WIDTH);
        int labelY = slotY + 4 - (label.size() - 1) * 5;
        for (FormattedCharSequence line : label) {
            graphics.drawString(font, line, slotX + 21, labelY, labelColor, false);
            labelY += 10;
        }

        renderPreview(graphics, x + background.getWidth(), y + backgroundHeight, selectedAge);

        if (suggestionsVisible())
            return;
        if (mouseX >= slotX && mouseX < slotX + 16 && mouseY >= slotY && mouseY < slotY + 16)
            graphics.renderComponentTooltip(font, dropOffTooltip(dropOff, status), mouseX, mouseY);
        else if (mouseX >= x + 44 && mouseX < x + 159 && mouseY >= y + MOB_ROW && mouseY < y + MOB_ROW + 22)
            graphics.renderComponentTooltip(font, mobFieldTooltip(), mouseX, mouseY);
    }

    @Override
    protected void renderWindowForeground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (suggestionsVisible()) {
            renderSuggestions(graphics, mouseX, mouseY);
            return;
        }
        super.renderWindowForeground(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    protected void renderWindowBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
    }

    private void renderBackgroundTexture(GuiGraphics graphics, int x, int y) {
        ResourceLocation texture = background.getLocation();
        int u = background.getStartX();
        int v = background.getStartY();
        int width = background.getWidth();
        int lastRowEnd = AGE_ROW + ROW_HEIGHT;
        graphics.blit(texture, x, y, u, v, width, lastRowEnd);
        graphics.blit(texture, x, y + AREA_ROW, u, v + AGE_ROW, width, ROW_HEIGHT);
        graphics.blit(texture, x, y + AREA_ROW + ROW_HEIGHT, u, v + lastRowEnd, width,
                background.getHeight() - lastRowEnd);
    }

    private void renderSuggestions(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = suggestionsX();
        int y = suggestionsY();
        int rows = visibleSuggestionCount();
        int hovered = suggestionRowAt(mouseX, mouseY);

        graphics.pose()
                .pushPose();
        graphics.pose()
                .translate(0, 0, 400);
        graphics.fill(x - 1, y - 1, x + SUGGESTION_WIDTH + 1, y + rows * SUGGESTION_HEIGHT + 1, 0xFF7A5230);
        graphics.fill(x, y, x + SUGGESTION_WIDTH, y + rows * SUGGESTION_HEIGHT, 0xF0201510);

        for (int row = 0; row < rows; row++) {
            int index = suggestionScroll + row;
            EntityType<?> type = suggestions.get(index);
            int rowY = y + row * SUGGESTION_HEIGHT;
            boolean selected = index == suggestionIndex || index == hovered;
            if (selected)
                graphics.fill(x, rowY, x + SUGGESTION_WIDTH, rowY + SUGGESTION_HEIGHT, 0xFF4A3524);

            ResourceLocation id = EntityType.getKey(type);
            String namespace = id.getNamespace()
                    .equals(ResourceLocation.DEFAULT_NAMESPACE) ? "" : id.getNamespace();
            int namespaceWidth = namespace.isEmpty() ? 0 : font.width(namespace) + 4;
            String name = font.plainSubstrByWidth(type.getDescription()
                    .getString(), SUGGESTION_WIDTH - 6 - namespaceWidth);
            graphics.drawString(font, name, x + 3, rowY + 2, selected ? 0xFFE9B0 : 0xFFFFFF, false);
            if (!namespace.isEmpty())
                graphics.drawString(font, namespace, x + SUGGESTION_WIDTH - 3 - font.width(namespace), rowY + 2,
                        0x9A8A7A, false);
        }

        if (suggestions.size() > rows) {
            String more = (suggestionScroll + rows) + "/" + suggestions.size();
            graphics.drawString(font, more, x + SUGGESTION_WIDTH - font.width(more), y + rows * SUGGESTION_HEIGHT + 3,
                    0xB0A090, true);
        }
        graphics.pose()
                .popPose();
    }

    private void drawInputText(GuiGraphics graphics, Component text, int x, int y, int color) {
        String trimmed = font.plainSubstrByWidth(text.getString(), 96);
        graphics.drawString(font, trimmed, x, y, color, true);
    }

    // Shows the filtered mob next to the window
    private void renderPreview(GuiGraphics graphics, int windowRight, int windowBottom, AgeFilter age) {
        LivingEntity entity = getPreviewEntity(age);
        if (entity == null) {
            GuiGameElement.of(renderedItem).<GuiGameElement.GuiRenderBuilder>at(windowRight + 6, windowBottom - 56, -200)
                    .scale(5)
                    .render(graphics);
            return;
        }

        float size = Math.max(entity.getBbHeight(), entity.getBbWidth());
        float scale = Mth.clamp(56 / size, 4, 30);
        float spin = (AnimationTickHolder.getRenderTime() * 2) % 360;
        entity.setYRot(180 + spin);
        entity.yBodyRot = 180 + spin;
        entity.yBodyRotO = entity.yBodyRot;
        entity.setYHeadRot(entity.getYRot());
        entity.yHeadRotO = entity.getYRot();
        entity.setXRot(0);

        Quaternionf pose = new Quaternionf().rotateZ(Mth.PI);
        Quaternionf camera = new Quaternionf().rotateX(15 * Mth.DEG_TO_RAD);
        pose.mul(camera);
        try {
            InventoryScreen.renderEntityInInventory(graphics, windowRight + 46, windowBottom - 44, scale,
                    new Vector3f(0, entity.getBbHeight() / 2, 0), pose, camera, entity);
        } catch (RuntimeException e) {
            previewEntity = null;
        }
    }

    @Nullable
    private LivingEntity getPreviewEntity(AgeFilter age) {
        EntityType<?> type = mobValid ? selectedMob : null;
        if (type == previewType && age.ordinal() == previewAge)
            return previewEntity;

        previewType = type;
        previewAge = age.ordinal();
        previewEntity = null;
        if (type == null || minecraft == null || minecraft.level == null)
            return null;
        try {
            Entity entity = type.create(minecraft.level);
            if (entity instanceof LivingEntity living) {
                if (living instanceof Mob mob)
                    mob.setBaby(age == AgeFilter.BABIES);
                previewEntity = living;
            }
        } catch (RuntimeException ignored) {
        }
        return previewEntity;
    }

    private List<Component> mobFieldTooltip() {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.translatable("createrancher.gui.mechanical_herder.mob_filter")
                .withColor(AbstractSimiWidget.HEADER_RGB.getRGB()));
        if (!mobValid)
            tooltip.add(Component.translatable("createrancher.gui.mechanical_herder.unknown_mob")
                    .withColor(INVALID_COLOR));
        tooltip.addAll(FontHelper.cutTextComponent(
                Component.translatable("createrancher.gui.mechanical_herder.mob_filter_hint"),
                FontHelper.Palette.GRAY_AND_WHITE));
        return tooltip;
    }

    private List<Component> dropOffTooltip(@Nullable BlockPos dropOff, DropOffStatus status) {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.translatable("createrancher.gui.mechanical_herder.drop_off")
                .withColor(AbstractSimiWidget.HEADER_RGB.getRGB()));
        if (dropOff == null) {
            tooltip.add(Component.translatable("createrancher.gui.mechanical_herder.no_drop_off")
                    .withStyle(ChatFormatting.GRAY));
            tooltip.addAll(FontHelper.cutTextComponent(Component.translatable(DropOffStatus.NONE.getHintKey(),
                    MechanicalHerderBlockEntity.RANGE), FontHelper.Palette.GRAY_AND_WHITE));
            return tooltip;
        }
        tooltip.add(minecraft.level.getBlockState(dropOff)
                .getBlock()
                .getName()
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(dropOffPosition(dropOff, false)
                .copy()
                .withStyle(ChatFormatting.DARK_GRAY));
        if (status != DropOffStatus.VALID)
            tooltip.addAll(FontHelper.cutTextComponent(Component.translatable(status.getHintKey(),
                    MechanicalHerderBlockEntity.RANGE), FontHelper.Palette.GRAY_AND_RED));
        return tooltip;
    }

    // The drop-off's coordinates, or its offset from the herder on a sub-level
    private Component dropOffPosition(BlockPos dropOff, boolean brief) {
        BlockPos herder = be.getBlockPos();
        if (!SubLevels.isOnSubLevel(be.getLevel(), herder))
            return Component.literal(dropOff.getX() + ", " + dropOff.getY() + ", " + dropOff.getZ());
        BlockPos offset = dropOff.subtract(herder);
        return Component.translatable("createrancher.gui.mechanical_herder.drop_off_offset" + (brief ? "_short" : ""),
                offset.getX(), offset.getY(), offset.getZ());
    }

    private Component dropOffLabel(@Nullable BlockPos dropOff, DropOffStatus status) {
        return switch (status) {
            case NONE -> Component.translatable("createrancher.gui.mechanical_herder.no_drop_off");
            case TOO_FAR -> Component.translatable("createrancher.gui.mechanical_herder.drop_off_too_far");
            case IN_AREA -> Component.translatable("createrancher.gui.mechanical_herder.drop_off_in_area");
            case INVALID_TARGET -> Component.translatable("createrancher.gui.mechanical_herder.drop_off_invalid");
            case VALID -> dropOffPosition(dropOff, true);
        };
    }

    private static Component areaName(int size) {
        return Component.literal(size + "x" + MechanicalHerderBlockEntity.AREA_HEIGHT + "x" + size);
    }

    private int getSelectedAreaSize() {
        return MechanicalHerderBlockEntity.AREA_SIZES[areaInput.getState()];
    }

    private ItemStack mobIcon() {
        if (!mobValid)
            return new ItemStack(Items.BARRIER);
        if (selectedMob == null)
            return new ItemStack(Items.NAME_TAG);
        SpawnEggItem egg = SpawnEggItem.byId(selectedMob);
        return new ItemStack(egg == null ? Items.NAME_TAG : egg);
    }

    private static ItemStack ageIcon(AgeFilter age) {
        return new ItemStack(switch (age) {
            case ANY -> Items.CLOCK;
            case BABIES -> Items.EGG;
            case ADULTS -> Items.WHEAT;
        });
    }

    private ItemStack dropOffIcon(@Nullable BlockPos dropOff) {
        if (dropOff == null || minecraft == null || minecraft.level == null)
            return new ItemStack(Items.BARRIER);
        ItemStack stack = new ItemStack(minecraft.level.getBlockState(dropOff)
                .getBlock());
        return stack.isEmpty() ? new ItemStack(Items.GRASS_BLOCK) : stack;
    }

    @Override
    public void removed() {
        Optional<ResourceLocation> mobFilter = mobValid ? Optional.ofNullable(selectedMob)
                .map(EntityType::getKey) : Optional.ofNullable(be.getMobFilter());
        PacketDistributor.sendToServer(new ConfigureMechanicalHerderPacket(be.getBlockPos(), mobFilter,
                ageInput.getState(), getSelectedAreaSize()));
    }

    // A scroll input that keeps its tooltip hidden while the mob suggestions are open
    private class QuietScrollInput extends SelectionScrollInput {

        QuietScrollInput(int x, int y, int width, int height) {
            super(x, y, width, height);
        }

        @Override
        protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
            if (!suggestionsVisible())
                super.renderTooltip(graphics, mouseX, mouseY, partialTicks);
        }
    }

    // A button that keeps its tooltip hidden while the mob suggestions are open
    private class QuietIconButton extends IconButton {

        QuietIconButton(int x, int y, ScreenElement icon) {
            super(x, y, icon);
        }

        @Override
        protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
            if (!suggestionsVisible())
                super.renderTooltip(graphics, mouseX, mouseY, partialTicks);
        }
    }
}
