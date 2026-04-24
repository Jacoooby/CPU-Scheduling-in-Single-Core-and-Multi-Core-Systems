import pandas as pd
import numpy as np
from sklearn.model_selection import train_test_split
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import accuracy_score, classification_report, confusion_matrix
import threading
from collections import deque

class SimTask:

    # Constructor for a simulated task
    def __init__(self, pid, arrival_time, burst_time):
        self.pid = pid
        self.arrival_time = float(arrival_time)
        self.max_burst = float(burst_time)
        self.current_burst = 0.0
        self.remaining_burst = float(burst_time)
        self.state = "NEW"
        self.finished = False

    # Simulates the cpu running a task given an amount of time
    def run_one_burst(self, amount=1.0):
        if self.finished:
            return 0.0

        actual = min(amount, self.remaining_burst)
        self.current_burst += actual
        self.remaining_burst -= actual

        if self.remaining_burst <= 0.00000001:
            self.remaining_burst = 0.0
            self.finished = True
            self.state = "FINISHED"

        return actual

    # Outputs the string form of the simulated task for logging and debugging
    def __repr__(self):
        return (
            f"P{self.pid}(arrival={self.arrival_time}, "
            f"max={self.max_burst}, current={self.current_burst}, "
            f"remaining={self.remaining_burst}, state={self.state})"
        )

class ReadyQueue:

    # Constructor for the ready queue
    def __init__(self):
        self.queue = deque()
        self.lock = threading.Lock()

    # Method to add a task to the queue
    def add_task(self, task):
        with self.lock:
            if task is not None and task not in self.queue:
                self.queue.append(task)

    # Method to remove a task from the queue
    def remove_task(self, task):
        with self.lock:
            if task in self.queue:
                self.queue.remove(task)

    # Method to pop the first task in the queue (follows FIFO for round-robin)
    def pop_left(self):
        with self.lock:
            if self.queue:
                return self.queue.popleft()
            return None

    # Method to return a copy of the current queue
    def snapshot(self):
        with self.lock:
            return list(self.queue)

    # Method to clear the queue once all tasks are finished
    def clear_completed(self):
        with self.lock:
            self.queue = deque([task for task in self.queue if not task.finished])

    # Method to check if the queue is empty
    def is_empty(self):
        with self.lock:
            return len(self.queue) == 0

    # Method to return the number of tasks currently in the queue
    def __len__(self):
        with self.lock:
            return len(self.queue)

class SimulationLogger:

    # Constructor for the logger
    def __init__(self):
        self.entries = []
        self.lock = threading.Lock()

    # Method that constructs a log of the current state of the simulation and saves it as an entry
    def log(self, time, ready_queue, action, chosen, remaining_after):
        with self.lock:
            if remaining_after == "-" or remaining_after is None:
                remaining_value = "-"
            else:
                remaining_value = int(remaining_after)

            self.entries.append({
                                "Time": int(time),
                                "ReadyQueue": ready_queue,
                                "Action": action,
                                "Chosen": chosen,
                                "RemainingAfter": remaining_value
                                })

    # Method to print all entries saved by the logger
    def print_all(self):
        print("=" * 70)
        print("SCHEDULER DECISION LOG")
        print("=" * 70)
        df = pd.DataFrame(self.entries)
        print(df.to_string(index=True))

class TaskWorker(threading.Thread):

    # Constructor for the worker thread
    def __init__(self, task):
        super().__init__(daemon=True)
        self.task = task

        self.run_sem = threading.Semaphore(0)
        self.done_sem = threading.Semaphore(0)

        self.shutdown_requested = False
        self.assigned_burst = 0.0

    # Method to assign the amount of time the worker thread is to run on the cpu
    def assign_cpu_burst(self, burst_time):
        self.assigned_burst = float(burst_time)
        self.run_sem.release()

    # Method to instruct the cpu how long to wait before the worker thread is done with its burst
    def wait_for_completion(self):
        self.done_sem.acquire()

    # Method to shut down the worker thread
    def shutdown_worker(self):
        self.shutdown_requested = True
        self.run_sem.release()

    # Method to start work on the thread
    def run(self):
        while True:
            self.run_sem.acquire()

            if self.shutdown_requested:
                self.done_sem.release()
                break

            self.task.state = "RUNNING"
            self.task.run_one_burst(self.assigned_burst)

            if not self.task.finished:
                self.task.state = "READY"

            self.done_sem.release()

