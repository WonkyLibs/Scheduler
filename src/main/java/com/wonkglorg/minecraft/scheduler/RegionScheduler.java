package com.wonkglorg.minecraft.scheduler;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Handles region execution, folia compatible
 */
@SuppressWarnings("unused")
public class RegionScheduler{//NOSONAR
	/**
	 * All Region schedulers and their registered namespace
	 */
	private static final Map<String, RegionScheduler> SCHEDULER_MAP = new ConcurrentHashMap<>();
	
	private final JavaPlugin plugin;
	
	private final ScheduledExecutorService asyncExecutor;
	
	private RegionScheduler(JavaPlugin plugin, int threadCount) {
		this.plugin = plugin;
		this.asyncExecutor = Executors.newScheduledThreadPool(threadCount, r -> {
			Thread t = new Thread(r);
			t.setDaemon(true);
			return t;
		});
	}
	
	/**
	 * Retrieves the existing instance or creates a new instance of the GuiManager for this plugin
	 *
	 * @param plugin the plugin to create the instance for
	 * @param asyncThreadCount how many async threads to register
	 * @return the created instance
	 */
	public static RegionScheduler getInstance(JavaPlugin plugin, int asyncThreadCount) {
		if(!SCHEDULER_MAP.containsKey(plugin.namespace())){
			SCHEDULER_MAP.put(plugin.namespace(), new RegionScheduler(plugin, asyncThreadCount));
		}
		return SCHEDULER_MAP.get(plugin.namespace());
	}
	
	/**
	 * Retrieves the existing instance or creates a new instance of the GuiManager for this plugin
	 *
	 * @param plugin the plugin to create the instance for
	 * @return the created instance
	 */
	public static RegionScheduler getInstance(JavaPlugin plugin) {
		return getInstance(plugin, Math.max(2, Runtime.getRuntime().availableProcessors() / 2));
	}
	
	/**
	 * Runs a task on the next tick on the global region scheduler
	 *
	 * @param task the task to run
	 */
	public void runNextTick(Runnable task) {
		plugin.getServer().getGlobalRegionScheduler().run(plugin, s -> task.run());
	}
	
	/**
	 * Runs a task on the next tick on a local region scheduler
	 *
	 * @param location the location of the region scheduler
	 * @param task the task to run
	 */
	public void runNextTick(Location location, Runnable task) {
		plugin.getServer().getRegionScheduler().run(plugin, location, s -> task.run());
	}
	
	/**
	 * Runs a task on the next tick available on the global region scheduler
	 *
	 * @param task the task to run
	 */
	public void run(Runnable task) {
		plugin.getServer().getGlobalRegionScheduler().execute(plugin, task);
	}
	
	/**
	 * Runs a task on the next tick available on the local region scheduler
	 *
	 * @param location the location of the region scheduler
	 * @param task the task to run
	 */
	public void runAtLocation(Location location, Runnable task) {
		plugin.getServer().getRegionScheduler().execute(plugin, location, task);
	}
	
	/**
	 * Run the task at the location at the next possible server tick after the elapsed time
	 *
	 * @param location the location to run it at
	 * @param task the task to run
	 * @param delay the delay
	 * @param unit the timeunit
	 */
	public void runAtLocationLater(Location location, Runnable task, long delay, TimeUnit unit) {
		long delayMillis = unit.toMillis(delay);
		long ticks = Math.max(1, delayMillis / 50);
		
		plugin.getServer().getRegionScheduler().runDelayed(plugin, location, scheduledTask -> task.run(), ticks);
	}
	
	/**
	 * Runs a task on the next tick available on the local region scheduler
	 *
	 * @param chunk the chunk whose region scheduler should execute the task
	 * @param task the task to run
	 */
	public void runAtChunk(Chunk chunk, Runnable task) {
		plugin.getServer().getRegionScheduler().execute(plugin, chunk.getWorld(), chunk.getX(), chunk.getZ(), task);
	}
	
