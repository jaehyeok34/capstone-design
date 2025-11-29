init:
	# gui 의존성 설치
	cd gui && npm install

	# api gateway 가상환경 생성 및 의존성 설치
	cd api_gateway && python3 -m venv .venv && \
		source .venv/bin/activate && pip install -r requirements.txt

	# convert service 가상환경 생성 및 의존성 설치
	cd services/convert_service && python3 -m venv .venv && \
		source .venv/bin/activate && pip install -r requirements.txt

	# join service 가상환경 생성 및 의존성 설치
	cd services/join_service && python3 -m venv .venv && \
		source .venv/bin/activate && pip install -r requirements.txt

run-gui:
	cd gui && npm run dev

run-api-gateway:
	cd api_gateway && source .venv/bin/activate && python -m app 

run-broker:
	cd message_broker && java -jar app/build/libs/app.jar 

run-convert-service:
	cd services/convert_service && source .venv/bin/activate && python -m main 
	
run-join-service:
	cd services/join_service && source .venv/bin/activate && python -m main 