class CPU:
    # Constructor for the virtual CPU
    def __init__(self, cpu_id=0):
        self.cpu_id = cpu_id

    # Method to facilitate the running of a thread on the virtual CPU and to return the amount of time the worker thread
    # took to complete
    def run_task(self, worker, burst_time):
        if worker is None:
            return 0.0

        before = worker.task.current_burst
        worker.assign_cpu_burst(burst_time)
        worker.wait_for_completion()
        after = worker.task.current_burst
        return after - before

# Implementation of the Java class of the same name in python
class SingleCoreDispatcher(threading.Thread):

    # Constructor for the dispatcher
    def __init__(self, cpu, ready_queue, pending_tasks, workers, model, feature_columns, rr_quantum, logger):
        super().__init__()
        self.cpu = cpu
        self.ready_queue = ready_queue
        self.pending_tasks = sorted(pending_tasks, key=lambda t: (t.arrival_time, t.pid))
        self.workers = workers
        self.model = model
        self.feature_columns = feature_columns
        self.rr_quantum = float(rr_quantum)
        self.rr_active_task = None
        self.rr_ticks_used = 0.0
        self.logger = logger

        self.current_time = 0.0
        self.current_task = None
        self.total_bursts_executed = 0.0
        self.initial_total_bursts = sum(worker.task.max_burst for worker in workers.values())

    # Method to temporarily hold tasks and add them to the ready queue
    def release_pending_arrivals(self):
        released_any = False
        i = 0

        while i < len(self.pending_tasks):
            task = self.pending_tasks[i]
            if task.arrival_time <= self.current_time:
                task.state = "READY"
                self.ready_queue.add_task(task)
                self.pending_tasks.pop(i)
                released_any = True
            else:
                i += 1

        return released_any

    # Method to compile input features for the model to predict upon
    def compute_features(self):
        ready_tasks = self.ready_queue.snapshot()

        remaining = [task.remaining_burst for task in ready_tasks]
        if len(remaining) == 0:
            remaining = [0.0]

        feature_values = {
            "QueueId": 0.0,
            "QueueThreadCount": float(len(ready_tasks)),
            "QueueMaxRemainingBursts": float(max(remaining)),
            "QueueMinRemainingBursts": float(min(remaining)),
            "QueueMeanRemainingBursts": float(sum(remaining) / len(remaining)),
            "QueueMedianRemainingBursts": 0.0,
            "QueueRangeRemainingBursts": 0.0,
            "QueueTotalRemainingBursts": float(sum(remaining)),
            "CoreBurstAge": 0.0,
            "ThreadBurstsRan": float(self.total_bursts_executed),
            "ThreadBurstsRemaining": float(self.initial_total_bursts - self.total_bursts_executed),
            "AvgThreadThroughput": 0.0,
            "AvgThreadTurnaround": 0.0
        }

        ordered = {}
        for col in self.feature_columns:
            ordered[col] = float(feature_values.get(col, 0.0))

        return ordered

    # Method to use the model to predict the next action based upon the current feature set
    def predict_action(self):
        features = self.compute_features()
        X = pd.DataFrame([features])

        action = self.model.predict(X)[0]

        if action == "NEXT_OTHER":
            action = "NEXT_FCFS"

        if action == "NONE" and self.current_task is None:
            action = "NEXT_FCFS"

        return action, features

    # Method to select which task should be done depending on the action predicted by the model
    def choose_task_from_action(self, action):
        ready_tasks = self.ready_queue.snapshot()

        if action == "NONE":
            return self.current_task

        if len(ready_tasks) == 0:
            return None

        if action == "NEXT_FCFS":
            chosen = min(ready_tasks, key=lambda t: (t.arrival_time, t.pid))
            self.ready_queue.remove_task(chosen)
            return chosen

        if action == "NEXT_SJF":
            chosen = min(ready_tasks, key=lambda t: (t.remaining_burst, t.arrival_time, t.pid))
            self.ready_queue.remove_task(chosen)
            return chosen

        if action == "NEXT_RR":
            if (
                self.rr_active_task is not None
                and not self.rr_active_task.finished
                and self.rr_ticks_used < self.rr_quantum
                ):
                return self.rr_active_task

            chosen = self.ready_queue.pop_left()

            if chosen is not None:
                self.rr_active_task = chosen
                self.rr_ticks_used = 0.0
            return chosen

        chosen = min(ready_tasks, key=lambda t: (t.arrival_time, t.pid))
        self.ready_queue.remove_task(chosen)
        return chosen

    # Method to simulate the running of the dispatcher (Most of the logic of the simulator)
    def run(self):
        total_task_count = len(self.workers)
        finished_count = 0

        while finished_count < total_task_count:

            # Clean the queue
            self.ready_queue.clear_completed()
            self.release_pending_arrivals()

            # If there is no task, IDLE
            if self.current_task is None and self.ready_queue.is_empty():
                self.logger.log(
                                time=self.current_time,
                                ready_queue=[],
                                action="IDLE",
                                chosen="-",
                                remaining_after="-"
                                )
                self.current_time += 1.0
                continue

            # Predict the next action
            action, features = self.predict_action()

            # Handle NONE fallthrough
            if self.current_task is not None and self.ready_queue.is_empty():
                action = "NONE"
                selected_task = self.current_task
            else:
                selected_task = self.choose_task_from_action(action)

            if action == "NONE" and selected_task is None:
                action = "NEXT_FCFS"
                selected_task = self.choose_task_from_action(action)

            if selected_task is None:
                self.logger.log(
                                time=self.current_time,
                                ready_queue=[],
                                action="IDLE",
                                chosen="-",
                                remaining_after="-"
                                )
                self.current_time += 1.0
                continue

            # If tasks are being switched add the current task back to the queue
            if (
                    self.current_task is not None
                    and self.current_task != selected_task
                    and not self.current_task.finished
            ):
                self.current_task.state = "READY"
                self.ready_queue.add_task(self.current_task)

            # Advance CPU clock by one
            burst_time = 1.0
            actual_ran = self.cpu.run_task(self.workers[selected_task.pid], burst_time)

            # Update and log the system state
            self.total_bursts_executed += actual_ran
            ready_snapshot = [f"P{task.pid}" for task in self.ready_queue.snapshot()]

            self.logger.log(
                            time=self.current_time,
                            ready_queue=ready_snapshot,
                            action=action,
                            chosen=f"P{selected_task.pid}",
                            remaining_after=selected_task.remaining_burst
                            )

            # Advance the system time
            self.current_time += actual_ran

            # Handle task if completed and RR if not
            if selected_task.finished:
                selected_task.state = "FINISHED"
                finished_count += 1

                if action == "NEXT_RR" and self.rr_active_task == selected_task:
                    self.rr_active_task = None
                    self.rr_ticks_used = 0.0

                self.current_task = None

            else:
                if action == "NEXT_RR":
                    if self.rr_active_task != selected_task:
                        self.rr_active_task = selected_task
                        self.rr_ticks_used = 0.0

                    self.rr_ticks_used += 1.0

                    if self.rr_ticks_used >= self.rr_quantum:
                        selected_task.state = "READY"
                        self.ready_queue.add_task(selected_task)
                        self.rr_active_task = None
                        self.rr_ticks_used = 0.0
                        self.current_task = None
                    else:
                        self.current_task = selected_task

                elif action == "NONE":
                    self.current_task = selected_task

                else:
                    self.rr_active_task = None
                    self.rr_ticks_used = 0.0

                    self.current_task = selected_task

