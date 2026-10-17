package com.fischgesichtert.timemod.time;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Räumliche Form einer Zeitzone. */
public sealed interface ZoneShape permits ZoneShape.Sphere, ZoneShape.Box, ZoneShape.Global {

    boolean contains(double x, double y, double z);

    /** Umschließender Quader; {@code null} bei globalen Zonen. */
    AABB bounds();

    default boolean isGlobal() {
        return false;
    }

    default boolean intersectsChunk(int chunkX, int chunkZ) {
        AABB b = bounds();
        if (b == null) {
            return true;
        }
        double minX = chunkX << 4;
        double minZ = chunkZ << 4;
        return b.maxX > minX && b.minX < minX + 16 && b.maxZ > minZ && b.minZ < minZ + 16;
    }

    void write(FriendlyByteBuf buf);

    static ZoneShape read(FriendlyByteBuf buf) {
        return switch (buf.readByte()) {
            case 0 -> new Sphere(new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()), buf.readDouble());
            case 1 -> new Box(new AABB(buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble()));
            default -> Global.INSTANCE;
        };
    }

    record Sphere(Vec3 center, double radius) implements ZoneShape {
        @Override
        public boolean contains(double x, double y, double z) {
            double dx = x - center.x;
            double dy = y - center.y;
            double dz = z - center.z;
            return dx * dx + dy * dy + dz * dz <= radius * radius;
        }

        @Override
        public AABB bounds() {
            return new AABB(center.x - radius, center.y - radius, center.z - radius, center.x + radius, center.y + radius, center.z + radius);
        }

        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeByte(0);
            buf.writeDouble(center.x);
            buf.writeDouble(center.y);
            buf.writeDouble(center.z);
            buf.writeDouble(radius);
        }
    }

    record Box(AABB box) implements ZoneShape {
        @Override
        public boolean contains(double x, double y, double z) {
            return x >= box.minX && x < box.maxX && y >= box.minY && y < box.maxY && z >= box.minZ && z < box.maxZ;
        }

        @Override
        public AABB bounds() {
            return box;
        }

        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeByte(1);
            buf.writeDouble(box.minX);
            buf.writeDouble(box.minY);
            buf.writeDouble(box.minZ);
            buf.writeDouble(box.maxX);
            buf.writeDouble(box.maxY);
            buf.writeDouble(box.maxZ);
        }
    }

    record Global() implements ZoneShape {
        public static final Global INSTANCE = new Global();

        @Override
        public boolean contains(double x, double y, double z) {
            return true;
        }

        @Override
        public AABB bounds() {
            return null;
        }

        @Override
        public boolean isGlobal() {
            return true;
        }

        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeByte(2);
        }
    }
}
