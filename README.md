# practice-preconnection
Practice Preconnection in Async Enviroment

# Overview

실제 서비스에서 항상 성공적인 로직의 수행이 꿈만 같은 이야기일 것입니다.
불행하게도 여러가지 장애가 발생할 것이고, 이에 따른 대비책을 세워두어야 합니다.

# 설명
본 레포지토리는 서버 성능 향상 및 장애 대응에 대한 내용을 다루었습니다.

## 환경
* 모든 로직은 비동기로 이루어졌습니다.
* 외부 Connection을 이용하여 로직이 수행됩니다.
* 별도의 메모리 누수가 없어야 합니다.

## 문제 및 요구사항

* 성능 향상 : 동기적인 코드로 인한 성능 이슈를 최적화
* 장애 대응 : 외부 Connection 장애로 인한 새 Connection 대비
  * 새 Connection의 동시 요청에 대한 대비
  * 장애에 대한 로깅
* 누수되는 요청이 없어야 함

# 코드 & 설명
구성되어있는 코드는 다음과 같습니다.

## Connection

* Connection : 외부 Connection을 말하는 객체입니다.
* ConnectionFactory : 다수의 Connection을 관리해주는 Factory입니다.

## Server
* Server : main 메서드를 가지고 있으며, 자체적으로 요청을 만듭니다.

## Thread
* CustomThread : 실제 요청마다 행해질 동작을 명시한 객체입니다. tomcat에서의 worker Thread라고 보면 됩니다.

## User
* User : 더미데이터를 위한 객체입니다.

## 전체 흐름 파악하기

1. Server(main) 에서 ThreadPool을 준비합니다.

```java
ExecutorService es = Executors.newCachedThreadPool();
```

2. Server(main) 에서 ConnectionFactory를 통해서 Connection을 준비합니다.

```java
ConnectionFactory.init();
```

3. Server(main) 에서 Thread를 201개 생성합니다.

```java
for (int i = 0; i <=200; i++) {
            es.submit(new CustomThread(i));
}
```

4. Thread 내용은 다음과 같습니다.

```java
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
```

Connection에 존재하는 `getDataBy`를 호출합니다.

NullPointerException이 터지게 되면, `ConnectionFactory`를 통해서 예비 Connection을 새롭게 등록합니다.

5. Connection의 내용은 다음과 같습니다.

```java
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
}

```

요청이 올 때마다, `Count`의 수를 증가하고 100이 되었을 경우 새로운 Request 요청이 오면 NullPointerException을 터트립니다.

6. ConnectionFactory의 내용은 다음과 같습니다.

```java
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
            System.out.println(" ----------------------");
            System.out.println("Change 수행");
            System.out.println("예외가 생긴 Connection Id : " + mainConnection.get().getConnectionId());
            System.out.println("새롭게 바뀔 Connection Id : " + preConnectionList.get(0).get().getConnectionId());

            mainConnection.compareAndSet(mainConnection.get(), preConnectionList.get(0).get());
            preConnectionList.remove(0);
        }
        if(preConnectionList.size() == 1){
            preConnectionList.add(new AtomicReference<>(new Connection(UUID.randomUUID().toString())));
        }


    }

}

```

예비로 준비해둔 Connection을 조회하여 새롭게 등록합니다.




# 결과 보기

<img width="503" alt="image" src="https://user-images.githubusercontent.com/79268661/213625594-83ab64ca-da28-4f17-a000-3d1240dc8dc7.png">
위의 화면에서 볼 수 있듯이, Connection이 총 3개가 사용된 것을 볼 수 있습니다.

# 고민 및 핵심 요점 살펴보기

## 원자성

각각의 Thread가 수행되기 때문에, 모든 코드는 비동기적으로 작동을 합니다.

그래서 `getDataBy` 호출은 비동기적으로 예상할 수 없습니다.

특히나 내부에 `Count`값은 원자성이 유지되기가 힘듭니다.

그리하여 다음의 객체를 사용하였습니다.

```java
private AtomicInteger requestCount = new AtomicInteger(1);
```

또한 해당 `requestCount`를 +1 해주는 작업 또한 작업 순서를 보장할 수가 없습니다. 그리하여 다음의 `getAndAdd()` 함수를 사용하여 숫자를 증가시켜주었습니다.

```java
this.requestCount.getAndAdd(1);
```

## 임계영역

`ConnectionFactory`에서 `changeMainConnection()` 메서드 또한 임계영역이라고 판단이 되었습니다. 이유는 비동기적으로 처리가 되기 때문에 산발적인 change가 발생합니다. 각 코드가 따로 비동기적으로 돌면 Connection이 한번에 여러개가 바뀌는 현상이 일어난다고 판단하였습니다. 그래서 `changeMainConnection()` 메서드는 `synchronized`를 이용하여 잠금을 하였습니다.

```java
public static synchronized void changeMainConnection(){

        if(mainConnection.get().isRunning() == true){
            return;
        }
        if(mainConnection.get().isRunning() == false) {
            mainConnection.compareAndSet(mainConnection.get(), preConnectionList.get(0).get());
            preConnectionList.remove(0);
        }
        if(preConnectionList.size() == 1){
            preConnectionList.add(new AtomicReference<>(new Connection(UUID.randomUUID().toString())));
        }


    }
```

또한 임계구역에 접근을 성공했더라도 mainConnection을 확인하고 바로 나갈 수 있도록 if문을 추가하였습니다.

## Predicate<T>

함수형 인터페이스인 `Predicate<T>`를 이용하였습니다.

먼저 `Filter` 인터페이스를 추가해주었습니다.

`Failter` 인터페이스는 다음과 같습니다.

```java
public interface Filter extends Predicate<User> {

    boolean test(User user);
}
```

또한 구현체는 람다를 통해서 다음과 같이 전달하였습니다.

```java
connection.getDataBy(user -> user.getAge() == 1);
```

# 아쉬운 점

모든 코드가 비동기적으로 작동하여 예상하지 못하는 결과가 나왔습니다.

> Connection에서의 `requestCount`가 100이 되면 새롭게 Connection이 바뀌어야 하는데 실제로 console 출력을 해보니 `requestCount`가 150까지 증가하는 경우가 생겼습니다.


아직 원인은 파악하지 못하였습니다. 추 후에 학습을 통해서 알아낼 예정입니다.

# Reference

* [HyperConnect 기술 블로그](https://hyperconnect.github.io/2020/03/24/improve-stomp-client.html)

* [Java References](https://docs.oracle.com/en/java/javase/17/docs/api/index.html)
