def train_model():
    ## Dataset creation and manips
    # Loading dataset
    dataset = pd.read_csv("Sample.csv")

    # Printing dataset info
    print("Rows: ", len(dataset))
    print()

    print("Action counts overall:")
    print(dataset["Action"].value_counts())
    print()

    # Converting both versions of inf to NaN
    dataset = dataset.replace([np.inf, -np.inf], np.nan)

    # Changing all NaNs to 0
    dataset = dataset.fillna(0)

    # Dropping the 4 specified columns
    dataset = dataset.drop(columns=["SchedType", "CoreCount", "CoreId", "IsCoreIdle"])

    # Separating features and labels
    features = dataset.drop(columns=["Action"])
    labels = dataset["Action"]

    ## Model creation
    # Splitting dataset into testing and training
    X_train, X_test, y_train, y_test = train_test_split(features, labels, test_size=0.2, random_state=42, stratify=labels)

    # Creating Random Forest Model
    model = RandomForestClassifier(n_estimators=2000, random_state=42, n_jobs=-1)

    # Print feature list
    print("Number of features:", features.shape[1])
    print("Feature names:", features.columns.tolist())
    print()

    # Training the model
    model.fit(X_train, y_train)

    # Create test predictions onto the test set
    testPredictions = model.predict(X_test)

    accuracy = accuracy_score(y_test, testPredictions)
    print("Accuracy:")
    print(accuracy)
    print()

    print("Classification report:")
    print(classification_report(y_test, testPredictions))
    print()

    confusion = pd.crosstab(
        y_test,
        testPredictions,
        rownames=["Actual Action"],
        colnames=["Predicted Action"]
    )

    print("Confusion matrix:")
    print(confusion)
    print()

    print("Raw confusion matrix (rows=true, cols=pred):")
    print(confusion_matrix(y_test, testPredictions))

    return model, list(features.columns)

