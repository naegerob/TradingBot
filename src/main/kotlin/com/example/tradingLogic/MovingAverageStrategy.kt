package com.example.tradingLogic

import com.example.data.AlpacaAPI

class MovingAverageStrategy(
    val mIndicators: Indicators,
    val mApiClient: AlpacaAPI
) : TradingStrategy {
    override fun executeAlgorithm() {
        TODO("Not yet implemented")
        mIndicators.calculateIndicators()

/*
        mOriginalPrices.forEachIndexed { index, price ->
            if(mShortSMA[index] > mLongSMA[index] && price < mLowerBollingerBand[index]) {
                // TODO: store position
            }
        }
 */

    }

}