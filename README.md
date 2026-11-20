# MP4

## Build

```
javac -d bin src/*.java
```

## Run

Start the Talker first, then the Listener in a second terminal.

```
java -cp bin Talker 5000 5001
java -cp bin Listener 5001 5000 127.0.0.1
```

To drop half the ACKs, add `true` to the Listener:

```
java -cp bin Listener 5001 5000 127.0.0.1 true
```
