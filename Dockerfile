FROM python:3.13.3

WORKDIR /matching_key_server

COPY . .

RUN pip install --upgrade pip && \
    pip install -r requirements.txt

EXPOSE 1783

CMD [ "python", "app.py" ]