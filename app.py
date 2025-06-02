from flask import Flask
from controller.matching_key_controller import matching_key_bp


app = Flask(__name__)
app.register_blueprint(matching_key_bp)

@app.route('/')
def home():
    return "matching-key-server"
    

if __name__ == '__main__':
    port = 1783
    callback_url = f'http://localhost:{port}/generate-matching-key'

#     ok = subscribe(
#         topic='matching-key.generate.request',
#         callback_url=callback_url,
#         count=3,
#         interval=5
#     )
#     if not ok:
#         exit(1)

    # app.run(port=port, debug=True)
    app.run(port=port)