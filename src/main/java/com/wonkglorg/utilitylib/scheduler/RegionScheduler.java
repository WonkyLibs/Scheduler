package com.wonkglorg.utilitylib.scheduler;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
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
	private static RegionScheduler instance;
	private static JavaPlugin plugin;
	
	private final ScheduledExecutorService asyncExecutor;
	
	private RegionScheduler(String threadName) {
		this.asyncExecutor = Executors.newScheduledThreadPool(Math.max(2, Runtime.getRuntime().availableProcessors() / 2), r -> {
			Thread t = new Thread(r, threadName);
			t.setDaemon(true);
			return t;
		});
	}
	
	/**
	 * Creates a global Async manager instance
	 *
	 * @param plugin the owning plugin
	 * @return the created instance, the same instance can be retrieved using {@link RegionScheduler#getInstance()}
	 */
	public static RegionScheduler createInstance(JavaPlugin plugin, String threadName) {
		RegionScheduler.plugin = plugin;
		instance = new RegionScheduler(threadName);
		return instance;
	}
	
	/**
	 * Creates a global Async manager instance
	 *
	 * @param plugin the owning plugin
	 * @return the created instance, the same instance can be retrieved using {@link RegionScheduler#getInstance()}
	 */
	public static RegionScheduler createInstance(JavaPlugin plugin) {
		return createInstance(plugin, "async-thread");
	}
	
	/**
	 * @return the instance created using {@link #createInstance(JavaPlugin, String)}
	 * @throws IllegalStateException when the instance has not been properly initialised
	 */
	public static RegionScheduler getInstance() {
		if(instance == null){
			throw new NullPointerException("AsyncManager instance not initialized!");
		}
		return instance;
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
	
}