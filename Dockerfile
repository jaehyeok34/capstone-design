FROM python:3.13.3

WORKDIR /data_server

COPY . .

RUN pip install --upgrade pip && \
    pip install -r requirements.txt

CMD [ "python", "app.py" ]