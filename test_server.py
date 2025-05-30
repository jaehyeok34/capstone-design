from flask import Flask, request

app = Flask(__name__)

@app.route('/test/<string:data>', methods=['POST'])
def home(data):
    print(data)
    json = request.get_json()
    print(type(json), json)

    return 'ok', 200

if __name__ == '__main__':
    app.run(port=2121, debug=True)