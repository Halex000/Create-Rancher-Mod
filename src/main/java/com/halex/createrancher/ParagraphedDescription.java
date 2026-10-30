package com.halex.createrancher;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.TooltipHelper;

import net.createmod.catnip.lang.FontHelper.Palette;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

// Create's shift summary tooltip with more than one paragraph
public class ParagraphedDescription extends ItemDescription.Modifier {

    public ParagraphedDescription(Item item, Palette palette) {
        super(item, palette);
    }

    @Override
    public void modify(ItemTooltipEvent context) {
        if (checkLocale())
            description = create(item, palette);
        if (description == null)
            return;
        context.getToolTip()
                .addAll(1, description.getCurrentLines());
    }

    @Nullable
    private static ItemDescription create(Item item, Palette palette) {
        String key = ItemDescription.getTooltipTranslationKey(item);
        ItemDescription description = ItemDescription.create(key, palette);
        if (description == null || !I18n.exists(key + ".summary2"))
            return description;
        boolean controls = I18n.exists(key + ".control1");
        int at = (controls ? 3 : 2) + TooltipHelper.cutStringTextComponent(I18n.get(key + ".summary"), palette)
                .size();
        List<Component> paragraphs = new ArrayList<>();
        for (int i = 2; I18n.exists(key + ".summary" + i); i++) {
            paragraphs.add(CommonComponents.EMPTY);
            paragraphs.addAll(TooltipHelper.cutStringTextComponent(I18n.get(key + ".summary" + i), palette));
        }
        List<Component> shift = new ArrayList<>(description.linesOnShift());
        shift.addAll(Math.min(at, shift.size()), paragraphs);
        return new ItemDescription(description.lines(), ImmutableList.copyOf(shift), description.linesOnCtrl());
    }
}
