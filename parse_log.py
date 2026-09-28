import re

with open("full_log.txt", "r") as f:
    lines = f.readlines()

in_output = False
keystore_lines = []
has_started_keystore = False

for line in lines:
    if "##[endgroup]" in line:
        in_output = True
        continue
    
    if in_output:
        if "KEYSTORE:" in line:
            has_started_keystore = True
            continue
        if "ALIAS:" in line:
            break
        
        if has_started_keystore and 'Z ' in line:
            content = line.split('Z ', 1)[1].strip()
            keystore_lines.append(content)

keystore_reversed = "".join(keystore_lines)
print("Parsed length:", len(keystore_reversed))
import base64
original = keystore_reversed[::-1]
decoded = base64.b64decode(original).decode('utf-8')
if decoded.endswith('\n'): decoded = decoded[:-1]
print("Decoded length:", len(decoded))

import os, subprocess
env = os.environ.copy()
subprocess.run(["gh", "secret", "set", "KEYSTORE_BASE64", "-b", decoded, "-R", "mystxnavadeep/Mystx"], env=env)
print("Done!")
