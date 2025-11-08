from flask import Flask

from blueprint.app_bp import app_bp

def create_app():
    app = Flask(__name__)
    app.register_blueprint(app_bp, url_prefix='/')

    return app

if __name__ == '__main__':
    app = create_app()
    app.run(debug=True)