import javax.swing.*;
import java.awt.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import java.util.stream.*;
import java.util.*;
import java.util.List;

class SmartHomeGUI extends JFrame {

    // UI Status Labels
    private JLabel doorLabel, lightLabel, fanLabel, acLabel, energyLabel, systemStatus;
    private JLabel thermoLabel, cameraLabel;
    private JTextArea consoleLog;
    private JButton masterBtn, stopBtn, resumeBtn, scanBtn;

    // --- CRITICAL CONCURRENCY FRAMEWORK ENGINES (Chapters 4, 7 & 11) ---
    // Chapter 4: Cached Thread Pool managing elastic runtime resource allocation and automatic thread recycling
    private final ExecutorService executor = Executors.newCachedThreadPool();

    // Chapter 11: Specialized ForkJoinPool engine handling deep divide-and-conquer parallel processing jobs
    private final ForkJoinPool forkJoinPool =
            new ForkJoinPool(Runtime.getRuntime().availableProcessors());
    // Chapter 7: Explicit Mutual Exclusion Lock providing non-blocking entry isolation for the main door
    private final Lock doorLock = new ReentrantLock();

    // Chapters 4 & 8: High-throughput concurrent map tracking active task tokens to enable targeted thread cancellations
    private final Map<String, Future<?>> activeTasks = new ConcurrentHashMap<>();

    // Chapter 4: Volatile safety flag providing reliable cross-thread state visibility to prevent new tasks during an emergency
    private volatile boolean isEmergencyMode = false;
    private int currentTemperature = 24;

    public SmartHomeGUI() {
        setupUI();
    }

    private void setupUI() {
        setTitle("Real-Time Smart Home Automation Hub (STIWK3014)");
        setSize(1200, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        JPanel statusPanel = new JPanel(new GridLayout(2, 8, 10, 10));
        statusPanel.setBorder(BorderFactory.createTitledBorder("Live System Monitor Matrix"));

        statusPanel.add(new JLabel(" Main Door:")); doorLabel = createLabel("CLOSED", Color.RED); statusPanel.add(doorLabel);
        statusPanel.add(new JLabel(" Lights:")); lightLabel = createLabel("OFF", Color.GRAY); statusPanel.add(lightLabel);
        statusPanel.add(new JLabel(" Fan:")); fanLabel = createLabel("OFF", Color.GRAY); statusPanel.add(fanLabel);
        statusPanel.add(new JLabel(" Air-Cond:")); acLabel = createLabel("OFF", Color.GRAY); statusPanel.add(acLabel);
        statusPanel.add(new JLabel(" Energy:")); energyLabel = createLabel("0W", Color.BLUE); statusPanel.add(energyLabel);
        statusPanel.add(new JLabel(" Thermostat:")); thermoLabel = createLabel("24°C (IDLE)", Color.BLUE); statusPanel.add(thermoLabel);
        statusPanel.add(new JLabel(" CCTV Cam:")); cameraLabel = createLabel("IDLE", Color.GRAY); statusPanel.add(cameraLabel);
        statusPanel.add(new JLabel(" System Mode:")); systemStatus = createLabel("NORMAL", new Color(0, 150, 0)); statusPanel.add(systemStatus);

        add(statusPanel, BorderLayout.NORTH);

        consoleLog = new JTextArea();
        consoleLog.setEditable(false);
        consoleLog.setBackground(Color.BLACK);
        consoleLog.setForeground(new Color(50, 255, 50));
        add(new JScrollPane(consoleLog), BorderLayout.CENTER);

        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));

        JButton doorBtn = new JButton("Toggle Door");
        doorBtn.addActionListener(e -> toggleDoor());

        JButton lightBtn = new JButton("Toggle Lights");
        lightBtn.addActionListener(e -> toggleDevice("Lights", lightLabel, Color.BLUE));

        JButton fanBtn = new JButton("Toggle Fan");
        fanBtn.addActionListener(e -> toggleDevice("Fan", fanLabel, new Color(0, 150, 0)));

        JButton acBtn = new JButton("Toggle AC");
        acBtn.addActionListener(e -> toggleDevice("AirCond", acLabel, Color.BLUE));

        JButton thermoBtn = new JButton("Toggle Thermostat");
        thermoBtn.addActionListener(e -> toggleThermostat());

        JButton camBtn = new JButton("CCTV Frame Scan");
        camBtn.addActionListener(e -> runCameraFrameProcessing());

        JButton stressBtn = new JButton("50 DEVICE TEST");
        stressBtn.addActionListener(e -> run50DeviceSimulation());
        controlPanel.add(stressBtn);

