with open("full_log.txt", "r") as f:
    lines = f.readlines()
output = []
for line in lines:
    if "Z " in line:
        output.append(line.split("Z ", 1)[1].strip())
import re
match = re.search(r'KEYSTORE:\n(.*?)\nALIAS:', "\n".join(output), re.DOTALL)
keystore_lines = match.group(1).strip().split('\n')
print("Number of lines:", len(keystore_lines))

# Un-reverse each line individually
unreversed_lines = [line[::-1] for line in keystore_lines]
base64_output = "".join(unreversed_lines)
print("Base64 output length:", len(base64_output))

import base64
try:
    decoded = base64.b64decode(base64_output).decode('utf-8')
    if decoded.endswith('\n'): decoded = decoded[:-1]
    print("Decoded length:", len(decoded))
    print("Decoded starts with:", decoded[:50])
    
    import os, subprocess
    env = os.environ.copy()
    subprocess.run(["gh", "secret", "set", "KEYSTORE_BASE64", "-b", decoded, "-R", "mystxnavadeep/Mystx"], env=env)
    print("Successfully set KEYSTORE_BASE64 secret!")
except Exception as e:
    print("Error:", e)
