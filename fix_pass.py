import base64
import os, subprocess

def decode_secret(reversed_b64):
    b64 = reversed_b64[::-1]
    decoded = base64.b64decode(b64).decode('utf-8')
    if decoded.endswith('\n'):
        decoded = decoded[:-1]
    return decoded

alias_reversed = "KgHdzlXb"
keypass_reversed = "==gC3YGRWl2VPBTZPRHO6lVbnNna0gXT"
storepass_reversed = "==gC3YGRWl2VPBTZPRHO6lVbnNna0gXT"

alias = decode_secret(alias_reversed)
keypass = decode_secret(keypass_reversed)
storepass = decode_secret(storepass_reversed)

print(f"Alias: {alias!r}")
print(f"Keypass: {keypass!r}")

env = os.environ.copy()
def set_secret(name, value):
    subprocess.run(["gh", "secret", "set", name, "-b", value, "-R", "mystxnavadeep/Mystx"], env=env)

set_secret("KEY_ALIAS", alias)
set_secret("KEY_PASSWORD", keypass)
set_secret("KEYSTORE_PASSWORD", storepass)
print("Done fixing passwords!")
