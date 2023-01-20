package connection;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public class ConnectionFactory {

    private static AtomicReference<Connection> mainConnection;
    private static List<AtomicReference<Connection>> preConnectionList = new ArrayList<>();


    public static void init(){
        mainConnection = new AtomicReference<>(new Connection(UUID.randomUUID().toString()));
        if(preConnectionList.size() == 0) {
            for (int i = 0; i < 3; i++) {

                preConnectionList.add(new AtomicReference<>(new Connection(UUID.randomUUID().toString())));
            }
        }
    }
    public static Connection getConnection(){
        if(mainConnection.get().isRunning()){
            return mainConnection.get();
        }

        changeMainConnection();
        return mainConnection.get();
    }

    public static synchronized void changeMainConnection(){

        if(mainConnection.get().isRunning() == true){
            return;
        }
        if(mainConnection.get().isRunning() == false) {
            System.out.println("change 수행");
            mainConnection.compareAndSet(mainConnection.get(), preConnectionList.get(0).get());
            preConnectionList.remove(0);
        }
        if(preConnectionList.size() == 1){
            preConnectionList.add(new AtomicReference<>(new Connection(UUID.randomUUID().toString())));
        }


    }



}
