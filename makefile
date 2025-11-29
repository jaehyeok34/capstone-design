init:
	# gui 의존성 설치
	cd gui && npm install && npm run build

	# message broker 빌드
	cd message_broker && ./gradlew shadowJar

	# api gateway 가상환경 생성 및 의존성 설치
	cd api_gateway && python3 -m venv .venv && \
		source .venv/bin/activate && pip install -r requirements.txt

	# convert service 가상환경 생성 및 의존성 설치
	cd services/convert_service && python3 -m venv .venv && \
		source .venv/bin/activate && pip install -r requirements.txt

	# join service 가상환경 생성 및 의존성 설치
	cd services/join_service && python3 -m venv .venv && \
		source .venv/bin/activate && pip install -r requirements.txt

# gui 실행
g: run.gui
run.gui:
	cd gui && npm run preview

# api gateway 실행
a: run.api.gateway
run.api.gateway:
	cd api_gateway && source .venv/bin/activate && python -m app 

# message broker 실행
b: run.message.broker
m: run.message.broker
run.message.broker:
	cd message_broker && java -jar app/build/libs/app.jar 

# convert service 실행
c: run.convert.service
run.convert.service:
	cd services/convert_service && source .venv/bin/activate && python -m main 

# join service 실행
j: run.join.service
run.join.service:
	cd services/join_service && source .venv/bin/activate && python -m main 
