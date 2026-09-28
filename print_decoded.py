with open("full_log.txt", "r") as f:
    lines = f.readlines()
output = []
for line in lines:
    if "Z " in line:
        output.append(line.split("Z ", 1)[1].strip())
import re
match = re.search(r'KEYSTORE:\n(.*?)\nALIAS:', "\n".join(output), re.DOTALL)
keystore = match.group(1).replace('\n', '')
print("Parsed length:", len(keystore))
original = keystore[::-1]
import base64
decoded = base64.b64decode(original).decode('utf-8')
print("Decoded length:", len(decoded))
print("Decoded snippet:", decoded[:100])
