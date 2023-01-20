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

# 결과 보기

<img width="503" alt="image" src="https://user-images.githubusercontent.com/79268661/213625594-83ab64ca-da28-4f17-a000-3d1240dc8dc7.png">
위의 화면에서 볼 수 있듯이, Connection이 총 3개가 사용된 것을 볼 수 있습니다.


















