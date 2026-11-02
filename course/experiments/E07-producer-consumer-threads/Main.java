class MessageBuffer {
    private String message;
    private boolean isEmpty = true;

    // Producer calls put() to place a message into the buffer
    public synchronized void put(String msg) {
        while (!isEmpty) { // Wait while buffer is still full
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        this.message = msg;
        this.isEmpty = false;
        System.out.println("Produced: " + msg);
        notify(); // Wake up Consumer thread
    }

    // Consumer calls take() to retrieve a message from the buffer
    public synchronized String take() {
        while (isEmpty) { // Wait while buffer is empty
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        String msg = this.message;
        this.isEmpty = true;
        System.out.println("Consumed: " + msg);
        notify(); // Wake up Producer thread
        return msg;
    }
}

class Producer extends Thread {
    private final MessageBuffer buffer;

    Producer(MessageBuffer buffer) {
        this.buffer = buffer;
    }

    @Override
    public void run() {
        for (int i = 1; i <= 5; i++) {
            buffer.put("Message " + i);
        }
    }
}

class Consumer extends Thread {
    private final MessageBuffer buffer;

    Consumer(MessageBuffer buffer) {
        this.buffer = buffer;
    }

    @Override
    public void run() {
        for (int i = 1; i <= 5; i++) {
            buffer.take();
        }
    }
}

public class Main {
    public static void main(String[] args) {
        MessageBuffer buffer = new MessageBuffer();
        Producer producer = new Producer(buffer);
        Consumer consumer = new Consumer(buffer);

        producer.start();
        consumer.start();
    }
}
