class TimerUtil {
    private long startTime;

    void start() {
        startTime = System.currentTimeMillis();
    }

    void printElapsed(String msg) {
        long now = System.currentTimeMillis();
        System.err.println(msg + " 耗时: " + (now - startTime) + " ms");
        startTime = now;
    }
}
