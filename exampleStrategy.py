import pandas as pd
import numpy as np

class TradingBot:
    def __init__(self, data, short_window=20, long_window=50, bb_window=20, support_resistance_window=14):
        """
        Initializes the TradingBot.

        Parameters:
            data (pd.DataFrame): Historical market data with 'Close' prices.
            short_window (int): Window for short-term moving average.
            long_window (int): Window for long-term moving average.
            bb_window (int): Window for Bollinger Bands.
            support_resistance_window (int): Window for identifying support and resistance levels.
        """
        self.data = data
        self.short_window = short_window
        self.long_window = long_window
        self.bb_window = bb_window
        self.support_resistance_window = support_resistance_window
        self.positions = []
        self.trades = []

    def calculate_indicators(self):
        """Calculates moving averages, Bollinger Bands, and support/resistance levels."""
        self.data['SMA_Short'] = self.data['Close'].rolling(window=self.short_window).mean()
        self.data['SMA_Long'] = self.data['Close'].rolling(window=self.long_window).mean()
        self.data['BB_Middle'] = self.data['Close'].rolling(window=self.bb_window).mean()
        self.data['BB_Upper'] = self.data['BB_Middle'] + 2 * self.data['Close'].rolling(window=self.bb_window).std()
        self.data['BB_Lower'] = self.data['BB_Middle'] - 2 * self.data['Close'].rolling(window=self.bb_window).std()
        
        # Calculate support and resistance levels
        self.data['Support'] = self.data['Close'].rolling(window=self.support_resistance_window).min()
        self.data['Resistance'] = self.data['Close'].rolling(window=self.support_resistance_window).max()

    def simulate_trading(self):
        """Simulates trading using the strategy."""
        self.calculate_indicators()
        position = None

        for index, row in self.data.iterrows():
            # Entry conditions
            if position is None:
                if row['SMA_Short'] > row['SMA_Long'] and row['Close'] < row['BB_Lower']:
                    position = {
                        'entry_price': row['Close'],
                        'entry_index': index,
                        'stop_loss': row['Support'] * 0.98 if not pd.isna(row['Support']) else row['Close'] * 0.98,
                        'take_profit': row['Resistance'] * 0.98 if not pd.isna(row['Resistance']) else row['Close'] * 1.02
                    }
                    self.positions.append(position)

            # Exit conditions
            if position:
                if row['Close'] <= position['stop_loss'] or row['Close'] >= position['take_profit']:
                    trade = {
                        'entry_price': position['entry_price'],
                        'exit_price': row['Close'],
                        'entry_index': position['entry_index'],
                        'exit_index': index,
                        'stop_loss': position['stop_loss'],
                        'take_profit': position['take_profit']
                    }
                    self.trades.append(trade)
                    position = None

    def get_results(self):
        """Returns the trades and performance metrics."""
        total_profit = sum([trade['exit_price'] - trade['entry_price'] for trade in self.trades])
        return {
            'trades': self.trades,
            'total_profit': total_profit
        }

# Example Usage
if __name__ == "__main__":
    # Load historical data (replace with your data source)
    data = pd.DataFrame({
        'Close': np.random.rand(100) * 100  # Replace with actual historical data
    })
    

