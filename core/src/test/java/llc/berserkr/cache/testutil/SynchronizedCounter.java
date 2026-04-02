package llc.berserkr.cache.testutil;

public class SynchronizedCounter {
    private int count = 0;

    public SynchronizedCounter(int initialValue) {
        this.count = initialValue;
    }

    public synchronized void increment() {
        count++;
    }

    public synchronized void decrement() {
        count--;
    }

    public synchronized int getCount() {
        return count;
    }
}
