import matplotlib.pyplot as plt
import requests

baseUrl = "http://127.0.0.1:8080"
urlIndicators = baseUrl + "/Indicators"
urlOriginal = urlIndicators + "/Original"
urlBands = urlIndicators + "/BollingerBands"
urlMiddle = urlBands + "/Middle"
urlLower = urlBands + "/Lower"
urlUpper = urlBands + "/Upper"
urlRsi = urlIndicators + "/Rsi"

# Data series
original = requests.get(urlOriginal)
sma = requests.get(urlMiddle)
lowerBand = requests.get(urlLower)
upperBand = requests.get(urlUpper)
rsi = requests.get(urlRsi)

# Configuration
windowSma = 20
windowRsi = 14
rsiLow = 30
rsiHigh = 70

# Modifying the data
sma = sma.text.replace("[", "").replace("]", "").replace("'", "").split(",")
sma = [float(value) for value in sma]
lowerBand = lowerBand.text.replace("[", "").replace("]", "").replace("'", "").split(",")
lowerBand = [float(value) for value in lowerBand]
upperBand = upperBand.text.replace("[", "").replace("]", "").replace("'", "").split(",")
upperBand = [float(value) for value in upperBand]
rsi = rsi.text.replace("[", "").replace("]", "").replace("'", "").split(",")
rsi = [float(value) for value in rsi]
original = original.text.replace("[", "").replace("]", "").replace("'", "").split(",")
original = [float(value) for value in original]

x_values = list(range(len(original)))
x_valuesSma = list(range(len(sma)))
x_valuesRsi = list(range(len(rsi)))

rsiLowList = [rsiLow] * len(x_valuesRsi)
rsiHighList = [rsiHigh] * len(x_valuesRsi)
del rsiHighList[:windowSma-windowRsi]
del rsiLowList[:windowSma-windowRsi]

del original[:windowSma]
del rsi[:windowSma-windowRsi]

# Plot the data
# Create a figure with 2 subplots (one above the other)
fig, (ax1, ax2) = plt.subplots(2, 1, figsize=(8, 6))  # (rows, columns)

print("original")
print(original)
print("rsi")
print(rsi)
print("sma")
print(sma)
print("upperBand")
print(upperBand)
print("lowerBand")
print(lowerBand)
print(x_valuesRsi)
print("----")
print(len(original))
print(len(x_valuesRsi))

ax1.plot(x_valuesSma, original, label='original', marker='o', linestyle='-', color="red")
ax1.plot(x_valuesSma, sma, label='mva', marker='x', linestyle='--', color="blue")
ax1.plot(x_valuesSma, lowerBand, label='lower', marker='x', linestyle='--', color="green")
ax1.plot(x_valuesSma, upperBand, label='upper', marker='x', linestyle='--', color="green")
ax1.set_title("Stock")
ax1.set_xlabel("Index")
ax1.set_ylabel("Dollar [$]")

ax2.set_ylim([0, 100])
ax2.plot(x_valuesSma, rsi, label='RSI', marker='x', linestyle='--', color="blue")
ax2.plot(x_valuesSma, rsiLowList, linestyle='-', linewidth=1.0, color="cornflowerblue")
ax2.plot(x_valuesSma, rsiHighList, linestyle='-', linewidth=1.0, color="cornflowerblue")
ax2.set_title("RSI")
ax2.set_xlabel("Index")
ax2.set_ylabel("RSI [%]")
# Adjust layout to prevent overlap
plt.tight_layout()
plt.legend()

# Show the figure with both subplots
plt.show()