	/**
	 * Run task as entity
	 */
	public void runAtEntity(Entity entity, Runnable task) {
		entity.getScheduler().execute(plugin, task, null, 1);
	}
	
	/**
	 * Run the task at the entities location at the next possible server tick after the elapsed time
	 *
	 * @param entity the entity to run it at
	 * @param task the task to run
	 * @param delay the delay
	 * @param unit the timeunit
	 */
	public void runAtEntityLater(Entity entity, Runnable task, long delay, TimeUnit unit) {
		long delayMillis = unit.toMillis(delay);
		long ticks = Math.max(1, delayMillis / 50);
		
		entity.getScheduler().runDelayed(plugin, scheduledTask -> task.run(), null, ticks);
	}
	
	/**
	 * Runs async task (this should not interact with any minecraft api!)
	 */
	public <T> CompletableFuture<T> runAsync(Supplier<T> task) {
		return CompletableFuture.supplyAsync(task, asyncExecutor);
	}
	
	/**
	 * Shutdown on plugin disable
	 */
	public void shutdown() {
		asyncExecutor.shutdownNow();
	}
	
	/**
	 * Repeatedly runs a task async
	 *
	 * @param task the task to run
	 * @param delay the delay before starting
	 * @param period the delay between reruns
	 * @param timeUnit the timeunit
	 * @return the schedules tasks future
	 */
	public ScheduledFuture<?> runTaskTimerAsync(Runnable task, long delay, long period, TimeUnit timeUnit) {
		return asyncExecutor.scheduleAtFixedRate(task, delay, period, timeUnit);
	}
	
	/**
	 * Runs a task on a tick basis
	 *
	 * @param task the task to run
	 * @param delayTicks the delay in ticks
	 * @param periodTicks the ticks to wait between runs
	 * @return a BukkitTask that contains the id number
	 */
	@NotNull
	public ScheduledTask runTaskTimer(@NotNull Consumer<ScheduledTask> task, long delayTicks, long periodTicks) {
		return plugin.getServer().getGlobalRegionScheduler().runAtFixedRate(plugin, task, delayTicks, periodTicks);
	}
	
	/**
	 * Runs a task on a tick basis
	 *
	 * @param task the task to run
	 * @param delayTicks the delay in ticks
	 * @param periodTicks the ticks to wait between runs
	 * @return a BukkitTask that contains the id number
	 */
	@NotNull
	public ScheduledTask runTaskTimer(@NotNull Runnable task, long delayTicks, long periodTicks) {
		return runTaskTimer(t -> task.run(), delayTicks, periodTicks);
	}
	
	/**
	 * Runs a task on a tick basis
	 *
	 * @param task the task to run
	 * @param delay the delay in ticks
	 * @param period the ticks to wait between runs
	 * @return a BukkitTask that contains the id number
	 */
	@NotNull
	public ScheduledTask runTaskTimer(@NotNull Consumer<ScheduledTask> task, long delay, long period, TimeUnit timeUnit) {
		long delayTicks = Math.max(0, timeUnit.toMillis(delay) / 50);
		long periodTicks = Math.max(1, timeUnit.toMillis(period) / 50);
		
		return plugin.getServer().getGlobalRegionScheduler().runAtFixedRate(plugin, task, delayTicks, periodTicks);
	}
	
	/**
	 * Runs a task on a tick basis
	 *
	 * @param task the task to run
	 * @param delay the delay in ticks
	 * @param period the ticks to wait between runs
	 * @return a BukkitTask that contains the id number
	 */
	@NotNull
	public ScheduledTask runTaskTimer(@NotNull Runnable task, long delay, long period, TimeUnit timeUnit) {
		return runTaskTimer(t -> task.run(), delay, period, timeUnit);
	}
	
	/**
	 * Runs a command globally
	 *
	 * @param command the command to run
	 */
	public void runCommand(String command) {
		plugin.getServer().getGlobalRegionScheduler().execute(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command));
	}
	
}