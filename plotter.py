import matplotlib.pyplot as plt

# Data series
original = [227.03, 226.8, 225.45, 225.87, 226.0517, 221.86, 221.31, 220.17, 220.235, 221.21, 221.1203, 222.66, 221.8988, 222.64, 221.921, 222.0501, 222.2, 222.78, 222.46, 222.0, 221.15, 222.95, 222.935, 221.935, 222.7299, 222.43, 223.3889, 223.77, 223.5002, 223.35, 223.3607, 223.18, 222.82, 222.62, 223.2, 224.14, 224.7495, 225.18, 225.3242, 224.04, 223.695, 223.0743, 222.75, 223.65, 223.3584, 223.45, 223.255, 223.24, 223.72, 224.06, 225.25, 225.09, 225.76, 224.51, 223.065, 222.73, 222.4, 222.05, 222.56, 222.79, 222.56, 222.47, 222.15, 222.17, 220.89, 221.51, 221.8, 222.04, 223.26, 225.66, 227.79, 228.445, 230.15, 231.435, 231.58, 229.94, 230.04, 229.8, 229.7745, 229.79, 229.27, 231.67, 230.93, 230.66, 230.3, 234.69, 238.84, 239.72, 238.73, 238.81, 239.165, 238.24, 238.15, 237.5, 237.6, 238.35, 237.2, 237.15, 237.47, 235.65, 234.58, 235.79, 237.7894, 236.54, 236.03, 237.0, 238.08, 239.35, 239.25, 238.6209, 238.8001, 239.2015, 238.97, 238.88, 239.35, 238.7, 238.95, 239.46, 238.06, 238.78, 238.395, 239.645, 239.861, 237.56, 233.99, 245.03, 245.0, 244.75, 245.61, 245.31, 245.91, 247.16, 246.79, 241.605, 240.925, 238.93, 238.31, 235.26, 234.11, 235.84, 235.25, 235.2, 234.9202, 233.92]

sma = [222.6, 222.4, 222.28, 222.08, 221.92, 221.94, 222.05, 222.23, 222.39, 222.5, 222.61, 222.64, 222.68, 222.68, 222.75, 222.85, 222.98, 223.1, 223.24, 223.34, 223.47, 223.48, 223.47, 223.55, 223.58, 223.64, 223.63, 223.6, 223.61, 223.65, 223.74, 223.84, 223.99, 224.08, 224.07, 224.0, 223.89, 223.73, 223.59, 223.53, 223.47, 223.44, 223.41, 223.34, 223.21, 223.12, 223.04, 222.98, 222.96, 223.04, 223.17, 223.34, 223.55, 223.9, 224.33, 224.69, 225.07, 225.46, 225.82, 226.17, 226.5, 226.96, 227.4, 227.83, 228.3, 228.96, 229.81, 230.69, 231.47, 232.12, 232.69, 233.18, 233.58, 233.88, 234.19, 234.61, 234.96, 235.33, 235.72, 236.01, 236.28, 236.48, 236.82, 237.12, 237.4, 237.52, 237.48, 237.46, 237.49, 237.48, 237.46, 237.51, 237.55, 237.62, 237.71, 237.73, 237.81, 237.93, 237.96, 238.11, 238.3, 238.5, 238.6, 238.65, 238.55, 238.95, 239.3, 239.57, 239.89, 240.22, 240.58, 240.97, 241.36, 241.5, 241.58, 241.59, 241.56, 241.35, 241.15, 241.0, 240.85, 240.63, 240.38, 240.2]

lowerBand = [218.86, 219.19, 219.37, 219.68, 220.32, 220.32, 220.34, 220.59, 220.93, 221.09, 221.3, 221.31, 221.39, 221.39, 221.49, 221.49, 221.43, 221.28, 221.2, 221.36, 221.76, 221.77, 221.75, 221.98, 222.05, 222.2, 222.18, 222.15, 222.16, 222.19, 222.13, 222.15, 222.17, 222.36, 222.33, 222.17, 221.97, 221.75, 221.69, 221.61, 221.51, 221.44, 221.35, 221.22, 220.83, 220.64, 220.49, 220.4, 220.4, 220.25, 219.82, 219.35, 218.66, 217.93, 217.51, 217.49, 217.59, 217.84, 218.1, 218.4, 218.8, 219.18, 219.76, 220.46, 221.59, 222.46, 222.83, 223.4, 224.21, 224.7, 224.95, 225.34, 225.58, 225.77, 226.0, 226.47, 227.02, 227.71, 228.49, 229.31, 230.28, 230.86, 231.79, 232.95, 234.56, 234.96, 234.98, 235.02, 234.98, 234.99, 235.02, 234.97, 234.95, 234.95, 234.94, 234.94, 234.98, 235.03, 235.07, 235.4, 236.12, 236.58, 236.62, 236.84, 236.06, 235.28, 234.81, 234.49, 234.17, 234.07, 233.99, 233.82, 233.85, 234.07, 234.21, 234.24, 234.16, 233.5, 232.8, 232.38, 231.94, 231.4, 230.82, 230.3]

