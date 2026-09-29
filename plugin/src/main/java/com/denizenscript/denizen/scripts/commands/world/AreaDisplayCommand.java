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
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class AreaDisplayCommand extends AbstractCommand {

    public AreaDisplayCommand() {
        setName("areadisplay");
        setSyntax("areadisplay ({auto}/create/update/remove) [<id>|...] (area:<area>) (players:<player>|...) (duration:<duration>) (particle:<particle>) (special_data:<map>) (density:<#>) (interval:<duration>) (range:<#.#>) (max_particles:<#>) (grid:<true/false>)");
        setRequiredArguments(1, 12);
        isProcedural = false;
        autoCompile();
    }

    // <--[command]
    // @Name AreaDisplay
    // @Syntax areadisplay ({auto}/create/update/remove) [<id>|...] (area:<area>) (players:<player>|...) (duration:<duration>) (particle:<particle>) (special_data:<map>) (density:<#>) (interval:<duration>) (range:<#.#>) (max_particles:<#>) (grid:<true/false>)
    // @Required 1
    // @Maximum 12
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
    // Optionally, specify the particle to draw with. Defaults to 'flame', which fades quickly enough that successive redraws do not pile up.
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
    // Optionally, specify the maximum number of particles a viewer is sent per redraw. Defaults to 1000.
    // When more points than that are in range, the ones nearest to the viewer are sent and the rest are skipped for that redraw.
    //
    // Each redraw is spread out over its interval, up to 1 second, rather than sent all at once: every tick sends an even share of the particles,
    // interleaved so each share covers the whole display. A redraw is skipped if the previous one is still being picked. Picking and sending the particles happens off the main thread.
    //
    // Optionally, specify 'grid:true' to additionally draw a grid on the surface of the area.
    // The grid gets sparser as the surface gets bigger: its lines are spaced one block apart, plus one more block for every 256 square blocks
    // of the surface they are on, up to 30 blocks apart. Each pair of opposite faces of a cuboid is spaced separately;
    // the side faces of a polygon share the spacing of the widest one, so the horizontal lines meet all the way around.
    // An ellipsoid's grid consists of horizontal rings and meridians, spaced by the biggest face of its bounding box.
    // Grid lines are drawn with at most 2 particles per block, however high the density is.
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

    /** 网格线的每格粒子数上限，网格只为勾勒表面，无需与轮廓一样密。 */
    public static final int MAX_GRID_DENSITY = 2;

    /** 网格间距按所在表面的面积放宽：每 256 平方格加宽一格。 */
    public static final double GRID_AREA_PER_SPACING = 256;

    /** 网格间距的上限，再宽就看不出是网格了。 */
    public static final int MAX_GRID_SPACING = 30;

    /** 每位观看者每次重绘默认最多收到的粒子数。 */
    public static final int DEFAULT_MAX_PARTICLES = 1000;

    /** 一刻的毫秒数，用于把一次重绘分摊到各刻发出。 */
    public static final long TICK_MILLIS = 50;

    /** 一次重绘至多分摊的批数，即至多分摊到 1 秒。间隔再长也不必拖得更久，免得线程池里积压大量待发的批次。 */
    public static final int MAX_SLICES = 20;

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
     * 挑选并发送粒子的后台线程。
     * <p>
     * 主线程每次重绘只记下观看者的位置，挑点、排序与发包都在这里完成，并按刻分摊。
     * 发粒子包只是把数据包交给玩家的网络连接，Paper 下可在任意线程调用。
     */
    public static ScheduledThreadPoolExecutor sender = null;

    public static ScheduledThreadPoolExecutor getSender() {
        if (sender == null || sender.isShutdown()) {
            sender = new ScheduledThreadPoolExecutor(1, runnable -> {
                Thread thread = new Thread(runnable, "Denizen AreaDisplay Sender");
                thread.setDaemon(true);
                return thread;
            });
            // 显示全部移除后即关闭线程，尚未发出的分摊批次随之作废。
            sender.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        }
        return sender;
    }

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

        public int maxParticles;

        /** 已被移除，后台线程据此放弃尚未发出的分摊批次。 */
        public volatile boolean removed = false;

        /** 上一轮尚在后台挑点，主线程据此跳过这一轮，免得后台跟不上时任务越积越多。 */
        public volatile boolean selecting = false;

        /** 后台线程已报告过错误，此后同类错误不再重复刷屏。 */
        public volatile boolean errorReported = false;

        /** 到期的内部刻，小于 0 表示永久显示。 */
        public long expireTick = -1;

        public long nextDrawTick;

        public List<PointBucket> buckets = new ArrayList<>();

        public int pointCount;

        public boolean isViewer(UUID uuid) {
            return forAllPlayers || players.contains(uuid);
        }

        /**
         * 在主线程记下各观看者此刻的位置，再交给后台线程挑点与发送。
         * 桶列表在更新时整体替换、此后不再改动，故可直接交给后台线程读取。
         */
        public void draw() {
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                return;
            }
            List<ViewerSnapshot> viewers = new ArrayList<>();
            for (Player player : world.getPlayers()) {
                if (isViewer(player.getUniqueId())) {
                    Location location = player.getLocation();
                    viewers.add(new ViewerSnapshot(player, location.getX(), location.getY(), location.getZ()));
                }
            }
            if (viewers.isEmpty() || selecting) {
                return;
            }
            selecting = true;
            ScheduledThreadPoolExecutor executor = getSender();
            DrawJob job = new DrawJob(this, executor, world, viewers, buckets, particle, particleData, range, maxParticles, interval);
            executor.execute(job::select);
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
            result.putObject("max_particles", new ElementTag(maxParticles));
            result.putObject("grid", new ElementTag(grid));
            result.putObject("points", new ElementTag(pointCount));
            return result;
        }
    }

    public record ViewerSnapshot(Player player, double x, double y, double z) {
    }

    /**
     * 一次重绘，在后台线程执行。
     * <p>
     * 先为每位观看者挑出范围内的点，超出上限时只留最近的那些；
     * 再把一次重绘拆成与间隔刻数相同的批次，第 k 批取序号除以批数余 k 的点，于第 k 刻发出，
     * 使每一批都均匀覆盖整个显示，而非一刻内把全部粒子挤在一起发出。
     */
    public static class DrawJob {

        public final AreaDisplay display;

        /** 由主线程传入，后台线程不得自行调用 getSender()，以免与主线程关闭、重建线程池的操作相互竞争。 */
        public final ScheduledThreadPoolExecutor executor;

        public final World world;

        public final List<ViewerSnapshot> viewers;

        public final List<PointBucket> buckets;

        public final Particle particle;

        public final Object particleData;

        public final double range;

        public final int maxParticles;

        public final int slices;

        public final boolean force;

        /** 各观看者选中的点，坐标按 x、y、z 依次平铺，与 viewers 一一对应。 */
        public final List<double[]> selections = new ArrayList<>();

        public DrawJob(AreaDisplay display, ScheduledThreadPoolExecutor executor, World world, List<ViewerSnapshot> viewers, List<PointBucket> buckets, Particle particle, Object particleData,
                       double range, int maxParticles, int interval) {
            this.display = display;
            this.executor = executor;
            this.world = world;
            this.viewers = viewers;
            this.buckets = buckets;
            this.particle = particle;
            this.particleData = particleData;
            this.range = range;
            this.maxParticles = maxParticles;
            this.slices = Math.max(1, Math.min(interval, MAX_SLICES));
            this.force = range > VANILLA_PARTICLE_RANGE;
        }

        /** 线程池会吞掉任务抛出的异常，故在此自行报告；每个显示只报告第一次，以免每刻刷屏。 */
        public void reportError(Throwable ex) {
            if (display.errorReported) {
                return;
            }
            display.errorReported = true;
            Debug.echoError("Area display '" + display.id + "' failed to draw (further errors from it will not be reported):");
            Debug.echoError(ex);
        }

        public void select() {
            try {
                if (display.removed) {
                    return;
                }
                for (ViewerSnapshot viewer : viewers) {
                    selections.add(selectFor(viewer));
                }
            }
            catch (Throwable ex) {
                reportError(ex);
                return;
            }
            finally {
                display.selecting = false;
            }
            try {
                for (int slice = 0; slice < slices; slice++) {
                    int current = slice;
                    executor.schedule(() -> sendSlice(current), slice * TICK_MILLIS, TimeUnit.MILLISECONDS);
                }
            }
            catch (RejectedExecutionException ex) {
                // 线程池已在显示全部移除时关闭，这一轮不必再发。
            }
        }

        public double[] selectFor(ViewerSnapshot viewer) {
            double rangeSquared = range * range;
            double[] coords = new double[48];
            float[] distances = new float[16];
            int count = 0;
            for (PointBucket bucket : buckets) {
                if (bucket.distanceSquaredTo(viewer.x(), viewer.y(), viewer.z()) > rangeSquared) {
                    continue;
                }
                double[] points = bucket.coords;
                for (int i = 0; i < bucket.size; i += 3) {
                    double dx = points[i] - viewer.x(), dy = points[i + 1] - viewer.y(), dz = points[i + 2] - viewer.z();
                    double distanceSquared = dx * dx + dy * dy + dz * dz;
                    if (distanceSquared > rangeSquared) {
                        continue;
                    }
                    if (count == distances.length) {
                        distances = Arrays.copyOf(distances, count * 2);
                        coords = Arrays.copyOf(coords, count * 6);
                    }
                    distances[count] = (float) distanceSquared;
                    System.arraycopy(points, i, coords, count * 3, 3);
                    count++;
                }
            }
            if (count <= maxParticles) {
                return Arrays.copyOf(coords, count * 3);
            }
            // 非负 float 的位模式与其数值同序，把距离放在高 32 位、序号放在低 32 位，排一次 long 数组即得由近及远的次序。
            long[] order = new long[count];
            for (int i = 0; i < count; i++) {
                order[i] = ((long) Float.floatToRawIntBits(distances[i]) << 32) | i;
            }
            Arrays.sort(order);
            double[] result = new double[maxParticles * 3];
            for (int i = 0; i < maxParticles; i++) {
                System.arraycopy(coords, (int) order[i] * 3, result, i * 3, 3);
            }
            return result;
        }

        public void sendSlice(int slice) {
            if (display.removed) {
                return;
            }
            try {
                for (int v = 0; v < viewers.size(); v++) {
                    Player player = viewers.get(v).player();
                    // 分摊期间下线或换了世界的观看者不再发送，免得粒子出现在另一个世界的同一坐标上。
                    if (!player.isOnline() || player.getWorld() != world) {
                        continue;
                    }
                    double[] points = selections.get(v);
                    for (int i = slice * 3; i < points.length; i += slices * 3) {
                        player.spawnParticle(particle, points[i], points[i + 1], points[i + 2], 1, 0, 0, 0, 0, particleData, force);
                    }
                }
            }
            catch (Throwable ex) {
                reportError(ex);
            }
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

        public final double gridDensity;

        public final Map<Long, PointBucket> buckets = new HashMap<>();

        public int count = 0;

        public boolean overflow = false;

        public PointCollector(int density) {
            this.density = density;
            this.gridDensity = Math.min(density, MAX_GRID_DENSITY);
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

        public int stepsFor(double length, boolean grid) {
            return Math.max(1, (int) Math.ceil(length * (grid ? gridDensity : density)));
        }

        /** 画一条线段，含两端点。 */
        public void line(double x1, double y1, double z1, double x2, double y2, double z2) {
            lineInternal(x1, y1, z1, x2, y2, z2, true);
        }

        /** 画一条网格线，不含两端点，其两端已落在轮廓上。 */
        public void lineInterior(double x1, double y1, double z1, double x2, double y2, double z2) {
            lineInternal(x1, y1, z1, x2, y2, z2, false);
        }

        public void lineInternal(double x1, double y1, double z1, double x2, double y2, double z2, boolean withEnds) {
            double dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
            int steps = stepsFor(Math.sqrt(dx * dx + dy * dy + dz * dz), !withEnds);
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
                               double from, double to, boolean withEnds, boolean grid) {
            double a = Math.sqrt(ax * ax + ay * ay + az * az), b = Math.sqrt(bx * bx + by * by + bz * bz);
            double fullLength = Math.PI * (3 * (a + b) - Math.sqrt((3 * a + b) * (a + 3 * b)));
            double span = to - from;
            boolean closed = span >= Math.PI * 2 - 1e-9;
            int steps = Math.max(closed ? 8 : 2, stepsFor(fullLength * span / (Math.PI * 2), grid));
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

    /**
     * 按所在表面的面积定出网格间距（格），面积越大间距越宽，封顶 MAX_GRID_SPACING。
     * 网格线的条数因此大致与表面边长成正比，而非与面积成正比，大区域的粒子数不会随之暴涨。
     */
    public static int gridSpacing(double area) {
        return Math.min((int) (area / GRID_AREA_PER_SPACING) + 1, MAX_GRID_SPACING);
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
            // 每对相对的面各按其面积定出间距，从低角起每隔该间距画一条横线与竖线，线的两端落在棱上，故不含端点。
            double width = x2 - x1, height = y2 - y1, length = z2 - z1;
            int spacingXY = gridSpacing(width * height), spacingZY = gridSpacing(length * height), spacingXZ = gridSpacing(width * length);
            for (double z : new double[] {z1, z2}) {
                for (double x = x1 + spacingXY; x < x2; x += spacingXY) {
                    collector.lineInterior(x, y1, z, x, y2, z);
                }
                for (double y = y1 + spacingXY; y < y2; y += spacingXY) {
                    collector.lineInterior(x1, y, z, x2, y, z);
                }
            }
            for (double x : new double[] {x1, x2}) {
                for (double z = z1 + spacingZY; z < z2; z += spacingZY) {
                    collector.lineInterior(x, y1, z, x, y2, z);
                }
                for (double y = y1 + spacingZY; y < y2; y += spacingZY) {
                    collector.lineInterior(x, y, z1, x, y, z2);
                }
            }
            for (double y : new double[] {y1, y2}) {
                for (double x = x1 + spacingXZ; x < x2; x += spacingXZ) {
                    collector.lineInterior(x, y, z1, x, y, z2);
                }
                for (double z = z1 + spacingXZ; z < z2; z += spacingXZ) {
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
        // 侧面：各面共用同一间距，使横线绕多边形一周首尾相接；间距取最宽的那一面来定。
        double longestEdge = 0, doubleArea = 0;
        for (int i = 0; i < size; i++) {
            PolygonTag.Corner start = corners.get(i), end = corners.get((i + 1) % size);
            longestEdge = Math.max(longestEdge, Math.hypot(end.x - start.x, end.z - start.z));
            doubleArea += start.x * end.z - end.x * start.z;
        }
        int wallSpacing = gridSpacing(longestEdge * (y2 - y1));
        for (int i = 0; i < size; i++) {
            PolygonTag.Corner start = corners.get(i), end = corners.get((i + 1) % size);
            double dx = end.x - start.x, dz = end.z - start.z;
            double length = Math.sqrt(dx * dx + dz * dz);
            if (y2 > y1) {
                for (double d = wallSpacing; d < length; d += wallSpacing) {
                    double t = d / length;
                    collector.lineInterior(start.x + dx * t, y1, start.z + dz * t, start.x + dx * t, y2, start.z + dz * t);
                }
            }
            for (double y = y1 + wallSpacing; y < y2; y += wallSpacing) {
                collector.lineInterior(start.x, y, start.z, end.x, y, end.z);
            }
        }
        // 顶面与底面：按多边形面积定出间距，以相应的 x 与 z 坐标切割多边形，按奇偶规则两两配对交点，画出落在多边形内的线段。
        int topSpacing = gridSpacing(Math.abs(doubleArea) / 2);
        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, minZ = Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
        for (PolygonTag.Corner corner : corners) {
            minX = Math.min(minX, corner.x);
            maxX = Math.max(maxX, corner.x);
            minZ = Math.min(minZ, corner.z);
            maxZ = Math.max(maxZ, corner.z);
        }
        double[] faces = y2 > y1 ? new double[] {y1, y2} : new double[] {y1};
        for (double x = Math.floor(minX) + topSpacing; x < maxX; x += topSpacing) {
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
        for (double z = Math.floor(minZ) + topSpacing; z < maxZ; z += topSpacing) {
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
        collector.ellipseArc(cx, cy, cz, rx, 0, 0, 0, 0, rz, 0, full, true, false);
        collector.ellipseArc(cx, cy, cz, rx, 0, 0, 0, ry, 0, 0, full, true, false);
        collector.ellipseArc(cx, cy, cz, 0, 0, rz, 0, ry, 0, 0, full, true, false);
        if (!grid) {
            return;
        }
        // 间距按外接长方体最大的一面来定，纬线与经线共用。
        int spacing = gridSpacing(4 * Math.max(rx * ry, Math.max(rz * ry, rx * rz)));
        // 纬线：自赤道起上下每隔该间距一圈水平椭圆，赤道已画过，跳过。
        for (double offset = spacing; offset < ry; offset += spacing) {
            double ratio = offset / ry;
            double scale = Math.sqrt(1 - ratio * ratio);
            collector.ellipseArc(cx, cy + offset, cz, rx * scale, 0, 0, 0, 0, rz * scale, 0, full, true, true);
            collector.ellipseArc(cx, cy - offset, cz, rx * scale, 0, 0, 0, 0, rz * scale, 0, full, true, true);
        }
        // 经线：沿赤道每隔约该间距一条，自顶至底画半圈，两极点已落在轮廓上，故不含端点。
        double a = rx, b = rz;
        double equator = Math.PI * (3 * (a + b) - Math.sqrt((3 * a + b) * (a + 3 * b)));
        int meridians = Math.max(4, (int) Math.ceil(equator / spacing));
        for (int i = 0; i < meridians && !collector.overflow; i++) {
            double phi = full * i / meridians;
            double cos = Math.cos(phi), sin = Math.sin(phi);
            collector.ellipseArc(cx, cy, cz, 0, ry, 0, rx * cos, 0, rz * sin, 0, Math.PI, false, true);
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

    /**
     * 插件停用时调用：清空所有显示并立即关闭后台线程。
     * <p>
     * Bukkit 停用插件时只会取消主线程上的绘制任务，后台线程不受其管，
     * 若不在此关闭，服务器停止期间仍会向正在断开的连接发包，热重载插件后还会留下一条空转的线程。
     */
    public static void shutdown() {
        for (AreaDisplay display : displays.values()) {
            display.removed = true;
        }
        displays.clear();
        if (drawTask != null) {
            drawTask.cancel();
            drawTask = null;
        }
        if (sender != null) {
            sender.shutdownNow();
            sender = null;
        }
    }

    public static void tickDisplays() {
        currentTick++;
        Iterator<AreaDisplay> iterator = displays.values().iterator();
        while (iterator.hasNext()) {
            AreaDisplay display = iterator.next();
            if (display.expireTick >= 0 && currentTick >= display.expireTick) {
                display.removed = true;
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
                    display.removed = true;
                    iterator.remove();
                }
            }
        }
        if (displays.isEmpty() && drawTask != null) {
            drawTask.cancel();
            drawTask = null;
            if (sender != null) {
                sender.shutdown();
                sender = null;
            }
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
                                   @ArgName("max_particles") @ArgPrefixed @ArgDefaultNull ElementTag maxParticlesInput,
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
        Particle particle = existing == null ? Particle.FLAME : existing.particle;
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
        int maxParticles = existing == null ? DEFAULT_MAX_PARTICLES : existing.maxParticles;
        if (maxParticlesInput != null) {
            if (!maxParticlesInput.isInt() || maxParticlesInput.asInt() < 1) {
                throw new InvalidArgumentsRuntimeException("Max particles must be a whole number above 0.");
            }
            maxParticles = maxParticlesInput.asInt();
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
        display.maxParticles = maxParticles;
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
            display.removed = true;
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
