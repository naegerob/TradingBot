import matplotlib.pyplot as plt

# Data series
original = [434.56, 434.39, 434.39, 435.689, 434.26, 412.25, 413.8926, 413.6326, 417.8292, 420.39, 417.0365, 424.08, 424.52, 422.72, 421.8925, 420.2101, 419.85, 419.22, 419.19, 419.86, 417.4, 418.0, 419.815, 421.6079, 423.255, 418.13, 419.765, 415.1, 416.17, 415.65, 415.67, 413.7817, 412.0, 411.96, 412.82, 413.1, 415.7, 413.29, 413.5, 411.86, 411.57, 412.3299, 410.6001, 412.37, 412.05, 412.1505, 412.2073, 411.8559, 414.9, 414.86, 415.09, 416.03, 415.71, 416.9363, 413.29, 413.2, 410.72, 408.1, 407.95, 406.535, 406.65, 406.35, 406.15, 405.47, 393.85, 387.54, 389.38, 391.51, 394.49, 404.0665, 403.3, 399.75, 395.02, 394.37, 389.52, 397.06, 398.2057, 398.2603, 397.4, 396.41, 400.0, 400.1, 398.64, 397.05, 396.61, 391.7, 389.82, 390.232, 394.6388, 393.55, 398.4, 398.09, 396.95, 396.79, 396.17, 396.6509, 395.22, 396.35, 397.68, 396.58, 396.14, 393.37, 391.498, 388.32, 388.2464, 389.02, 390.04, 388.03, 401.65, 407.5, 403.8, 405.25, 399.53, 397.47, 399.0, 401.9, 406.88, 387.79, 391.23, 400.95, 407.24, 402.5841, 406.4322, 400.48, 399.6101, 400.5122, 401.9525, 400.9, 400.16, 399.88, 401.0, 400.86, 399.45, 414.82, 415.6556, 418.64, 419.13, 411.48, 406.5533, 404.58, 404.61, 404.6, 403.3422, 402.02]

mva = [0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 419.47, 418.89, 418.34, 417.74, 417.16, 416.61, 416.6, 416.56, 416.52, 416.44, 416.31, 416.26, 416.06, 415.84, 415.69, 415.48, 415.3, 415.07, 414.79, 414.51, 414.18, 413.91, 413.62, 413.28, 412.88, 412.14, 411.38, 410.62, 410.03, 409.48, 409.19, 408.89, 408.53, 408.11, 407.67, 407.09, 406.69, 406.25, 405.87, 405.47, 405.09, 404.8, 404.49, 404.19, 403.81, 403.42, 402.91, 402.35, 401.81, 401.3, 400.77, 400.35, 399.91, 399.44, 398.93, 398.5, 398.09, 397.7, 397.41, 397.15, 396.9, 396.64, 396.32, 395.95, 395.52, 395.38, 395.42, 395.44, 395.35, 395.53, 395.61, 395.63, 395.76, 395.88, 395.95, 396.19, 396.31, 396.53, 396.27, 396.11, 396.23, 396.41, 396.47, 396.66, 396.75, 396.82, 397.04, 397.35, 397.61, 397.75, 397.91, 397.98, 398.05, 398.11, 398.56, 399.05, 399.6, 400.19, 400.57, 400.79, 400.99, 401.21, 401.49, 401.78, 402.12]

lowerBand = [0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 406.02, 406.13, 406.43, 406.76, 407.68, 408.73, 408.72, 408.6, 408.47, 408.39, 408.34, 408.29, 408.49, 408.78, 408.97, 409.02, 408.99, 408.78, 408.28, 407.81, 407.26, 406.68, 406.14, 405.71, 405.41, 403.25, 399.82, 397.48, 395.69, 394.49, 394.24, 393.97, 393.44, 392.48, 391.52, 390.07, 389.5, 389.11, 388.71, 388.29, 387.8, 387.56, 387.37, 387.09, 386.77, 386.45, 385.79, 385.02, 384.36, 384.23, 384.09, 384.31, 384.65, 385.03, 385.64, 386.01, 386.51, 386.82, 387.05, 387.36, 387.58, 387.85, 388.04, 388.17, 388.0, 387.54, 387.71, 387.78, 387.44, 387.38, 387.05, 387.02, 386.73, 386.77, 386.85, 387.27, 387.22, 386.87, 386.25, 385.98, 385.98, 385.66, 385.61, 385.38, 385.41, 385.45, 385.73, 386.17, 386.63, 386.78, 387.0, 387.02, 387.06, 387.11, 386.4, 385.8, 385.03, 384.48, 384.52, 384.66, 384.88, 385.13, 385.57, 386.18, 387.13]

upperBand = [0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 432.91, 431.65, 430.25, 428.73, 426.65, 424.49, 424.49, 424.52, 424.56, 424.49, 424.27, 424.23, 423.62, 422.9, 422.41, 421.93, 421.61, 421.37, 421.31, 421.21, 421.1, 421.14, 421.1, 420.84, 420.34, 421.03, 422.93, 423.75, 424.37, 424.48, 424.15, 423.8, 423.63, 423.74, 423.82, 424.11, 423.88, 423.39, 423.04, 422.66, 422.37, 422.03, 421.61, 421.29, 420.84, 420.39, 420.03, 419.68, 419.27, 418.38, 417.45, 416.4, 415.16, 413.84, 412.22, 411.0, 409.67, 408.58, 407.77, 406.95, 406.23, 405.43, 404.59, 403.73, 403.04, 403.22, 403.13, 403.09, 403.26, 403.67, 404.18, 404.24, 404.8, 404.98, 405.06, 405.11, 405.4, 406.19, 406.28, 406.24, 406.47, 407.15, 407.33, 407.94, 408.09, 408.2, 408.36, 408.52, 408.6, 408.73, 408.82, 408.93, 409.04, 409.1, 410.72, 412.29, 414.16, 415.91, 416.63, 416.93, 417.11, 417.28, 417.4, 417.38, 417.12]

# X-axis values
x_values = list(range(len(original)))
window = 20
# Plot the data
plt.figure(figsize=(10, 5))
plt.plot(x_values[window:], original[window:], label='original', marker='o', linestyle='-', color="red")
plt.plot(x_values[window:], mva[window:], label='mva', marker='x', linestyle='--', color="blue")
plt.plot(x_values[window:], lowerBand[window:], label='lower', marker='x', linestyle='--', color="green")
plt.plot(x_values[window:], upperBand[window:], label='upper', marker='x', linestyle='--', color="green")

# Labels and title
plt.xlabel('Index')
plt.ylabel('Value')
plt.title('Data Plot')
plt.legend()

# Show the plot
plt.show()
