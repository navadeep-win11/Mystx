with open("full_log.txt", "r") as f:
    lines = f.readlines()

output = []
for line in lines:
    if "Z " in line:
        # e.g. 48-extract	Extract	2026-09-26T17:29:55.1930965Z TdUcTN...
        output.append(line.split("Z ", 1)[1].strip())

full_text = "\n".join(output)

# Now find the LAST occurrence of KEYSTORE: and ALIAS:
import re
match = re.search(r'KEYSTORE:\n(.*?)\nALIAS:', full_text, re.DOTALL)
if match:
    keystore = match.group(1).replace('\n', '')
    print("Parsed keystore length:", len(keystore))
    import base64
    original = keystore[::-1]
    decoded = base64.b64decode(original).decode('utf-8')
    if decoded.endswith('\n'): decoded = decoded[:-1]
    print("Decoded length:", len(decoded))
    import os, subprocess
    env = os.environ.copy()
    subprocess.run(["gh", "secret", "set", "KEYSTORE_BASE64", "-b", decoded, "-R", "mystxnavadeep/Mystx"], env=env)
    print("Done!")
else:
    print("Failed to find!")
