package com.halex.createrancher.index;

import com.halex.createrancher.CreateRancher;
import net.createmod.catnip.lang.LangBuilder;

public class LangRegistry {
    public static LangBuilder builder() {
        return new LangBuilder(CreateRancher.MOD_ID);
    }
}