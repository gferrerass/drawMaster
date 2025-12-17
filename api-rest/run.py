from app import create_app

app = create_app()

if __name__ == '__main__':
    # Bind to 0.0.0.0 so the Flask app is reachable from outside the container
    app.run(host='0.0.0.0', port=5000)
