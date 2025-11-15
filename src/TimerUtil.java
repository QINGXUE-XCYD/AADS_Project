class TimerUtil {
    private long startTime;
    private long originTime;

    void start() {
        startTime = System.currentTimeMillis();
        originTime = startTime;
    }

    void printElapsed(String msg) {
        long now = System.currentTimeMillis();
        System.err.println(msg + " 耗时: " + (now - startTime) + " ms" + "; 共: " + (now - originTime) + " ms)");
        startTime = now;
    }
}
