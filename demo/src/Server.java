import connection.ConnectionFactory;
import thread_pool.CustomThread;
import thread_pool.CustomThreadPool;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    public static void main(String[] args) {
        ExecutorService es = Executors.newCachedThreadPool();
        ConnectionFactory.init();
        for (int i = 0; i <=200; i++) {
            es.submit(new CustomThread(i));
        }

    }
}