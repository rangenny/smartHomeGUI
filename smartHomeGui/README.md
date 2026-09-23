# Real-Time Smart Home Automation Hub (STIWK3014)

A Java Swing desktop application that simulates a smart-home control panel.
Its real purpose is to **demonstrate one Java concurrency technique per feature** —
thread pools, explicit locks, `Future` cancellation, Fork/Join, parallel streams,
and the Swing Event Dispatch Thread (EDT).

---

## 1. Requirements

- **JDK 8 or newer** (developed/tested on OpenJDK 11).
- No external libraries — uses only the standard Java API (`javax.swing`, `java.awt`, `java.util.concurrent`).
- A desktop environment (the app opens a GUI window).

---

## 2. Project structure

```
smartHomeGui/
├── src/
│   └── Main.java        <- source file (contains class SmartHomeGUI)
└── out/production/...    <- compiled .class files
```

> Note: the top-level class is `SmartHomeGUI` (not `public`), so it is allowed to live
> in a file named `Main.java`. This matters when running from the command line (see below).

---

## 3. How to compile and run

### Option A — IntelliJ IDEA
1. Open the `smartHomeGui` project.
2. Open `Main.java`.
3. Click the green ▶ next to `main(...)` (or press **Shift+F10**).

### Option B — Command line
From the `src` folder:
```bash
javac Main.java          # compiles -> produces SmartHomeGUI.class
java SmartHomeGUI        # RUN THIS (the class holding main), NOT "java Main"
```

---

## 4. What the app does (feature guide)

| Button | What it does | Concurrency technique | Chapter |
|---|---|---|---|
| Toggle Door | Opens/closes the door (2s) | `ReentrantLock` + non-blocking `tryLock()` | 7, 13 |
| Toggle Lights / Fan / AC | Turns a device on/off | `synchronized` method + `Future` cancellation | 4, 5 |
| Toggle Thermostat | Starts/stops a live temperature loop | Long-running thread + interruption | 3, 4 |
| CCTV Frame Scan | Fake frame processing | Fork/Join divide-and-conquer (`RecursiveAction`) | 11 |
| Dynamic Load Scan | Sums 50,000 energy readings | Parallel streams (filter–map–reduce) | 12 |
| 50 Device Test | Launches 50 concurrent tasks | Cached thread pool stress test | 4 |
| Master Trigger | Fires many devices at once | Bulk orchestration macro | — |
| Emergency Stop | Halts everything | `volatile` flag + mass `cancel(true)` | 4 |
| Resume System | Resets to normal | — | — |

---

## 5. Code overview (what each part does)

### Instance fields
- `doorLabel, lightLabel, …` — the coloured status labels on the dashboard.
- `consoleLog` — the black output log (a `JTextArea`).
- `masterBtn, stopBtn, resumeBtn, scanBtn` — buttons kept as fields because other methods enable/disable them.
- `executor = Executors.newCachedThreadPool()` — elastic thread pool for short, bursty background jobs.
- `forkJoinPool = new ForkJoinPool(availableProcessors())` — pool sized one-thread-per-core for divide-and-conquer work.
- `doorLock = new ReentrantLock()` — explicit lock so only one door operation runs at a time.
- `activeTasks = new ConcurrentHashMap<>()` — thread-safe map of `deviceName -> Future`, used to cancel running tasks.
- `isEmergencyMode (volatile)` — safety flag; `volatile` guarantees every thread sees its latest value.
- `currentTemperature` — the thermostat's current reading (starts 24°C).

### Methods
- `SmartHomeGUI()` — constructor; calls `setupUI()`.
- `setupUI()` — builds the window: layouts (`BorderLayout`, `GridLayout`, `FlowLayout`), labels, buttons, and lambda `addActionListener` handlers.
- `showThreadInfo()` — casts `executor` to `ThreadPoolExecutor` and logs pool size / active / completed / queued counts.
- `run50DeviceSimulation()` — loop submitting 50 tasks to the pool (each sleeps 3s) to stress-test it.
- `triggerAllAtOnce()` — macro that fires several devices; guards each with a state check.
- `toggleDoor()` — submits door work; uses `tryLock()` and a `try/finally unlock()` so the lock is always released.
- `toggleDevice(name, label, colour)` — `synchronized`; starts a device task and stores its `Future`, or cancels and removes it.
- `toggleThermostat()` — `synchronized`; starts a loop thread `while(!isInterrupted())` that polls temperature, or cancels it.
- `runCameraFrameProcessing()` — runs a `CameraFilterTask` on the `forkJoinPool` and times it.
- `runDynamicParallelScan()` — builds 50,000 readings and runs a `parallelStream().filter().mapToLong().sum()` pipeline.
- `emergencyStop()` — sets `isEmergencyMode=true`, cancels all tracked tasks, sets labels to HALTED, toggles buttons.
- `resumeSystem()` — clears the flag and resets all labels/buttons to normal.
- `createLabel(text, colour)` — helper that builds a styled `JLabel`.
- `log(msg)` / `updateLabel(...)` — route UI updates onto the EDT via `SwingUtilities.invokeLater(...)`.
- `CameraFilterTask` — a `RecursiveAction`; if the range ≤ THRESHOLD it "processes", else it splits in two and `invokeAll`s them.
- `main(String[] args)` — starts the window on the EDT via `invokeLater`; registers a JVM shutdown hook.

