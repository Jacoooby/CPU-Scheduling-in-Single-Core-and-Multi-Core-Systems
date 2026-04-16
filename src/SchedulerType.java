public enum SchedulerType {
    FCFS(1, "FCFS"),
    RR(2, "Round Robin"),
    NSJF(3, "Non Preemptive - Shortest Job First"),
    PSJF(4, "Preemptive - Shortest Job First");

    private final int code;
    private final String displayName;

    SchedulerType(int code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public int getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static SchedulerType fromCode(int code) {
        for (SchedulerType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid scheduler code: " + code);
    }
}