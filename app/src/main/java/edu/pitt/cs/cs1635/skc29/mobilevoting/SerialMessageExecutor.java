package edu.pitt.cs.cs1635.skc29.mobilevoting;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.Executor;

/**
 * Created by Spencer Cousino on 2/28/2017.
 */

class SerialMessageExecutor implements Executor {
    private final Queue<Runnable> messageQueue = new ArrayDeque<>();
    private final Executor executor;
    private Runnable currentMessage;

    //Constructor
    SerialMessageExecutor(Executor ex) {
        this.executor = ex;
    }

    @Override
    public synchronized void execute(final Runnable command) {
        messageQueue.add(new Runnable() {
            @Override
            public void run() {
                try {
                    command.run();
                } finally {
                    queueNextMessage();
                }
            }
        });

        if(currentMessage == null) {
            queueNextMessage();
        }
    }

    protected synchronized void queueNextMessage() {
        if ((currentMessage = messageQueue.poll()) != null) {
            executor.execute(currentMessage);
        }
    }
}
