# Internals

## How reactor was used?
- This library uses Reactor Schedulers to process actor messages in a thread.
- Sinks for publishing message to an actor & it provides data as Flux which is processed by subscriber.
- All this happens inside an actor.

### Core Reactor concept being used
This is how we are using reactor Sinks, Flux & reactor operators to create Actor type behaviour.

```kotlin
private var sink: Sinks.Many<Any> = Sinks.many()
                                        .unicast()
                                        .onBackpressureBuffer()
// sink is used to publish the message which is
// then processed by flux subscriber.
private var flux: Flux<Any> = sink.asFlux()
                                    .publishOn(Schedulers.boundedElastic())
// publishOn operator make sure that the message is processed in a thread provided by scheduler.

flux.map { processWrapper(it) }
    .doOnError { error -> log.info("Terminating due to error $error for $name") }
    .onErrorComplete()
    .doOnComplete { onTerminate() }
    .subscribe()
```
- We can publish the message to this sink using below method.
- `emitNext` method of sink is a thread-safe method.
```kotlin
sink.emitNext(message, EmitFailureErrorHandler(Duration.ofSeconds(2)))
```
- processWrapper method is executed in a thread provided by scheduler.