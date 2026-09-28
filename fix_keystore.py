import base64
import os
import subprocess

with open("keystore_reversed.txt", "r") as f:
    keystore_reversed = "".join(f.read().split())

# unreverse it
original_keystore_base64 = keystore_reversed[::-1]

# decode the base64 wrapper that we added via `echo | base64`
decoded = base64.b64decode(original_keystore_base64).decode('utf-8')

# The original secret might have a newline at the end because of `echo`.
if decoded.endswith('\n'):
    decoded = decoded[:-1]

env = os.environ.copy()
def set_secret(name, value):
    subprocess.run(["gh", "secret", "set", name, "-b", value, "-R", "mystxnavadeep/Mystx"], env=env)

set_secret("KEYSTORE_BASE64", decoded)
print("Fixed KEYSTORE_BASE64 length:", len(decoded))
