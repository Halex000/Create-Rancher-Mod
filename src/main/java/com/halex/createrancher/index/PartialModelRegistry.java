package com.halex.createrancher.index;

import java.util.Arrays;

import com.halex.createrancher.CreateRancher;
import com.halex.createrancher.content.grate.GrateMetal;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;

public class PartialModelRegistry {

    public static final PartialModel[] HERDER_WIDE_SEGMENT = herderSegment("wide"),
            HERDER_MEDIUM_SEGMENT = herderSegment("medium"),
            HERDER_THIN_SEGMENT = herderSegment("thin");

    public static final PartialModel
            HERDER_LARGE_JOINT = block("mechanical_herder/large_joint"),
            HERDER_SMALL_JOINT = block("mechanical_herder/small_joint"),
            HERDER_PLUNGER_HANDLE = block("mechanical_herder/plunger_handle"),
            HERDER_PLUNGER_CUP = block("mechanical_herder/plunger_cup"),

            FEEDER_COG = block("mechanical_feeder/cog"),
            FEEDER_BASE = block("mechanical_feeder/base"),
            FEEDER_LOWER_BODY = block("mechanical_feeder/lower_body"),
            FEEDER_UPPER_BODY = block("mechanical_feeder/upper_body"),
            FEEDER_CLAW_BASE = block("mechanical_feeder/claw_base"),
            FEEDER_CLAW_BASE_GOGGLES = block("mechanical_feeder/claw_base_goggles"),
            FEEDER_CLAW_GRIP_UPPER = block("mechanical_feeder/upper_claw_grip"),
            FEEDER_CLAW_GRIP_LOWER = block("mechanical_feeder/lower_claw_grip"),

            SHEARER_HEAD = block("mechanical_shearer/head"),
            SHEARER_BLADE_LEFT = block("mechanical_shearer/blade_left"),
            SHEARER_BLADE_RIGHT = block("mechanical_shearer/blade_right"),

            GRATED_BELT_FLAT = block("belt_grate/belt_flat"),
            GRATED_BELT_SLOPE_NORTH = block("belt_grate/belt_slope_north"),
            GRATED_BELT_SLOPE_SOUTH = block("belt_grate/belt_slope_south"),
            GRATED_BELT_SLOPE_BOTH = block("belt_grate/belt_slope_both"),

            FISHER_PLATE = block("mechanical_fisher/plate"),
            FISHER_KNOT = block("mechanical_fisher/knot"),
            FISHER_NET_PILE = block("mechanical_fisher/net_pile");

    private static final String HELIX_VERSION = "_lite";

    public static final PartialModel[] AUGER_HELIX = Arrays.stream(new String[] {"single", "bottom", "middle", "top"})
            .map(part -> block("auger/helix_" + part + HELIX_VERSION))
            .toArray(PartialModel[]::new);
    public static final PartialModel AUGER_HELIX_EVEN = block("auger/helix_unit_even" + HELIX_VERSION);
    public static final PartialModel AUGER_HELIX_ODD = block("auger/helix_unit_odd" + HELIX_VERSION);
    public static final PartialModel AUGER_DIAGONAL_AXLE = block("auger/diagonal_axle");
    public static final PartialModel AUGER_JOINT_AXLE = block("auger/joint_axle");

    public static final PartialModel[]
            GRATED_BELT_CASING_X = casings("x", "west", "east"),
            GRATED_BELT_CASING_Z = casings("z", "north", "south");

    public static final PartialModel[][] BELT_GRATES = Arrays.stream(GrateMetal.values())
            .map(PartialModelRegistry::grates)
            .toArray(PartialModel[][]::new);

    private static PartialModel[] herderSegment(String width) {
        PartialModel[] pieces = new PartialModel[HERDER_SEGMENT_PIECES];
        for (int i = 0; i < pieces.length; i++)
            pieces[i] = block("mechanical_herder/" + width + "_segment_" + i);
        return pieces;
    }

    public static final int HERDER_SEGMENT_PIECES = 7;

    private static PartialModel[] grates(GrateMetal metal) {
        String suffix = metal == GrateMetal.PLAIN ? "" : "_" + metal.getName();
        return new PartialModel[] {block("belt_grate/grate" + suffix), block("belt_grate/grate_short_north" + suffix),
                block("belt_grate/grate_short_south" + suffix), block("belt_grate/grate_short_both" + suffix)};
    }

    private static PartialModel[] casings(String axis, String start, String end) {
        String path = "belt_grate/casing_" + axis;
        return new PartialModel[] {block(path), block(path + "_" + start), block(path + "_" + end),
                block(path + "_both")};
    }

    private static PartialModel block(String path) {
        return PartialModel.of(CreateRancher.asResource("block/" + path));
    }

    // Registers the partial models
    public static void register() {
    }
}
