
.PHONY: run
run: export PORT=8080
run:
	mvn -Djetty.http.port=8080 jetty:run

.PHONY: run2
run2: export PORT=8081
run2:
	mvn -Djetty.http.port=8081 jetty:run
