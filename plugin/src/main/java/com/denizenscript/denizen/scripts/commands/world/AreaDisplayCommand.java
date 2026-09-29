package com.denizenscript.denizen.scripts.commands.world;

import com.denizenscript.denizen.Denizen;
import com.denizenscript.denizen.objects.*;
import com.denizenscript.denizen.objects.properties.bukkit.BukkitColorExtensions;
import com.denizenscript.denizen.utilities.Utilities;
import com.denizenscript.denizencore.exceptions.InvalidArgumentsRuntimeException;
import com.denizenscript.denizencore.objects.ObjectFetcher;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.*;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.scripts.commands.generator.*;
import com.denizenscript.denizencore.tags.TagContext;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class AreaDisplayCommand extends AbstractCommand {

    public AreaDisplayCommand() {
        setName("areadisplay");
        setSyntax("areadisplay ({auto}/create/update/remove) [<id>|...] (area:<area>) (players:<player>|...) (duration:<duration>) (particle:<particle>) (special_data:<map>) (density:<#>) (interval:<duration>) (range:<#.#>) (grid:<true/false>)");
        setRequiredArguments(1, 11);
        isProcedural = false;
        autoCompile();
    }

    // <--[command]
    // @Name AreaDisplay
    // @Syntax areadisplay ({auto}/create/update/remove) [<id>|...] (area:<area>) (players:<player>|...) (duration:<duration>) (particle:<particle>) (special_data:<map>) (density:<#>) (interval:<duration>) (range:<#.#>) (grid:<true/false>)
    // @Required 1
    // @Maximum 11
    // @Short Outlines an area with particles for players to see.
    // @Group world
    //
    // @Description
    // Outlines an area object (a CuboidTag, EllipsoidTag or PolygonTag) with particles, repeatedly, for as long as the display is around.
    //
    // You can CREATE a new display, UPDATE an existing one, or REMOVE existing ones.
    // The default 'auto' will either 'create' or 'update' depending on whether it already exists.
    //
    // Requires an ID. 'remove' also accepts a list of IDs.
    //
    // 'create' requires an area. The area is snapshotted when given:
    // changing a noted area afterwards does not change the display until you 'update' it with the area again.
    // A cuboid is drawn as the twelve edges of each of its member boxes, covering the full blocks at both corners.
    // A polygon is drawn as its top and bottom outlines, plus a vertical line at each corner.
    // An ellipsoid is drawn as three ellipses through its center, one on each axis plane.
    // Any other area type is drawn as the outline of its cuboid boundary.
    //
    // Optionally, specify a list of players to show the display to.
    // If unspecified when creating a display, it is shown to every player on the server, including any player
    // that joins while it is still around.
    // Viewers are recorded by UUID, so a viewer that relogs keeps seeing the display.
    // Specifying players when updating a display replaces its viewers with the given players.
    // Using 'remove' with a 'players' list removes those players from the viewers instead of removing the display.
    // Note that doing so on a server-wide display narrows the display down to the players that are online at that moment,
    // so it stops following new joins.
    //
    // Optionally, specify a duration, after which the display removes itself. If unspecified, the display stays until removed.
    // Specifying a duration when updating a display restarts its countdown. A duration of 0 makes the display permanent.
    //
    // Optionally, specify the particle to draw with. Defaults to 'end_rod'.
    // Particles that need extra data take it through 'special_data', in the same map format as <@link command PlayEffect>.
    // If the particle is changed while updating without giving new special_data, the previous special_data carries over to the new particle, so it must suit that particle as well.
    // It is dropped instead if the new particle takes no special_data.
    //
    // Optionally, specify the density, which is the number of particles per block along each line, from 1 to 5. Defaults to 3.
    //
    // Optionally, specify the interval between redraws. Defaults to 12 ticks.
    //
    // Optionally, specify the range, in blocks: a viewer is only sent the particles within this distance of them. Defaults to 32.
    // Vanilla clients do not render ordinary particles further than 32 blocks away, so for a range above 32 the particles are forced,
    // which also makes clients render them regardless of their particle settings.
    //
    // Optionally, specify 'grid:true' to additionally draw a grid on the surface of the area, with a line on every block.
    // An ellipsoid's grid consists of a horizontal ring on every block of height, and meridians spaced one block apart along its equator.
    // Use 'grid:false' when updating to hide the grid again.
    //
    // A single display may not consist of more than 200,000 particles. Lower the density or disable the grid for very large areas.
    //
    // Displays are only kept in memory: they are all gone after a server restart. Reloading scripts does not affect them.
    //
    // @Tags
    // <server.area_displays>
    // <server.area_display[<id>]>
    // <PlayerTag.area_display_ids>
    //
    // @Usage
    // Outlines a cuboid for every player on the server for 30 seconds.
    // - areadisplay my_arena area:<cuboid[my_arena]> duration:30s
    //
    // @Usage
    // Outlines a polygon in red dust for the linked player alone, with a grid.
    // - areadisplay claim_preview area:<[claim]> players:<player> particle:dust special_data:[size=1;color=red] grid:true
    //
    // @Usage
    // Hides the grid of an existing display.
    // - areadisplay update claim_preview grid:false
    //
    // @Usage
    // Stops showing a display to one player.
    // - areadisplay remove my_arena players:<[player]>
    //
    // @Usage
    // Removes several displays at once.
    // - areadisplay remove my_arena|claim_preview
    // -->

    public enum Action {
        AUTO, CREATE, UPDATE, REMOVE
    }

    /** 单个区域显示最多容纳的粒子点数，防止误传超大区域时撑爆内存与带宽。 */
    public static final int MAX_POINTS = 200_000;

    /** 原版客户端只渲染 32 格以内的普通粒子，超过此距离须强制发送。 */
    public static final double VANILLA_PARTICLE_RANGE = 32;

    /** 空间分桶的边长，与区块段一致。 */
    public static final int BUCKET_SIZE = 16;

    @Override
    public void addCustomTabCompletions(TabCompletionsBuilder tab) {
        tab.add(displays.keySet());
        tab.addWithPrefix("particle:", Particle.values());
        tab.addWithPrefix("grid:", Arrays.asList("true", "false"));
    }

    /** 当前所有区域显示，按小写 ID 索引，保持创建顺序。 */
    public static final Map<String, AreaDisplay> displays = new LinkedHashMap<>();

    /** 自增的内部刻计数，由绘制任务推进，用于计算重绘时机与剩余时长。 */
    public static long currentTick = 0;

    public static BukkitTask drawTask = null;

    /**
     * 一个区域显示。
     * <p>
     * 粒子点在创建与更新时一次性算好，并按 16 格见方分桶存放，
     * 每次重绘只需遍历观看者附近的桶，而不必逐点计算整个区域。
     */
    public static class AreaDisplay {

        public String id;

        /** 创建或更新时所传区域的快照，与实际绘制的形状一致，不随记名区域之后的改动而变。 */
        public AreaContainmentObject area;

        public String worldName;

        /** 已登记的观看者，无论其当下是否在线。 */
        public final Set<UUID> players = new LinkedHashSet<>();

        /** 是否展示给全服玩家，包括其后才加入的玩家。 */
        public boolean forAllPlayers;

        public Particle particle;

        /** 原样保存的 special_data，更换粒子时据此重新解析。 */
        public MapTag specialData;

        public Object particleData;

        public int density;

        public int interval;

        public double range;

        public boolean grid;

        /** 到期的内部刻，小于 0 表示永久显示。 */
        public long expireTick = -1;

        public long nextDrawTick;

        public List<PointBucket> buckets = new ArrayList<>();

        public int pointCount;

        public boolean isViewer(UUID uuid) {
            return forAllPlayers || players.contains(uuid);
        }

        public void draw() {
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                return;
            }
            boolean force = range > VANILLA_PARTICLE_RANGE;
            double rangeSquared = range * range;
            for (Player player : world.getPlayers()) {
                if (!isViewer(player.getUniqueId())) {
                    continue;
                }
                Location location = player.getLocation();
                double px = location.getX(), py = location.getY(), pz = location.getZ();
                for (PointBucket bucket : buckets) {
                    if (bucket.distanceSquaredTo(px, py, pz) > rangeSquared) {
                        continue;
                    }
                    double[] coords = bucket.coords;
                    for (int i = 0; i < bucket.size; i += 3) {
                        double dx = coords[i] - px, dy = coords[i + 1] - py, dz = coords[i + 2] - pz;
                        if (dx * dx + dy * dy + dz * dz <= rangeSquared) {
                            player.spawnParticle(particle, coords[i], coords[i + 1], coords[i + 2], 1, 0, 0, 0, 0, particleData, force);
                        }
                    }
                }
            }
        }

        public MapTag describe() {
            MapTag result = new MapTag();
            result.putObject("id", new ElementTag(id, true));
            result.putObject("area", area);
            result.putObject("all_players", new ElementTag(forAllPlayers));
            ListTag playerList = new ListTag();
            if (forAllPlayers) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    playerList.addObject(new PlayerTag(player));
                }
            }
            else {
                for (UUID uuid : players) {
                    playerList.addObject(new PlayerTag(uuid));
                }
            }
            result.putObject("players", playerList);
            if (expireTick >= 0) {
                result.putObject("duration", new DurationTag(Math.max(0L, expireTick - currentTick)));
            }
            result.putObject("particle", new ElementTag(CoreUtilities.toLowerCase(particle.name()), true));
            if (specialData != null) {
                result.putObject("special_data", specialData);
            }
            result.putObject("density", new ElementTag(density));
            result.putObject("interval", new DurationTag((long) interval));
            result.putObject("range", new ElementTag(range));
            result.putObject("grid", new ElementTag(grid));
            result.putObject("points", new ElementTag(pointCount));
            return result;
        }
    }

    /** 一个 16 格见方空间桶内的粒子点，坐标按 x、y、z 依次平铺存放。 */
    public static class PointBucket {

        public final int bx, by, bz;

        public double[] coords = new double[48];

        public int size = 0;

        public PointBucket(int bx, int by, int bz) {
            this.bx = bx;
            this.by = by;
            this.bz = bz;
        }

        public void add(double x, double y, double z) {
            if (size + 3 > coords.length) {
                coords = Arrays.copyOf(coords, coords.length * 2);
            }
            coords[size++] = x;
            coords[size++] = y;
            coords[size++] = z;
        }

        /** 给定坐标到本桶包围盒的最短距离的平方，坐标在盒内时为 0。 */
        public double distanceSquaredTo(double x, double y, double z) {
            double dx = axisDistance(x, bx * BUCKET_SIZE);
            double dy = axisDistance(y, by * BUCKET_SIZE);
            double dz = axisDistance(z, bz * BUCKET_SIZE);
            return dx * dx + dy * dy + dz * dz;
        }

        public static double axisDistance(double value, double min) {
            if (value < min) {
                return min - value;
            }
            double max = min + BUCKET_SIZE;
            return value > max ? value - max : 0;
        }
    }

    /** 收集粒子点并按空间分桶，超出上限后不再收集。 */
    public static class PointCollector {

        public final double density;

        public final Map<Long, PointBucket> buckets = new HashMap<>();

        public int count = 0;

        public boolean overflow = false;

        public PointCollector(int density) {
            this.density = density;
        }

        public void add(double x, double y, double z) {
            if (overflow) {
                return;
            }
            if (++count > MAX_POINTS) {
                overflow = true;
                return;
            }
            int bx = (int) Math.floor(x / BUCKET_SIZE), by = (int) Math.floor(y / BUCKET_SIZE), bz = (int) Math.floor(z / BUCKET_SIZE);
            long key = (((long) bx & 0x3FFFFF) << 42) | (((long) by & 0xFFFFF) << 22) | ((long) bz & 0x3FFFFF);
            PointBucket bucket = buckets.get(key);
            if (bucket == null) {
                bucket = new PointBucket(bx, by, bz);
                buckets.put(key, bucket);
            }
            bucket.add(x, y, z);
        }

        public int stepsFor(double length) {
            return Math.max(1, (int) Math.ceil(length * density));
        }

        /** 画一条线段，含两端点。 */
        public void line(double x1, double y1, double z1, double x2, double y2, double z2) {
            lineInternal(x1, y1, z1, x2, y2, z2, true);
        }

        /** 画一条线段，不含两端点，用于端点已落在轮廓上的网格线。 */
        public void lineInterior(double x1, double y1, double z1, double x2, double y2, double z2) {
            lineInternal(x1, y1, z1, x2, y2, z2, false);
        }

        public void lineInternal(double x1, double y1, double z1, double x2, double y2, double z2, boolean withEnds) {
            double dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
            int steps = stepsFor(Math.sqrt(dx * dx + dy * dy + dz * dz));
            int start = withEnds ? 0 : 1, end = withEnds ? steps : steps - 1;
            for (int i = start; i <= end && !overflow; i++) {
                double t = (double) i / steps;
                add(x1 + dx * t, y1 + dy * t, z1 + dz * t);
            }
        }

        /**
         * 画一段椭圆弧：点为 center + axisA * cos(t) + axisB * sin(t)，t 取 [from, to]。
         * 闭合的整圈不重复画终点。
         */
        public void ellipseArc(double cx, double cy, double cz, double ax, double ay, double az, double bx, double by, double bz,
                               double from, double to, boolean withEnds) {
            double a = Math.sqrt(ax * ax + ay * ay + az * az), b = Math.sqrt(bx * bx + by * by + bz * bz);
            double fullLength = Math.PI * (3 * (a + b) - Math.sqrt((3 * a + b) * (a + 3 * b)));
            double span = to - from;
            boolean closed = span >= Math.PI * 2 - 1e-9;
            int steps = Math.max(closed ? 8 : 2, stepsFor(fullLength * span / (Math.PI * 2)));
            int start = withEnds ? 0 : 1, end = (closed || !withEnds) ? steps - 1 : steps;
            for (int i = start; i <= end && !overflow; i++) {
                double t = from + span * i / steps;
                double cos = Math.cos(t), sin = Math.sin(t);
                add(cx + ax * cos + bx * sin, cy + ay * cos + by * sin, cz + az * cos + bz * sin);
            }
        }

        public List<PointBucket> finish() {
            List<PointBucket> result = new ArrayList<>(buckets.values());
            for (PointBucket bucket : result) {
                bucket.coords = Arrays.copyOf(bucket.coords, bucket.size);
            }
            return result;
        }
    }

    public static void collectCuboid(PointCollector collector, CuboidTag cuboid, boolean grid) {
        for (CuboidTag.LocationPair pair : cuboid.pairs) {
            double x1 = pair.low.getBlockX(), y1 = pair.low.getBlockY(), z1 = pair.low.getBlockZ();
            double x2 = pair.high.getBlockX() + 1, y2 = pair.high.getBlockY() + 1, z2 = pair.high.getBlockZ() + 1;
            for (double y : new double[] {y1, y2}) {
                collector.line(x1, y, z1, x2, y, z1);
                collector.line(x1, y, z2, x2, y, z2);
                collector.line(x1, y, z1, x1, y, z2);
                collector.line(x2, y, z1, x2, y, z2);
            }
            collector.line(x1, y1, z1, x1, y2, z1);
            collector.line(x2, y1, z1, x2, y2, z1);
            collector.line(x1, y1, z2, x1, y2, z2);
            collector.line(x2, y1, z2, x2, y2, z2);
            if (!grid) {
                continue;
            }
            // 六个面上每隔一格各画一条横线与竖线，线的两端落在棱上，故不含端点。
            for (double x = x1 + 1; x < x2; x++) {
                for (double z : new double[] {z1, z2}) {
                    collector.lineInterior(x, y1, z, x, y2, z);
                }
                for (double y : new double[] {y1, y2}) {
                    collector.lineInterior(x, y, z1, x, y, z2);
                }
            }
            for (double y = y1 + 1; y < y2; y++) {
                for (double z : new double[] {z1, z2}) {
                    collector.lineInterior(x1, y, z, x2, y, z);
                }
                for (double x : new double[] {x1, x2}) {
                    collector.lineInterior(x, y, z1, x, y, z2);
                }
            }
            for (double z = z1 + 1; z < z2; z++) {
                for (double x : new double[] {x1, x2}) {
                    collector.lineInterior(x, y1, z, x, y2, z);
                }
                for (double y : new double[] {y1, y2}) {
                    collector.lineInterior(x1, y, z, x2, y, z);
                }
            }
        }
    }

    public static void collectPolygon(PointCollector collector, PolygonTag polygon, boolean grid) {
        List<PolygonTag.Corner> corners = polygon.corners;
        int size = corners.size();
        double y1 = polygon.yMin, y2 = polygon.yMax;
        for (int i = 0; i < size; i++) {
            PolygonTag.Corner start = corners.get(i), end = corners.get((i + 1) % size);
            collector.line(start.x, y1, start.z, end.x, y1, end.z);
            if (y2 > y1) {
                collector.line(start.x, y2, start.z, end.x, y2, end.z);
                collector.line(start.x, y1, start.z, start.x, y2, start.z);
            }
        }
        if (!grid || size < 3) {
            return;
        }
        // 侧面：沿每条边每隔一格画一条竖线，并在每格高度画一条横线。
        for (int i = 0; i < size; i++) {
            PolygonTag.Corner start = corners.get(i), end = corners.get((i + 1) % size);
            double dx = end.x - start.x, dz = end.z - start.z;
            double length = Math.sqrt(dx * dx + dz * dz);
            if (y2 > y1) {
                for (double d = 1; d < length; d++) {
                    double t = d / length;
                    collector.lineInterior(start.x + dx * t, y1, start.z + dz * t, start.x + dx * t, y2, start.z + dz * t);
                }
            }
            for (double y = Math.floor(y1) + 1; y < y2; y++) {
                collector.lineInterior(start.x, y, start.z, end.x, y, end.z);
            }
        }
        // 顶面与底面：以每格的 x 与 z 坐标切割多边形，按奇偶规则两两配对交点，画出落在多边形内的线段。
        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, minZ = Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
        for (PolygonTag.Corner corner : corners) {
            minX = Math.min(minX, corner.x);
            maxX = Math.max(maxX, corner.x);
            minZ = Math.min(minZ, corner.z);
            maxZ = Math.max(maxZ, corner.z);
        }
        double[] faces = y2 > y1 ? new double[] {y1, y2} : new double[] {y1};
        for (double x = Math.floor(minX) + 1; x < maxX; x++) {
            List<Double> hits = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                PolygonTag.Corner start = corners.get(i), end = corners.get((i + 1) % size);
                if ((start.x > x) != (end.x > x)) {
                    hits.add(start.z + (x - start.x) * (end.z - start.z) / (end.x - start.x));
                }
            }
            Collections.sort(hits);
            for (int i = 0; i + 1 < hits.size(); i += 2) {
                for (double y : faces) {
                    collector.lineInterior(x, y, hits.get(i), x, y, hits.get(i + 1));
                }
            }
        }
        for (double z = Math.floor(minZ) + 1; z < maxZ; z++) {
            List<Double> hits = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                PolygonTag.Corner start = corners.get(i), end = corners.get((i + 1) % size);
                if ((start.z > z) != (end.z > z)) {
                    hits.add(start.x + (z - start.z) * (end.x - start.x) / (end.z - start.z));
                }
            }
            Collections.sort(hits);
            for (int i = 0; i + 1 < hits.size(); i += 2) {
                for (double y : faces) {
                    collector.lineInterior(hits.get(i), y, z, hits.get(i + 1), y, z);
                }
            }
        }
    }

    public static void collectEllipsoid(PointCollector collector, EllipsoidTag ellipsoid, boolean grid) {
        double cx = ellipsoid.center.getX(), cy = ellipsoid.center.getY(), cz = ellipsoid.center.getZ();
        double rx = ellipsoid.size.getX(), ry = ellipsoid.size.getY(), rz = ellipsoid.size.getZ();
        double full = Math.PI * 2;
        collector.ellipseArc(cx, cy, cz, rx, 0, 0, 0, 0, rz, 0, full, true);
        collector.ellipseArc(cx, cy, cz, rx, 0, 0, 0, ry, 0, 0, full, true);
        collector.ellipseArc(cx, cy, cz, 0, 0, rz, 0, ry, 0, 0, full, true);
        if (!grid) {
            return;
        }
        // 纬线：每格高度一圈水平椭圆，赤道已画过，跳过。
        for (double y = Math.floor(cy - ry) + 1; y < cy + ry; y++) {
            double offset = (y - cy) / ry;
            if (Math.abs(y - cy) < 1e-6) {
                continue;
            }
            double scale = Math.sqrt(1 - offset * offset);
            collector.ellipseArc(cx, y, cz, rx * scale, 0, 0, 0, 0, rz * scale, 0, full, true);
        }
        // 经线：沿赤道每隔约一格一条，自顶至底画半圈，两极点已落在轮廓上，故不含端点。
        double a = rx, b = rz;
        double equator = Math.PI * (3 * (a + b) - Math.sqrt((3 * a + b) * (a + 3 * b)));
        int meridians = Math.max(4, (int) Math.ceil(equator));
        for (int i = 0; i < meridians && !collector.overflow; i++) {
            double phi = full * i / meridians;
            double cos = Math.cos(phi), sin = Math.sin(phi);
            collector.ellipseArc(cx, cy, cz, 0, ry, 0, rx * cos, 0, rz * sin, 0, Math.PI, false);
        }
    }

    /**
     * 取区域的快照。
     * <p>
     * 记名区域的 duplicate() 返回其本身，之后改动记名区域会连带改变快照，故须逐类型 clone()。
     * 除长方体、多边形、椭球体以外的区域一律以其长方体边界代替。
     */
    public static AreaContainmentObject snapshotArea(AreaContainmentObject area) {
        if (area instanceof CuboidTag cuboid) {
            return cuboid.refreshState().clone();
        }
        else if (area instanceof PolygonTag polygon) {
            return polygon.refreshState().clone();
        }
        else if (area instanceof EllipsoidTag ellipsoid) {
            return ellipsoid.refreshState().clone();
        }
        return area.getCuboidBoundary().clone();
    }

    /** 按区域类型收集粒子点，传入的须是 snapshotArea 的结果。 */
    public static PointCollector collectPoints(AreaContainmentObject area, int density, boolean grid) {
        PointCollector collector = new PointCollector(density);
        if (area instanceof PolygonTag polygon) {
            collectPolygon(collector, polygon, grid);
        }
        else if (area instanceof EllipsoidTag ellipsoid) {
            collectEllipsoid(collector, ellipsoid, grid);
        }
        else {
            collectCuboid(collector, (CuboidTag) area, grid);
        }
        return collector;
    }

    public static AreaContainmentObject parseArea(ObjectTag input, TagContext context) {
        if (input instanceof AreaContainmentObject area) {
            return area;
        }
        ObjectTag reparsed = ObjectFetcher.pickObjectFor(input.toString(), context);
        if (reparsed instanceof AreaContainmentObject area) {
            return area;
        }
        throw new InvalidArgumentsRuntimeException("Area input '" + input + "' is not a valid area object.");
    }

    /** 按 playeffect 的 special_data 映射格式解析粒子附加数据，不需要附加数据的粒子返回 null。 */
    public static Object parseParticleData(Particle particle, MapTag dataMap, TagContext context) {
        Class<?> clazz = particle.getDataType() == Void.class ? null : particle.getDataType();
        if (clazz == null) {
            return null;
        }
        String name = particle.name();
        if (dataMap == null) {
            throw new InvalidArgumentsRuntimeException("Missing required special_data for particle: " + name);
        }
        if (clazz == Particle.DustOptions.class) {
            ElementTag size = dataMap.getObjectAs("size", ElementTag.class, context);
            ColorTag color = dataMap.getObjectAs("color", ColorTag.class, context);
            if (size == null || !size.isFloat() || color == null) {
                throw new InvalidArgumentsRuntimeException("special_data input must have a 'size' key with a valid number and a 'color' key with a valid ColorTag, for particle: " + name);
            }
            return new Particle.DustOptions(BukkitColorExtensions.getColor(color), size.asFloat());
        }
        else if (clazz == BlockData.class) {
            MaterialTag material = dataMap.getObjectAs("material", MaterialTag.class, context);
            if (material == null) {
                throw new InvalidArgumentsRuntimeException("special_data input must have a 'material' key with a valid MaterialTag, for particle: " + name);
            }
            return material.getModernData();
        }
        else if (clazz == ItemStack.class) {
            ItemTag item = dataMap.getObjectAs("item", ItemTag.class, context);
            if (item == null) {
                throw new InvalidArgumentsRuntimeException("special_data input must have a 'item' key with a valid ItemTag, for particle: " + name);
            }
            return item.getItemStack();
        }
        else if (clazz == Particle.DustTransition.class) {
            ElementTag size = dataMap.getObjectAs("size", ElementTag.class, context);
            ColorTag fromColor = dataMap.getObjectAs("from", ColorTag.class, context);
            ColorTag toColor = dataMap.getObjectAs("to", ColorTag.class, context);
            if (size == null || !size.isFloat() || fromColor == null || toColor == null) {
                throw new InvalidArgumentsRuntimeException("special_data input must have a 'size' key with a valid number, and 'from' and 'to' keys with valid ColorTags, for particle: " + name);
            }
            return new Particle.DustTransition(BukkitColorExtensions.getColor(fromColor), BukkitColorExtensions.getColor(toColor), size.asFloat());
        }
        else if (clazz == Vibration.class) {
            DurationTag duration = dataMap.getObjectAs("duration", DurationTag.class, context);
            LocationTag origin = dataMap.getObjectAs("origin", LocationTag.class, context);
            ObjectTag destination = dataMap.getObject("destination");
            if (duration == null || origin == null || destination == null) {
                throw new InvalidArgumentsRuntimeException("special_data input must have 'duration', 'origin' and 'destination' keys, for particle: " + name);
            }
            Vibration.Destination destObj;
            if (destination.shouldBeType(EntityTag.class)) {
                destObj = new Vibration.Destination.EntityDestination(destination.asType(EntityTag.class, context).getBukkitEntity());
            }
            else if (destination.shouldBeType(LocationTag.class)) {
                destObj = new Vibration.Destination.BlockDestination(destination.asType(LocationTag.class, context));
            }
            else {
                throw new InvalidArgumentsRuntimeException("special_data input must have a 'destination' key with a valid LocationTag or EntityTag, for particle: " + name);
            }
            return new Vibration(origin, destObj, duration.getTicksAsInt());
        }
        else if (clazz == Particle.Trail.class) {
            ColorTag color = dataMap.getObjectAs("color", ColorTag.class, context);
            LocationTag target = dataMap.getObjectAs("target", LocationTag.class, context);
            DurationTag duration = dataMap.getObjectAs("duration", DurationTag.class, context);
            if (color == null || target == null || duration == null) {
                throw new InvalidArgumentsRuntimeException("special_data input must have a 'color' key with a valid ColorTag, a 'target' key with a valid LocationTag and a 'duration' key with a valid DurationTag, for particle: " + name);
            }
            return new Particle.Trail(target, BukkitColorExtensions.getColor(color), duration.getTicksAsInt());
        }
        else if (clazz == Color.class) {
            ColorTag color = dataMap.getObjectAs("color", ColorTag.class, context);
            if (color == null) {
                throw new InvalidArgumentsRuntimeException("special_data input must have a 'color' key with a valid ColorTag, for particle: " + name);
            }
            return BukkitColorExtensions.getColor(color);
        }
        else if (clazz == Integer.class) {
            DurationTag duration = dataMap.getObjectAs("duration", DurationTag.class, context);
            if (duration == null) {
                throw new InvalidArgumentsRuntimeException("special_data input must have a 'duration' key with a valid DurationTag, for particle: " + name);
            }
            return duration.getTicksAsInt();
        }
        else if (clazz == Float.class) {
            ElementTag radians = dataMap.getObjectAs("radians", ElementTag.class, context);
            if (radians == null || !radians.isFloat()) {
                throw new InvalidArgumentsRuntimeException("special_data input must have a 'radians' key with a valid number, for particle: " + name);
            }
            return radians.asFloat();
        }
        throw new InvalidArgumentsRuntimeException("Unknown particle data type: " + clazz.getCanonicalName() + " for particle: " + name);
    }

    /** 有区域显示时才启动绘制任务，全部移除后随即停止，未用到该命令的服务器不必承担其开销。 */
    public static void ensureDrawTask() {
        if (drawTask != null && !drawTask.isCancelled()) {
            return;
        }
        drawTask = new BukkitRunnable() {
            @Override
            public void run() {
                tickDisplays();
            }
        }.runTaskTimer(Denizen.getInstance(), 1, 1);
    }

    public static void tickDisplays() {
        currentTick++;
        Iterator<AreaDisplay> iterator = displays.values().iterator();
        while (iterator.hasNext()) {
            AreaDisplay display = iterator.next();
            if (display.expireTick >= 0 && currentTick >= display.expireTick) {
                iterator.remove();
                continue;
            }
            if (currentTick >= display.nextDrawTick) {
                display.nextDrawTick = currentTick + display.interval;
                try {
                    display.draw();
                }
                catch (Throwable ex) {
                    Debug.echoError("Area display '" + display.id + "' failed to draw, and has been removed:");
                    Debug.echoError(ex);
                    iterator.remove();
                }
            }
        }
        if (displays.isEmpty() && drawTask != null) {
            drawTask.cancel();
            drawTask = null;
        }
    }

    public static void autoExecute(ScriptEntry scriptEntry,
                                   @ArgName("action") @ArgDefaultText("auto") Action action,
                                   @ArgName("ids") @ArgLinear ListTag ids,
                                   @ArgName("area") @ArgPrefixed @ArgDefaultNull ObjectTag areaInput,
                                   @ArgName("players") @ArgPrefixed @ArgDefaultNull @ArgSubType(PlayerTag.class) List<PlayerTag> players,
                                   @ArgName("duration") @ArgPrefixed @ArgDefaultNull DurationTag duration,
                                   @ArgName("particle") @ArgPrefixed @ArgDefaultNull ElementTag particleInput,
                                   @ArgName("special_data") @ArgPrefixed @ArgDefaultNull MapTag specialData,
                                   @ArgName("density") @ArgPrefixed @ArgDefaultNull ElementTag densityInput,
                                   @ArgName("interval") @ArgPrefixed @ArgDefaultNull DurationTag intervalInput,
                                   @ArgName("range") @ArgPrefixed @ArgDefaultNull ElementTag rangeInput,
                                   @ArgName("grid") @ArgPrefixed @ArgDefaultNull ElementTag gridInput) {
        if (ids.isEmpty()) {
            throw new InvalidArgumentsRuntimeException("Must specify at least one ID.");
        }
        if (action == Action.REMOVE) {
            for (String rawId : ids) {
                remove(scriptEntry, CoreUtilities.toLowerCase(rawId), players);
            }
            return;
        }
        if (ids.size() > 1) {
            throw new InvalidArgumentsRuntimeException("Only 'remove' accepts more than one ID.");
        }
        String id = CoreUtilities.toLowerCase(ids.get(0));
        if (action == Action.AUTO) {
            action = displays.containsKey(id) ? Action.UPDATE : Action.CREATE;
        }
        AreaDisplay existing = displays.get(id);
        if (action == Action.CREATE && existing != null) {
            Debug.echoError(scriptEntry, "Area display '" + id + "' already exists!");
            return;
        }
        if (action == Action.UPDATE && existing == null) {
            Debug.echoError(scriptEntry, "Area display '" + id + "' does not exist!");
            return;
        }
        if (action == Action.CREATE && areaInput == null) {
            throw new InvalidArgumentsRuntimeException("Must specify an area to create an area display.");
        }
        // 先把所有输入校验、换算完毕，再一并写入，任何一项出错都不会留下改了一半的显示。
        AreaContainmentObject area = areaInput == null ? null : snapshotArea(parseArea(areaInput, scriptEntry.getContext()));
        if (area != null && area.getWorld() == null) {
            throw new InvalidArgumentsRuntimeException("Area '" + area + "' is not in a valid world.");
        }
        Particle particle = existing == null ? Particle.END_ROD : existing.particle;
        if (particleInput != null) {
            particle = Utilities.elementToEnumlike(particleInput, Particle.class);
            if (particle == null) {
                throw new InvalidArgumentsRuntimeException("Invalid particle '" + particleInput + "'.");
            }
        }
        MapTag newSpecialData = specialData != null ? specialData : (existing == null ? null : existing.specialData);
        Object particleData;
        if (specialData == null && particle.getDataType() == Void.class) {
            // 换成不需要附加数据的粒子时，旧的 special_data 已无意义，一并丢弃。
            newSpecialData = null;
            particleData = null;
        }
        else if (specialData == null && existing != null && particle == existing.particle) {
            particleData = existing.particleData;
        }
        else {
            if (specialData != null && particle.getDataType() == Void.class) {
                throw new InvalidArgumentsRuntimeException("Particles of type '" + particle.name() + "' cannot take special_data as input.");
            }
            particleData = parseParticleData(particle, newSpecialData, scriptEntry.getContext());
        }
        int density = existing == null ? 3 : existing.density;
        if (densityInput != null) {
            if (!densityInput.isInt() || densityInput.asInt() < 1 || densityInput.asInt() > 5) {
                throw new InvalidArgumentsRuntimeException("Density must be a whole number from 1 to 5.");
            }
            density = densityInput.asInt();
        }
        int interval = existing == null ? 12 : existing.interval;
        if (intervalInput != null) {
            if (intervalInput.getTicks() < 1) {
                throw new InvalidArgumentsRuntimeException("Interval must be at least 1 tick.");
            }
            interval = intervalInput.getTicksAsInt();
        }
        double range = existing == null ? VANILLA_PARTICLE_RANGE : existing.range;
        if (rangeInput != null) {
            if (!rangeInput.isDouble() || rangeInput.asDouble() <= 0) {
                throw new InvalidArgumentsRuntimeException("Range must be a number above 0.");
            }
            range = rangeInput.asDouble();
        }
        boolean grid = existing != null && existing.grid;
        if (gridInput != null) {
            if (!gridInput.isBoolean()) {
                throw new InvalidArgumentsRuntimeException("Grid must be 'true' or 'false'.");
            }
            grid = gridInput.asBoolean();
        }
        PointCollector collector = null;
        if (existing == null || area != null || density != existing.density || grid != existing.grid) {
            collector = collectPoints(area != null ? area : existing.area, density, grid);
            if (collector.overflow) {
                Debug.echoError(scriptEntry, "Area display '" + id + "' would need more than " + MAX_POINTS + " particles. Use a lower density, disable the grid, or display a smaller area.");
                return;
            }
        }
        AreaDisplay display = existing;
        if (display == null) {
            display = new AreaDisplay();
            display.id = id;
            display.forAllPlayers = players == null;
            display.nextDrawTick = currentTick;
        }
        if (area != null) {
            display.area = area;
            display.worldName = area.getWorld().getName();
        }
        if (collector != null) {
            display.buckets = collector.finish();
            display.pointCount = collector.count;
        }
        if (players != null) {
            display.forAllPlayers = false;
            display.players.clear();
            for (PlayerTag player : players) {
                display.players.add(player.getUUID());
            }
        }
        if (duration != null) {
            display.expireTick = duration.getTicks() > 0 ? currentTick + duration.getTicks() : -1;
        }
        display.particle = particle;
        display.specialData = newSpecialData;
        display.particleData = particleData;
        display.density = density;
        if (interval != display.interval) {
            display.nextDrawTick = Math.min(display.nextDrawTick, currentTick + interval);
        }
        display.interval = interval;
        display.range = range;
        display.grid = grid;
        displays.put(id, display);
        ensureDrawTask();
    }

    public static void remove(ScriptEntry scriptEntry, String id, List<PlayerTag> players) {
        AreaDisplay display = displays.get(id);
        if (display == null) {
            Debug.echoError(scriptEntry, "Area display '" + id + "' does not exist!");
            return;
        }
        if (players == null) {
            displays.remove(id);
            return;
        }
        if (display.forAllPlayers) {
            // 全服可见的显示一旦要剔除个别玩家，就只能落到当下在线的这批玩家上，不再跟随其后加入的玩家。
            display.forAllPlayers = false;
            for (Player player : Bukkit.getOnlinePlayers()) {
                display.players.add(player.getUniqueId());
            }
        }
        for (PlayerTag player : players) {
            display.players.remove(player.getUUID());
        }
    }
}