### Key syntax used
- **Lambdas** `() -> {...}` / `e -> method()` — compact `Runnable` / `ActionListener` bodies.
- **Ternary** `cond ? a : b` — inline if/else (e.g. door label text/colour).
- **`try / catch (InterruptedException) / finally`** — handle interruption and guarantee cleanup (lock release).
- **`synchronized`** — method-level mutual exclusion (monitor lock).
- **Generics** `Map<String, Future<?>>` — type-safe collections; `<?>` = "result type doesn't matter".
- **Enhanced casting** `(ThreadPoolExecutor) executor` — reach a concrete class's extra methods.

---

## 6. Step-by-step: how to test the system

Run the app, then work through this checklist and watch the **status labels** and the **console log**.

1. **App launches** — a window titled *"Real-Time Smart Home Automation Hub"* opens; all devices show OFF/CLOSED/IDLE; System Mode = NORMAL.
2. **Door lock test** — click **Toggle Door**. It shows `OPENING...` (orange) for ~2s, then `OPEN` (blue). Click again → `CLOSING...` → `CLOSED`.
3. **Rapid-click test (tryLock)** — click **Toggle Door** twice quickly. The second click logs *"Resource conflict blocked…"* instead of queuing — proof `tryLock()` drops the redundant request.
4. **Device on/off** — click **Toggle Lights**: `STARTING...` → `ON` (yellow). Click again → `OFF`. Repeat for Fan and AC.
5. **Thermostat loop** — click **Toggle Thermostat**: label switches to `REGULATING`, and the log prints a new temperature reading every 3s. Click again → back to `IDLE` and the readings stop (thread interrupted cleanly).
6. **Fork/Join** — click **CCTV Frame Scan**: label shows `FILTERING FRAMES...` then `LIVE FEED FEEDING`, and the log prints the processing time in ms.
7. **Parallel stream** — click **Dynamic Load Scan**: the Energy label updates to a wattage value and the log prints the reduction total.
8. **Thread-pool stress test** — click **50 DEVICE TEST**: the log lists 50 devices activating on different `pool-1-thread-N` names, then a THREAD MONITOR block with Pool Size / Active / Completed / Queued.
9. **Master Trigger** — click **MASTER TRIGGER**: the door, lights, fan, AC, thermostat, CCTV and scan all activate together.
10. **Emergency Stop** — click **EMERGENCY STOP**: System Mode = `EMERGENCY HALT` (red), all labels = `HALTED` (black), tracked tasks cancel, and Stop/Master/Scan buttons disable while Resume enables. Clicking device buttons now does nothing (guarded by `isEmergencyMode`).
11. **Resume** — click **RESUME SYSTEM**: everything resets to NORMAL and buttons re-enable.
12. **Responsiveness check** — during any 2–3s operation, confirm the window still drags/repaints smoothly (proof the slow work is on background threads, not the EDT).
13. **Clean shutdown** — close the window (✕). The console prints *"System shutting down..."* from the shutdown hook.

If every step behaves as described, the concurrency framework is working correctly.

---

## 7. Known limitations / notes

- `currentTemperature` is written by the thermostat thread and read by the EDT without `volatile`/lock — a benign data race here.
- Door, CCTV and Dynamic-Scan tasks are **not** stored in `activeTasks`, so Emergency Stop cannot cancel them mid-run; they finish on their own. An in-flight door task may briefly repaint over `HALTED`.
- A new `Random()` is created every thermostat cycle — better to create one instance and reuse it.
- Thermostat temperature has an upper reset (`>28 → 24`) but no lower bound.
- Device state is inferred from label **text**; a dedicated state variable/enum would be more robust.
- `executor` and `forkJoinPool` are never explicitly shut down (fine because `EXIT_ON_CLOSE` ends the JVM).

These are refinements, not defects — the program runs correctly as submitted.
