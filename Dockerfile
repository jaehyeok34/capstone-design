FROM python:3.13.3

WORKDIR /pseudonymization_server

COPY . .

RUN pip install --upgrade pip && \
    pip install -r requirements.txt

CMD [ "python", "app.py" ]