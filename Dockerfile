FROM python:3.13.3

WORKDIR /server/pii_detection

COPY . .

RUN pip install --upgrade pip && \
    pip install -r requirements.txt

EXPOSE 1782

CMD [ "python", "app.py" ]