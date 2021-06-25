const stats = require("./stats")
const localMath = require("./localMath")

// Simulator config
const powerForVolumeSimulation=-1.1 // Have found this value gives reasonable trade volume simulations when used with a power law distribution (powerDisnSample)

// Get a random number sampled from a Gaussian with mean=mean & variance = sigma ^ 2 & round to 2 dp
function getRandomPrice(mean,sigma){
	return localMath.round(mean + sigma * stats.getRandomGaussian(),2);
}

// Get a random trade volume, between min&maxTradeVol based on a power law distribution with power = powerForVolumeSimulation
function getRandomTradeVolume(minTradeVol,maxTradeVol,powerForVolumeSimulation){
	return Math.round(stats.powerDistnSample(minTradeVol,maxTradeVol,powerForVolumeSimulation))
}

// Return a trade as a map
function simulatedTrade(stockConfig){
	return {
		ticker:stockConfig.stockTicker,
		price:getRandomPrice(stockConfig.meanPrice,stockConfig.sqrtPriceVariance),
		volume:getRandomTradeVolume(stockConfig.minTradeVol,stockConfig.maxTradeVol,powerForVolumeSimulation),
		timestamp:Date.now()
	};
}

// exports.getRandomPrice = getRandomPrice
// exports.getRandomTradeVolume = getRandomTradeVolume
exports.simulatedTrade = simulatedTrade