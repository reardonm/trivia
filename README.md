# trivia

## Running locally
Run the server on port 8080 (no cors)
```
./gradlew server:run
```
Run client on port 3000
```
cd client
npm start
```


## Building an executable jar
NOTE: Build requires running docker machine for Redis

```
./gradlew assembleServerAndClient
```

Execute jar

```
java -jar server/build/libs/server-0.1-all.jar
```
---

Questions from https://opentdb.com/api_config.php