        masterBtn = new JButton("MASTER TRIGGER");
        masterBtn.setBackground(new Color(100, 100, 255)); masterBtn.setForeground(Color.WHITE);
        masterBtn.addActionListener(e -> triggerAllAtOnce());

        scanBtn = new JButton("Dynamic Load Scan");
        scanBtn.addActionListener(e -> runDynamicParallelScan());

        stopBtn = new JButton("EMERGENCY STOP");
        stopBtn.setBackground(Color.RED); stopBtn.setForeground(Color.WHITE);
        stopBtn.addActionListener(e -> emergencyStop());

        resumeBtn = new JButton("RESUME SYSTEM");
        resumeBtn.setBackground(new Color(0, 120, 0)); resumeBtn.setForeground(Color.WHITE);
        resumeBtn.setEnabled(false);
        resumeBtn.addActionListener(e -> resumeSystem());

        controlPanel.add(doorBtn); controlPanel.add(lightBtn); controlPanel.add(fanBtn);
        controlPanel.add(acBtn); controlPanel.add(thermoBtn); controlPanel.add(camBtn);
        controlPanel.add(masterBtn); controlPanel.add(scanBtn); controlPanel.add(stopBtn);
        controlPanel.add(resumeBtn);

        add(controlPanel, BorderLayout.SOUTH);
    }

    private void showThreadInfo() {
        ThreadPoolExecutor pool = (ThreadPoolExecutor) executor;

        log("========== THREAD MONITOR ==========");
        log("Pool Size: " + pool.getPoolSize());
        log("Active Threads: " + pool.getActiveCount());
        log("Completed Tasks: " + pool.getCompletedTaskCount());
        log("Queued Tasks: " + pool.getQueue().size());
    }

    private void run50DeviceSimulation() {
        log("[TEST] Starting 50 concurrent smart devices...");
        for (int i = 1; i <= 50; i++) {
            final int deviceId = i;
            executor.submit(() -> {
                try {
                    log("[Device-" + deviceId + "] Activated by " + Thread.currentThread().getName());
                    Thread.sleep(3000);
                    log("[Device-" + deviceId + "] Completed.");
                } catch (InterruptedException e) {
                    log("[Device-" + deviceId + "] Interrupted.");
                }
            });
        }
        showThreadInfo();
    }

    // --- AUTOMATION MACROS: BULK TASK ORCHESTRATION ---
    private void triggerAllAtOnce() {
        if (isEmergencyMode) return;
        log("[Master] Executing bulk concurrent device workflows...");
        toggleDoor();
        if(lightLabel.getText().equals("OFF")) toggleDevice("Lights", lightLabel, Color.YELLOW);
        if(fanLabel.getText().equals("OFF")) toggleDevice("Fan", fanLabel, new Color(0, 150, 0));
        if(acLabel.getText().equals("OFF")) toggleDevice("AirCond", acLabel, Color.CYAN);
        if(thermoLabel.getText().contains("IDLE")) toggleThermostat();
        runCameraFrameProcessing();
        runDynamicParallelScan();
    }

    // --- MAIN ENTRY PORTAL: LOCK ARCHITECTURE (Chapter 7) ---
    private void toggleDoor() {
        if (isEmergencyMode) return;
        executor.submit(() -> {
            // Chapter 13: Execute non-blocking tryLock checks to prevent GUI freeze and guarantee absolute resource isolation
            if (doorLock.tryLock()) {
                try {
                    boolean isClosed = doorLabel.getText().equals("CLOSED") || doorLabel.getText().equals("HALTED");
                    updateLabel(doorLabel, isClosed ? "OPENING..." : "CLOSING...", Color.ORANGE);

                    // Chapter 3: Simulate physical hardware delays by placing the background thread into timed suspension
                    Thread.sleep(2000);

                    updateLabel(doorLabel, isClosed ? "OPEN" : "CLOSED", isClosed ? Color.BLUE : Color.RED);
                    log("[Door] Safe structural access transition verified.");
                } catch (InterruptedException e) {
                    // Chapter 4: Catch immediate thread interruption signals to exit clean on emergency shutdown
                    log("[Door] Structural thread block aborted safely by InterruptedException.");
                } finally {
                    // Chapter 7: Guarantee lock release execution inside final block to prevent permanent deadlocks
                    doorLock.unlock();
                }
            } else {
                log("[Door] Resource conflict blocked execution request safely.");
            }
        });
    }

    // --- APPLIANCES: MONITOR-SYNCHRONIZED CRITICAL SECTION PATTERN (Chapter 5) ---
    // Chapter 5: Method synchronization protects device maps and labels from thread race conditions
    private synchronized void toggleDevice(String name, JLabel label, Color activeColor) {
        if (isEmergencyMode) return;
        String state = label.getText();
        if (state.equals("OFF") || state.equals("HALTED")) {
            // Chapter 4: Future representation token stores the reference to the running background thread assignment
            Future<?> task = executor.submit(() -> {
                try {
                    updateLabel(label, "STARTING...", Color.ORANGE);
                    Thread.sleep(1500);
                    updateLabel(label, "ON", activeColor);
                    log("[Device] " + name + " operational execution started.");
                } catch (InterruptedException e) {
                    updateLabel(label, "OFF", Color.GRAY);
                }
            });
            activeTasks.put(name, task);
        } else {
            Future<?> task = activeTasks.remove(name);
            // Chapter 4: Issue explicit cancellations using targeted thread interruptions via Future reference
            if (task != null) task.cancel(true);
            updateLabel(label, "OFF", Color.GRAY);
            log("[Device] " + name + " shutdown verified.");
        }
    }

    // --- CLIMATE MANAGEMENT: INDEPENDENT RUNNABLE LOOP SEPARATIONS (Chapter 3 & 5) ---
    private synchronized void toggleThermostat() {
        if (isEmergencyMode) return;
        if (activeTasks.containsKey("Thermostat")) {
            Future<?> task = activeTasks.remove("Thermostat");
            if (task != null) task.cancel(true);
            updateLabel(thermoLabel, currentTemperature + "°C (IDLE)", Color.BLUE);
            log("[Thermostat] Processing run-loop decoupled.");
        } else {
            Future<?> task = executor.submit(() -> {
                try {
                    // Chapter 4: Continuously monitor thread interruption states as loop criteria to prevent thread leaks
                    while (!Thread.currentThread().isInterrupted()) {
                        updateLabel(thermoLabel, currentTemperature + "°C (REGULATING)", new Color(255, 140, 0));
                        log("[Thermostat] Live environment temperature poll: reading " + currentTemperature + "°C");

                        // Chapter 3: Explicit timed checking sequence delays separating independent sensor polls
                        Thread.sleep(3000);

                        currentTemperature = new Random().nextBoolean() ? currentTemperature + 1 : currentTemperature - 1;
                        if(currentTemperature > 28) currentTemperature = 24;
                    }
                } catch (InterruptedException e) {
                    updateLabel(thermoLabel, currentTemperature + "°C (IDLE)", Color.BLUE);
                }
            });
            activeTasks.put("Thermostat", task);
        }
    }

    // --- SURVEILLANCE PROCESSING: FORK/JOIN DIVIDE-AND-CONQUER FRAMEWORKS (Chapter 11) ---
    private void runCameraFrameProcessing() {
        if (isEmergencyMode) return;
        executor.submit(() -> {
            updateLabel(cameraLabel, "FILTERING FRAMES...", Color.ORANGE);
            log("[CCTV] Capturing continuous raw input frame matrix...");

            long startTime = System.currentTimeMillis();

            // Chapter 11: Create the root recursive workload splitting task across multi-core processors
            CameraFilterTask rootTask = new CameraFilterTask(0, 1000);
            // Chapter 11: Execute worker framework threads using the target pool invocation mechanism
            forkJoinPool.invoke(rootTask);

            long endTime = System.currentTimeMillis();
            updateLabel(cameraLabel, "LIVE FEED FEEDING", new Color(0, 150, 0));
            log("[CCTV] Parallel multi-core subtask matrix processing wrapped up in: " + (endTime - startTime) + "ms");
        });
    }

    // --- ELECTRICAL MONITOR: PARALLEL MAP-FILTER-REDUCE PIPELINE OPERATIONS (Chapter 12) ---
    private void runDynamicParallelScan() {
        if (isEmergencyMode) return;
        executor.submit(() -> {
            log("[Scan] Running dynamic stream dataset decomposition pipelines...");
            Random rand = new Random();
            List<Integer> readings = Stream.generate(() -> rand.nextInt(500))
                    .limit(50000)
                    .collect(Collectors.toList());

            // Chapter 12: Launch concurrent stream reduction operations splitting datasets across all available CPU cores
            long total = readings.parallelStream()
                    .filter(p -> p > 250)
                    .mapToLong(p -> p / 5)
                    .sum(); // Terminal aggregate reduction execution step merges core answers safely

            updateLabel(energyLabel, total + "W", Color.BLUE);
            log("[Scan] Reduction operation calculations updated: " + total + "W");
        });
    }

    // --- LIVENESS OVERRIDES: MANDATORY INTERRUPTION CRITERIA (Chapter 4) ---
    private void emergencyStop() {
        // Change the volatile gate flag to true so that no new asynchronous threads can pass entry checks
        isEmergencyMode = true;
        log("[!!!] CRITICAL HARDWARE ALERT: EMERGENCY INTERRUPT COMMAND ISSUED.");

        // Chapter 4: Iterate structural task maps, issuing explicit cancel (true) signals to terminate active runnables
        activeTasks.forEach((name, task) -> task.cancel(true));
        activeTasks.clear();

        updateLabel(systemStatus, "EMERGENCY HALT", Color.RED);
        updateLabel(doorLabel, "HALTED", Color.BLACK);
        updateLabel(lightLabel, "HALTED", Color.BLACK);
        updateLabel(fanLabel, "HALTED", Color.BLACK);
        updateLabel(acLabel, "HALTED", Color.BLACK);
        updateLabel(energyLabel, "HALTED", Color.BLACK);
        updateLabel(thermoLabel, "HALTED (OFFLINE)", Color.BLACK);
        updateLabel(cameraLabel, "HALTED (OFFLINE)", Color.BLACK);

        stopBtn.setEnabled(false);
        resumeBtn.setEnabled(true);
        masterBtn.setEnabled(false);
        scanBtn.setEnabled(false);
    }

    // --- RECOVERY MECHANISM: SAFETY RESET PROTOCOLS ---
    private void resumeSystem() {
        log("[System] Validating environmental controls. Resetting core active flags...");
        // Re-allow background processing pathways by lowering the safety override barrier variable
        isEmergencyMode = false;

        updateLabel(systemStatus, "NORMAL", new Color(0, 150, 0));
        updateLabel(doorLabel, "CLOSED", Color.RED);
        updateLabel(lightLabel, "OFF", Color.GRAY);
        updateLabel(fanLabel, "OFF", Color.GRAY);
        updateLabel(acLabel, "OFF", Color.GRAY);
        updateLabel(energyLabel, "0W", Color.BLUE);
        updateLabel(thermoLabel, currentTemperature + "°C (IDLE)", Color.BLUE);
        updateLabel(cameraLabel, "IDLE", Color.GRAY);

        stopBtn.setEnabled(true);
        resumeBtn.setEnabled(false);
        masterBtn.setEnabled(true);
        scanBtn.setEnabled(true);
        log("[System] Core framework execution pathways completely restored.");
    }

    private JLabel createLabel(String text, Color color) {
        JLabel l = new JLabel(text);
        l.setForeground(color);
        l.setFont(new Font("Arial", Font.BOLD, 12));
        return l;
    }

    // --- GUI WORKERS: EVENT DISPATCH SYNC (Chapter 9) ---
    private void log(String msg) {
        // Chapter 9: Encapsulate text component mutations inside invokeLater to satisfy explicit Swing thread safety restrictions
        SwingUtilities.invokeLater(() -> {
            consoleLog.append(msg + "\n");
            consoleLog.setCaretPosition(consoleLog.getDocument().getLength());
        });
    }

    private void updateLabel(JLabel label, String text, Color color) {
        // Chapter 9: Route color and display adaptations safely back to the single-threaded Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            label.setText(text);
            label.setForeground(color);
        });
    }

    // --- DEEP PROCESSING COMPONENT: CORE WORK DECOMPOSITION (Chapter 11) ---
    private static class CameraFilterTask extends RecursiveAction {
        private static final int THRESHOLD = 200;
        private final int start;
        private final int end;

        public CameraFilterTask(int start, int end) {
            this.start = start;
            this.end = end;
        }

        @Override
        protected void compute() {
            if ((end - start) <= THRESHOLD) {
                try { Thread.sleep(50); } catch (InterruptedException ignored) {}
            } else {
                int mid = (start + end) / 2;
                CameraFilterTask left = new CameraFilterTask(start, mid);
                CameraFilterTask right = new CameraFilterTask(mid, end);

                // Chapter 11: Execute Fork / Join task partitioning pipelines to divide tracking array computations across cores
                invokeAll(left, right);
            }
        }
    }

    public static void main(String[] args) {
        // Chapter 9: Safe window initialization dispatched via the Event Dispatch Thread bootstrap pathway
        SwingUtilities.invokeLater(() -> new SmartHomeGUI().setVisible(true));

        Runtime.getRuntime().addShutdownHook(
                new Thread(() -> {
                    System.out.println("System shutting down...");
                })
        );
    }
}