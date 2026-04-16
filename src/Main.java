// Begin code changes by Jacob Berard
public class Main {
    public static void main(String[] args) {
        int algorithm = -1;
        // quantum is only need for round-robin, so it can be null for other algorithms
        Integer quantum = null;
        // defaults to 1 core if -C is not entered
        int cores = 1;

        try {
            int i = 0;
            while (i < args.length) {
                if (args[i].equals("-S")) {

                    // checks if there exist a value after -S
                    if (i + 1 >= args.length) {
                        throw new IllegalArgumentException("Missing parameter after -S.");
                    }

                    // reads number after -S or throws error if not provided
                    try {
                        algorithm = Integer.parseInt(args[i + 1]);
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Algorithm after -S must be an integer 1-4.");
                    }
                    // checks if number after -S is between 1-4
                    if (algorithm < 1 || algorithm > 4) {
                        throw new IllegalArgumentException("Algorithm must be between 1 and 4.");
                    }

                    // move index past -S and algorithm number
                    i += 2;

                    // if round-robin is selected check for valid quantum value
                    if (algorithm == 2) {
                        // check if quantum value is provided
                        if (i >= args.length) {
                            throw new IllegalArgumentException("Round-robin requires a quantum between 2 and 10.");
                        }

                        try {
                            quantum = Integer.parseInt(args[i]);
                        } catch (NumberFormatException e) {
                            throw new IllegalArgumentException("Quantum must be an integer between 2 and 10.");
                        }

                        if (quantum < 2 || quantum > 10) {
                            throw new IllegalArgumentException("Quantum must be between 2 and 10.");
                        }

                        // move index past quantum value
                        i++;
                    }
                }

                // if -C is provided ensure there is a valid value after it
                else if (args[i].equals("-C")) {
                    if (i + 1 >= args.length) {
                        throw new IllegalArgumentException("Missing parameter after -C.");
                    }

                    try {
                        cores = Integer.parseInt(args[i + 1]);
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Core count must be an integer between 1 and 4.");
                    }

                    // check if core count is a valid number between 1 and 4
                    if (cores < 1 || cores > 4) {
                        throw new IllegalArgumentException("Core count must be between 1 and 4.");
                    }

                    // move index past -C and core number
                    i += 2;
                }
                else {
                    throw new IllegalArgumentException("Unknown argument: " + args[i]);
                }
            }

            // if algorithm was never selected
            if (algorithm == -1) {
                throw new IllegalArgumentException("Missing required -S argument.");
            }

            System.out.println("Valid arguments received.");
            System.out.println("Algorithm: " + algorithm);

            if (algorithm == 2) {
                System.out.println("Quantum: " + quantum);
            }

            System.out.println("Cores: " + cores);

            if (cores == 1) {
                startSingleCoreTask(algorithm, quantum);
            } else {
                startMultiCoreTask(algorithm, quantum, cores);
            }

        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
            System.out.println("\nNo starting arguments detected, Below shows a few valid ways to start the program:");
            System.out.println("  -S 1 to use First-come, first-served");
            System.out.println("  -S 2 <quantum> to use Round-Robin with a specified quantum between (2-10)");
            System.out.println("  -S 3 to use Non-Preemptive shortest job first");
            System.out.println("  -S 4 to use Preemptive shortest job first");
            System.out.println("  -C <cores> can be added after any of the above to specify the number of cores (1-4) to use on a task.");
        }
    }

    // placeholder for task 1
    public static void startSingleCoreTask(int algorithm, Integer quantum) {
        System.out.println("Starting Task 1");
    }

    // placeholder for task 2
    public static void startMultiCoreTask(int algorithm, Integer quantum, int cores) {
        System.out.println("Starting Task 2");
    }
}
// End code changes by Jacob Berard