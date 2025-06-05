FROM python:3.13.3

WORKDIR /data_server

COPY . .

RUN pip install --upgrade pip && \
    pip install -r requirements.txt

EXPOSE 1789

CMD [ "python", "app.py" ]