# Helper method to check ints
def check_int(prompt, minimum = None):
    while True:
        try:
            value = int(input(prompt))

            if minimum is not None and value < minimum:
                print(f"Value must be at least {minimum}.")
                continue

            return value
        except ValueError:
            print("Please enter a valid integer.")

# Helper method to check floats
def check_float(prompt, minimum = None):
    while True:
        try:
            value = float(input(prompt))

            if minimum is not None and value < minimum:
                print(f"Value must be at least {minimum}.")
                continue

            return value
        except ValueError:
            print("Please enter a valid float.")

# Method to run the entire sim and model training
def run_single_core_simulator():

    # Training the model
    model, feature_columns = train_model()

    print("=" * 70)
    print("SIMPLE SIMULATION (ML predicts next queue-level action)")
    print("=" * 70)

    # User input for processes
    num_processes = check_int("How many processes? (min 2): ", 2)
    rr_quantum = check_float("RR quantum (e.g., 2): ", 0.0000001)

    all_tasks = []
    for i in range(num_processes):
        arrivalTime = check_float(f"P{i} arrival time: ", 0)
        burstTime = check_float(f"P{i} burst time: ", 0.0000001)

        all_tasks.append(SimTask(i, arrivalTime, burstTime))

    # Initializing queue, worker threads, and tasks
    ready_queue = ReadyQueue()
    pending_tasks = []
    workers = {}

    # Assigning tasks to workers and starting them
    for task in all_tasks:
        worker = TaskWorker(task)
        workers[task.pid] = worker
        worker.start()

        if task.arrival_time == 0:
            task.state = "READY"
            ready_queue.add_task(task)
        else:
            pending_tasks.append(task)

    # Initializing logger and virtual cpu
    logger = SimulationLogger()
    cpu = CPU(0)

    # Initializing the simulator
    dispatcher = SingleCoreDispatcher(
                                      cpu=cpu,
                                      ready_queue=ready_queue,
                                      pending_tasks=pending_tasks,
                                      workers=workers,
                                      model=model,
                                      feature_columns=feature_columns,
                                      rr_quantum=rr_quantum,
                                      logger=logger
                                    )

    # Starting the simulation
    dispatcher.start()
    dispatcher.join()

    # Terminating worker threads
    for worker in workers.values():
        worker.shutdown_worker()
    for worker in workers.values():
        worker.join()

    # Display results of simulation
    logger.print_all()

if __name__ == "__main__":
    run_single_core_simulator()