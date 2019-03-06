# SonarQube Analysis

## Run SonarQube

```bash
docker-compose -f ./docker/sonar/docker-compose.yml up -d
```

## Run analysis
Assuming DOCKER_MACHINE_IP is the IP of the docker machine.
```bash
gradle sonarqube -Dsonar.host.url=http://$DOCKER_MACHINE_IP:9000 -Dsonar.verbose=true
```

## See Results

Open a web browser at: 

[http://$DOCKER_MACHINE_IP:9000](http://localhost:9000/)

