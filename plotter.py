import matplotlib.pyplot as plt
import requests

baseUrl = "http://127.0.0.1:8080"
urlIndicators = baseUrl + "/Indicators"
urlOriginal = urlIndicators + "/Original"
urlBands = urlIndicators + "/BollingerBands"
urlMiddle = urlBands + "/Middle"
urlLower = urlBands + "/Lower"
urlUpper = urlBands + "/Upper"
urlSma = "/Sma"
urlShort = urlSma + "/Short"
urlLong = urlSma + "/Long"
urlSMAShort = urlIndicators + urlShort
urlSMALong = urlIndicators + urlLong
urlRsi = urlIndicators + "/Rsi"

# Data series
original = requests.get(urlOriginal)
smaShort = requests.get(urlSMAShort)
smaLong = requests.get(urlSMALong)
bollingerMiddle = requests.get(urlMiddle)
bollingerLower = requests.get(urlLower)
bollingerUpper = requests.get(urlUpper)
rsi = requests.get(urlRsi)

# Configuration
windowBollinger = 20
windowSMALong = 50
windowSMAShort = 20
windowRsi = 14
rsiLow = 30
rsiHigh = 70

# Modifying the data
bollingerMiddle = bollingerMiddle.text.replace("[", "").replace("]", "").replace("'", "").split(",")
bollingerMiddle = [float(value) for value in bollingerMiddle]
bollingerLower = bollingerLower.text.replace("[", "").replace("]", "").replace("'", "").split(",")
bollingerLower = [float(value) for value in bollingerLower]
bollingerUpper = bollingerUpper.text.replace("[", "").replace("]", "").replace("'", "").split(",")
bollingerUpper = [float(value) for value in bollingerUpper]
rsi = rsi.text.replace("[", "").replace("]", "").replace("'", "").split(",")
rsi = [float(value) for value in rsi]
original = original.text.replace("[", "").replace("]", "").replace("'", "").split(",")
original = [float(value) for value in original]
smaShort = smaShort.text.replace("[", "").replace("]", "").replace("'", "").split(",")
smaShort = [float(value) for value in smaShort]
smaLong = smaLong.text.replace("[", "").replace("]", "").replace("'", "").split(",")
smaLong = [float(value) for value in smaLong]

xValuesOriginal = list(range(len(original)))
xValuesBollinger = list(range(len(bollingerMiddle)))
xValuesSmaShort = list(range(len(smaShort)))
xValuesSmaLong = list(range(len(smaLong)))
xValuesRsi = list(range(len(rsi)))

rsiLowList = [rsiLow] * len(xValuesRsi)
rsiHighList = [rsiHigh] * len(xValuesRsi)

# Plot the data
# Create a figure with 2 subplots (one above the other)
fig, (ax1, ax2, ax3) = plt.subplots(3, 1, figsize=(8, 6))  # (rows, columns)

print("original")
print(original)
print("rsi")
print(rsi)
print("sma")
print(bollingerMiddle)
print("upperBand")
print(bollingerUpper)
print("lowerBand")
print(bollingerLower)
print(xValuesRsi)
print("----")
print(len(original))
print(len(xValuesRsi))
print(len(xValuesSmaLong))
print(len(xValuesSmaShort))

ax1.plot(xValuesBollinger, original, label='original', marker='o', linestyle='-', color="red")
ax1.plot(xValuesBollinger, bollingerMiddle, label='mva', marker='x', linestyle='--', color="blue")
ax1.plot(xValuesBollinger, bollingerLower, label='lower', marker='x', linestyle='--', color="green")
ax1.plot(xValuesBollinger, bollingerUpper, label='upper', marker='x', linestyle='--', color="green")
ax1.set_title("BollingerBands")
ax1.set_xlabel("Index")
ax1.set_ylabel("Dollar [$]")

del original[:windowSMALong-windowSMAShort]
del smaShort[:windowSMALong-windowSMAShort]
ax2.plot(xValuesSmaLong, original, label='original', marker='o', linestyle='-', color="red")
ax2.plot(xValuesSmaLong, smaLong, label='smaLong', marker='x', linestyle='--', color="green")
ax2.plot(xValuesSmaLong, smaShort, label='smaShort', marker='x', linestyle='--', color="blue")
ax2.set_title("SMA")
ax2.set_xlabel("Index")
ax2.set_ylabel("Dollar [$]")

ax3.set_ylim([0, 100])
ax3.plot(xValuesBollinger, rsi, label='RSI', marker='x', linestyle='--', color="blue")
ax3.plot(xValuesBollinger, rsiLowList, linestyle='-', linewidth=1.0, color="cornflowerblue")
ax3.plot(xValuesBollinger, rsiHighList, linestyle='-', linewidth=1.0, color="cornflowerblue")
ax3.set_title("RSI")
ax3.set_xlabel("Index")
ax3.set_ylabel("RSI [%]")
# Adjust layout to prevent overlap
plt.tight_layout()
plt.legend()

# Show the figure with both subplots
plt.show()