upperBand = [226.35, 225.62, 225.2, 224.49, 223.53, 223.57, 223.77, 223.88, 223.86, 223.92, 223.93, 223.98, 223.98, 223.98, 224.02, 224.22, 224.54, 224.93, 225.29, 225.33, 225.19, 225.2, 225.2, 225.13, 225.12, 225.09, 225.09, 225.06, 225.07, 225.12, 225.36, 225.54, 225.82, 225.81, 225.82, 225.84, 225.82, 225.72, 225.5, 225.46, 225.44, 225.45, 225.48, 225.47, 225.6, 225.61, 225.6, 225.57, 225.53, 225.84, 226.53, 227.34, 228.45, 229.88, 231.16, 231.9, 232.56, 233.09, 233.55, 233.95, 234.21, 234.75, 235.05, 235.21, 235.02, 235.47, 236.8, 237.99, 238.74, 239.55, 240.44, 241.03, 241.59, 242.0, 242.39, 242.76, 242.91, 242.96, 242.96, 242.72, 242.29, 242.11, 241.86, 241.3, 240.25, 240.09, 239.99, 239.91, 240.01, 239.98, 239.91, 240.06, 240.16, 240.3, 240.49, 240.53, 240.65, 240.84, 240.86, 240.83, 240.49, 240.43, 240.59, 240.47, 241.05, 242.63, 243.8, 244.66, 245.62, 246.38, 247.18, 248.13, 248.88, 248.94, 248.96, 248.95, 248.97, 249.21, 249.51, 249.63, 249.77, 249.87, 249.95, 250.11]

rsi = [34.48, 34.84, 35.3, 37.12, 36.51, 35.6, 33.92, 40.34, 40.3, 37.93, 40.91, 40.13, 43.83, 45.27, 44.4, 43.89, 43.94, 43.25, 41.85, 41.05, 44.37, 49.34, 52.31, 54.35, 55.05, 48.05, 46.34, 43.36, 41.85, 47.34, 45.83, 46.41, 45.3, 45.21, 48.71, 51.09, 58.37, 57.14, 60.86, 51.82, 43.73, 42.09, 40.48, 38.78, 42.56, 44.23, 42.89, 42.34, 40.39, 40.57, 33.44, 39.03, 41.51, 43.55, 52.62, 64.64, 71.55, 73.27, 77.16, 79.57, 79.83, 69.16, 69.43, 67.9, 67.73, 67.78, 63.99, 71.83, 66.99, 65.26, 62.93, 74.77, 80.96, 81.97, 77.02, 77.14, 77.7, 72.72, 72.24, 68.68, 68.93, 70.84, 64.32, 64.05, 65.08, 55.34, 50.55, 55.27, 61.76, 56.27, 54.15, 57.43, 60.79, 64.36, 63.87, 60.72, 61.3, 62.65, 61.33, 60.79, 62.63, 58.54, 59.63, 61.84, 53.23, 56.58, 54.34, 59.89, 60.78, 48.48, 36.23, 65.38, 65.29, 64.51, 66.01, 64.98, 66.12, 68.41, 66.97, 50.77, 49.09, 44.45, 43.09, 37.07, 35.08, 40.27, 39.12, 39.02, 38.42, 36.27]

# X-axis values
x_values = list(range(len(original)))
x_valuesSma = list(range(len(sma)))
x_valuesRsi = list(range(len(rsi)))
windowSma = 20
windowRsi = 14
rsiLow = 30
rsiHigh = 70
# Plot the data
# Create a figure with 2 subplots (one above the other)
fig, (ax1, ax2) = plt.subplots(2, 1, figsize=(8, 6))  # (rows, columns)

print(len(original))
print(len(sma))
ax1.plot(x_values[windowSma:], original[windowSma:], label='original', marker='o', linestyle='-', color="red")
ax1.plot(x_valuesSma, sma, label='mva', marker='x', linestyle='--', color="blue")
ax1.plot(x_valuesSma, lowerBand, label='lower', marker='x', linestyle='--', color="green")
ax1.plot(x_valuesSma, upperBand, label='upper', marker='x', linestyle='--', color="green")
ax1.set_title("Stock")
ax1.set_xlabel("Index")
ax1.set_ylabel("Dollar [$]")

rsiLowList = [rsiLow] * len(x_valuesRsi)
rsiHighList = [rsiHigh] * len(x_valuesRsi)
ax2.set_ylim([0, 100])
ax2.plot(x_valuesRsi, rsi, label='RSI', marker='x', linestyle='--', color="blue")
ax2.plot(x_valuesRsi, rsiLowList, linestyle='-', linewidth=1.0, color="cornflowerblue")
ax2.plot(x_valuesRsi, rsiHighList, linestyle='-', linewidth=1.0, color="cornflowerblue")
ax2.set_title("RSI")
ax2.set_xlabel("Index")
ax2.set_ylabel("RSI [%]")
# Adjust layout to prevent overlap
plt.tight_layout()
plt.legend()

# Show the figure with both subplots
plt.show()

