import java.util.concurrent.Semaphore;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PCWithSemaphores {

    static class BoundedBuffer<T> {
        private final Object[] buffer;
        private int head = 0; 
        private int tail = 0; 
        private int count = 0;

        private final Semaphore empty; 
        private final Semaphore full;  
        private final Semaphore mutex; 

        public BoundedBuffer(int capacity) {
            if (capacity <= 0) throw new IllegalArgumentException("capacidad > 0");
            this.buffer = new Object[capacity];
            this.empty = new Semaphore(capacity, true);
            this.full = new Semaphore(0, true);
            this.mutex = new Semaphore(1, true); 
        }

        public void put(T item) throws InterruptedException {
            empty.acquire();     
            mutex.acquire();     
            try {
                buffer[tail] = item;
                tail = (tail + 1) % buffer.length;
                count++;
            } finally {
                mutex.release(); 
                full.release(); 
            }
        }

        @SuppressWarnings("unchecked")
        public T take() throws InterruptedException {
            full.acquire();     
            mutex.acquire();     
            try {
                T item = (T) buffer[head];
                buffer[head] = null;
                head = (head + 1) % buffer.length;
                count--;
                return item;
            } finally {
                mutex.release(); 
                empty.release(); 
            }
        }

        public int size() { return count; }
        public int capacity() { return buffer.length; }
    }

    static class Producer extends Thread {
        private final BoundedBuffer<Integer> buf;
        private final int id;
        private final Random rnd = new Random();

        public Producer(int id, BoundedBuffer<Integer> buf) {
            this.id = id;
            this.buf = buf;
            setName("Producer-" + id);
        }

        @Override public void run() {
            try {
                for (int i = 0; i < 50; i++) {
                    int item = id * 1000 + i;
                    buf.put(item);
                    System.out.printf("[%s] produjo %d (buf=%d/%d)%n",
                            getName(), item, buf.size(), buf.capacity());
                    Thread.sleep(rnd.nextInt(20) + 5);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    static class Consumer extends Thread {
        private final BoundedBuffer<Integer> buf;
        private final int id;
        private final Random rnd = new Random();

        public Consumer(int id, BoundedBuffer<Integer> buf) {
            this.id = id;
            this.buf = buf;
            setName("Consumer-" + id);
        }

        @Override public void run() {
            try {
                for (int i = 0; i < 50; i++) {
                    int item = buf.take();
                    System.out.printf("[%s] consumió %d (buf=%d/%d)%n",
                            getName(), item, buf.size(), buf.capacity());
                    Thread.sleep(rnd.nextInt(30) + 5);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        int capacity = 10;
        int producers = 3;
        int consumers = 3;

        BoundedBuffer<Integer> buffer = new BoundedBuffer<>(capacity);
        List<Thread> threads = new ArrayList<>();

        for (int p = 0; p < producers; p++) threads.add(new Producer(p, buffer));
        for (int c = 0; c < consumers; c++) threads.add(new Consumer(c, buffer));

        threads.forEach(Thread::start);
        for (Thread t : threads) t.join();

        System.out.println("Ejecución con semáforos finalizada.");
    }
}
