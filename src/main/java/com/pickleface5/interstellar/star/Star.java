package com.pickleface5.interstellar.star;

public class Star {
    private final int id;
    private final String name;
    private final float x;
    private final float y;
    private final float z;

    public Star(int x, int y, int z) {
        this("", x, y, z);
    }

    public Star(String name, int x, int y, int z) {
        this(StarHandler.getStarsFromJson().length + 1, name, x, y, z);
    }

    private Star(int id, String name, int x, int y, int z) {
        this.id = id;
        this.name = name;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getZ() {
        return z;
    }
}