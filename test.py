import base64
original = '==gC9EEeKNUSndkdwFVa5FTZOZWbwUTcvcFTZVDM'[::-1]
decoded = base64.b64decode(original).decode('utf-8')
print("Decoded:", decoded)
