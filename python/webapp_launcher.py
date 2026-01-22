import threading
from waitress import serve
import rBrowser
from rBrowser import app

# This function will be called from Kotlin
def start_server():
    def run():
        # We bind to localhost only for security within the phone
        serve(app, host='127.0.0.1', port=5000, threads=6)

    t = threading.Thread(target=run)
    t.daemon = True
    t.start()
