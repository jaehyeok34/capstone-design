FROM python:3.13.3

WORKDIR /server/matching-key

COPY . .

RUN pip install --upgrade pip && \
    pip install -r requirements.txt

EXPOSE 1783

CMD [ "python", "app.py" ]