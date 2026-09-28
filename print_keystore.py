with open("full_log.txt", "r") as f:
    lines = f.readlines()
output = []
for line in lines:
    if "Z " in line:
        output.append(line.split("Z ", 1)[1].strip())
import re
match = re.search(r'KEYSTORE:\n(.*?)\nALIAS:', "\n".join(output), re.DOTALL)
keystore = match.group(1).replace('\n', '')
print("Keystore length:", len(keystore))
print("Keystore snippet (end):", keystore[-100:])
