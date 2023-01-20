package thread_pool;

import connection.Connection;
import connection.ConnectionFactory;
import user.User;

import java.util.function.Predicate;

public class CustomThread implements Runnable{
    private int threadNum;
    public CustomThread(int i){
        this.threadNum = i;
    }
    @Override
    public void run() {
        Connection connection = ConnectionFactory.getConnection();

        try {
            connection.getDataBy(user -> user.getAge() == 1);
        }catch (NullPointerException e){
            ConnectionFactory.changeMainConnection();
            run();
        }

    }
}
