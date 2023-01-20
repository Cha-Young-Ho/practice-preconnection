package connection;

import filter.Filter;
import user.User;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;


public class Connection<T> {

    private String connectionId;
    private List<User> dataList;

    private AtomicBoolean isRunning = new AtomicBoolean(false);

    private AtomicInteger requestCount = new AtomicInteger(1);

    public Connection(String connectionId) {
        this.connectionId = connectionId;
        this.dataList = new ArrayList<>();
        this.isRunning.compareAndExchange(false, true);

        for (int i = 0; i < 100; i++) {
            dataList.add(new User(UUID.randomUUID().toString(), "Name" + i, i, "location" + i % 10));
        }
    }

    public List<User> getDataBy(Filter filter){
        if(requestCount.get() % 100 == 0){
            isRunning.compareAndSet(true, false);
            throw new NullPointerException();
        }
        List<User> foundedUserList = new ArrayList<>();
        for (User user : this.dataList){
            if(filter.test(user)){
                foundedUserList.add(user);
            }
        }
        this.requestCount.getAndAdd(1);
        return foundedUserList;
    }



    /*--- Getter And Setter ---*/
    public String getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(String connectionId) {
        this.connectionId = connectionId;
    }

    public boolean isRunning() {
        return isRunning.get();
    }

    public AtomicInteger getRequestCount() {
        return requestCount;
    }
}
