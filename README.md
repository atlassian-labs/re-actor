# re-actor - Actor Framework with Project Reactor

[![Atlassian license](https://img.shields.io/badge/license-Apache%202.0-blue.svg?style=flat-square)](LICENSE) [![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg?style=flat-square)](CONTRIBUTING.md)

> The re-actor is an Actor Model Library, a robust solution designed to simplify the development of highly concurrent,
> and resilient applications.
>
> The actor model is a programming paradigm that uses actors as the basic building blocks of concurrent computation. In
> the actor model, actors are independent entities that communicate asynchronously by sending messages to each other. This
> library uses Project Reactor under the hood.


These are the interfaces involved for using it:

### AbstractActor

Abstract class to be implemented in order to implement actor lifecycle methods like
`Init`, `postStop`, `createReceive`, `preRestart`, `postRestart` etc.

### ActorSystem

This is the main class that provides the actor system. We can pass Scheduler that will be used to schedule tasks from
all actors in the system.

### ActorRef

This is the reference to the actor. It is used to communicate with the actor. ActorRef provides `tell` API to
asynchronously publish messages and `ask` API to synchronously publish messages & expect a response with timeout.

## Supervisor Strategies

- When there is some Exception while processing the message, Supervisor strategy kicks in.
- Parent actor’s Supervisor Strategy is used to decide SupervisorStrategyDirective for the exception

There are 4 SupervisorStrategies can be applied -

#### RESUME

- Skip the message which caused the exception & continue processing the next message

#### STOP

- Don’t process any further message.
- Terminate self & all the children.

#### RESTART

- Skip the current message which caused the exception.
- Pause the current actor & all children
- Restart the current actor & then all its children.
- Continue processing from next message.

#### ESCALATE

- If the SupervisorStrategyDirective of the parent supervisor is set to ESCALATE, then apply the same supervisor
  strategy to its own parent.
- Continue this process until you encounter a directive of either RESUME, STOP, or RESTART.
- Alternatively, if you have reached the root (ActorSystem) which has no parent, then by default the directive of STOP
  will be applied.
- This directive will be applied to all actors below in the hierarchy of actor whose parent returned this directive.

## How to use

### 1. Define AbstractActor implementation

```java
public class DemoReactor extends AbstractActor {

    private static final Logger log = LoggerFactory.getLogger(DemoReactor.class);

    @Override
    public void preStart() {
        log.info("initialised ");
    }

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .match(Integer.class, (m) -> log.info("This is Int in createReceive {}", m))
                .match(String.class, (m) -> log.info("This is String in createReceive {}", m))
                .matchAny((m) -> log.info("This is else in createReceive {}", m))
                .build();
    }

    @Override
    public void postStop() {
        log.info("Inside postStop");
    }

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return new OneForOneSupervisorStrategy(ex -> {
            if (ex instanceof Exception1)
                return SupervisorStrategyDirective.RESUME;
            return SupervisorStrategyDirective.STOP;
        });
    }

    public static class Exception1 extends Exception {
    }
}
```

### 2. Create ActorSystem

- Create ActorSystem using `ActorSystem.create` method.
- Supply a name & Scheduler to create the system.
- Register a callback to be called when the system is terminated.

```java
public class DemoReactorTest {

    @Test
    public void run() {
        ActorSystem actorSystem = ActorSystem.create("DemoActorSystem", new ReactorSystemConfig(Schedulers.boundedElastic()));
        actorSystem.registerOnTermination(() -> log.info("actorSystem " + actorSystem.getName() + " is TERMINATED"));
    }
}
```

### 3. Create ActorRef

- Create ActorRef using `ActorSystem` with `actorOf` method.

```java
ActorRef actor = actorSystem.actorOf(
        () -> new DemoReactor(),
        new ReactorConfig("MainReactor", Collections.emptyList()));
```

### 4. Use ActorRef to communicate with the actor

```java
actor.tell(999999);
actor.

tell("Hello message");
actor.

tell(1234.34442);

actor.

tell(1000,Duration.ofSeconds(1)); // publish message with some delay

        actor.

tell(PoisonPill.getInstance()); // Terminate the actor

        Thread.

sleep(5000); // wait
```

## More Details

- We first need to create an **ActorSystem** which can be used to create an `Actor(ActorRef)`. We have to pass a
  Scheduler to the ActorSystem which will be used to schedule tasks from all actors in the system.
- Actor can create child actors using `actorOf` API.
- Actor provides `tell` API to asynchronously publish messages with thread-safety.
- Actor maintains an internal queue & process the messages on same thread & in same order in which they are published.
- On start of the Actor, `preStart` method is called.
- Actor can be asynchronously terminated using `PoisonPill` message. Actor will process all the messages published
  before PoisonPill before terminating itself.
- You can also **schedule** any message to be processed async after some delay.
- If **ActorSystem** or Actor is terminated all its children should also be terminated asynchronously. And `postStop`
  method is called on Actor.
- Actor can be watched by other actors using watch API. When an Actor terminates its parent & all watchers would be
  notified.
- In case of any exception while processing the message, **Supervisor strategy** of parent kicks in.

- Following Supervisor Strategies are supported -
    1. **STOP** - Stops the actor & all its children
    2. **RESUME** - Skips the message which caused the exception & continue processing the next message.
    3. **RESTART**[maxRetries(default = 3)] [default]
        - Skips the current message which caused the exception.
        - Pause all children & current actor.
        - Restart the current actor & then all its children.
        - If all retries are exhausted, then stop the actor & all its children.
        - While restarting actor, preRestart & postRestart methods is called.
    4. **ESCALATE**
        - If the SupervisorStrategyDirective of the parent supervisor is set to ESCALATE, then apply the same supervisor
          strategy to its own parent.
        - Continue this process until you encounter a directive of either RESUME, STOP, or RESTART.
        - Alternatively, if you have reached the root (ActorSystem) which has no parent, then by default the directive
          of STOP will be applied.
        - This directive will be applied to all actors below in the hierarchy of actor whose parent returned this
          directive.
- ActorSystem & Actor emits following type of metrics -
    1. [Scheduler metrics](https://projectreactor.io/docs/core/release/reference/#micrometer-details-timedScheduler)
    2. [Reactor metrics](https://projectreactor.io/docs/core/release/reference/#micrometer-details-metrics)

## Installation

```shell
./gradlew clean build
```

## Tests

```shell
./gradlew test
```

## Contributions

Contributions to **re-actor** are welcome! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for details.

## License

Copyright (c) [2024] Atlassian US., Inc.
Apache 2.0 licensed, see [LICENSE](LICENSE) file.

<br/>

[![With â¤ï¸ from Atlassian](https://raw.githubusercontent.com/atlassian-internal/oss-assets/master/banner-cheers.png)](https://www.atlassian.com)