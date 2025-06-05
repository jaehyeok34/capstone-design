FROM python:3.13.3

WORKDIR /matching

COPY . .

RUN pip install --upgrade pip && \
    pip install -r requirements.txt

EXPOSE 1785

CMD [ "python", "app.py" ]