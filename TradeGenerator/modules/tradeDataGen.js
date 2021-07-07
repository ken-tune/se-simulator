const stats = require("./stats")
const localMath = require("./localMath")

const lastPriceMap = {}

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
	var newPrice;
	var newTimestamp = Date.now()	
	if(lastPriceMap[stockConfig.stockTicker] === undefined){
		newPrice = stockConfig.meanPrice
	}
	else{
		var timeSinceLastPriceMs = newTimestamp - lastPriceMap[stockConfig.stockTicker].timestamp
		var priceVariance = timeSinceLastPriceMs / (86400 * 1000) * Math.pow(stockConfig.sqrtPriceVariance,2)
		var priceIncrement = stats.getRandomGaussian() * Math.pow(priceVariance,0.5)
		newPrice = lastPriceMap[stockConfig.stockTicker].price + priceIncrement
	}
	lastPriceMap[stockConfig.stockTicker] = {"price":newPrice,"timestamp":newTimestamp}
	var formattedNewPrice = Math.round(newPrice * 10000)/10000
	return {
		ticker:stockConfig.stockTicker,
		price:formattedNewPrice,
		volume:getRandomTradeVolume(stockConfig.minTradeVol,stockConfig.maxTradeVol,powerForVolumeSimulation),
		timestamp: newTimestamp
	};
}

// exports.getRandomPrice = getRandomPrice
// exports.getRandomTradeVolume = getRandomTradeVolume
exports.simulatedTrade = simulatedTrade