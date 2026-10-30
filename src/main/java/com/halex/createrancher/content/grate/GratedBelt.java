package com.halex.createrancher.content.grate;

// Whether a belt segment has a grate and what it's plated with
public interface GratedBelt {

    boolean createrancher$isGrated();

    // Sets the flag only. Use BeltGrates to place grates.
    void createrancher$setGrated(boolean grated);

    // What the segment's grate is plated with
    GrateMetal createrancher$getGrateMetal();

    // Sets the metal only. Use BeltGrates to plate grates.
    void createrancher$setGrateMetal(GrateMetal metal);
}
