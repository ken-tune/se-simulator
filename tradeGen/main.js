
var stockConfigList=[];
stockConfigList[0] = stockConfig("A.COM",32,4,50,10000); 
stockConfigList[1] = stockConfig("B.COM",124,17,500,50000); 
stockConfigList[2] = stockConfig("C.COM",75,7,700,100000); 

const powerForVolumeSimulation=-1.1 // Have found this value gives reasonable trade volume simulations when used with a power law distribution (powerDisnSample)

// Utiltiy function to create a stock config object
function stockConfig(stockTicker,meanPrice,sqrtPriceVariance,minTradeVol,maxTradeVol){
	return {
		stockTicker:stockTicker,
		meanPrice:meanPrice,
		sqrtPriceVariance:sqrtPriceVariance,
		minTradeVol:minTradeVol,
		maxTradeVol:maxTradeVol
	}
}
// Return a trade as a map
function getRandomTrade(){
	stock = stockConfigList[getRandomInt(stockConfigList.length)]
	return {
		ticker:stock.stockTicker,
		price:getRandomPrice(stock.meanPrice,stock.sqrtPriceVariance),
		volume:getRandomTradeVolume(stock.minTradeVol,stock.maxTradeVol),
		timestamp:Date.now()
	};
}

// Get a random int between 0 and max - 1
function getRandomInt(max) {
  return Math.floor(Math.random() * max);
}

// Randomly sample from a (0,1) Gaussian
function getRandomGaussian(){
	return NormSInv(Math.random());
}

// Get a random number sampled from a Gaussian with mean=mean & variance = sigma ^ 2 & round to 2 dp
function getRandomPrice(mean,sigma){
	return round(mean + sigma * getRandomGaussian(),2);
}

// Get a random trade volume, between min&maxTradeVol based on a power law distribution with power = powerForVolumeSimulation
function getRandomTradeVolume(minTradeVol,maxTradeVol){
	return Math.round(powerDistnSample(minTradeVol,maxTradeVol,powerForVolumeSimulation))
}

// Round value to a specific number of decimal places
function round(value,places){
	return Math.round(value * Math.pow(10,places)) / Math.pow(10,places) ;
}
// from https://stackoverflow.com/questions/8816729/javascript-equivalent-for-inverse-normal-function-eg-excels-normsinv-or-nor
// Used by getRandomGaussian
function NormSInv(p) {
    var a1 = -39.6968302866538, a2 = 220.946098424521, a3 = -275.928510446969;
    var a4 = 138.357751867269, a5 = -30.6647980661472, a6 = 2.50662827745924;
    var b1 = -54.4760987982241, b2 = 161.585836858041, b3 = -155.698979859887;
    var b4 = 66.8013118877197, b5 = -13.2806815528857, c1 = -7.78489400243029E-03;
    var c2 = -0.322396458041136, c3 = -2.40075827716184, c4 = -2.54973253934373;
    var c5 = 4.37466414146497, c6 = 2.93816398269878, d1 = 7.78469570904146E-03;
    var d2 = 0.32246712907004, d3 = 2.445134137143, d4 = 3.75440866190742;
    var p_low = 0.02425, p_high = 1 - p_low;
    var q, r;
    var retVal;

    if ((p < 0) || (p > 1))
    {
        alert("NormSInv: Argument out of range.");
        retVal = 0;
    }
    else if (p < p_low)
    {
        q = Math.sqrt(-2 * Math.log(p));
        retVal = (((((c1 * q + c2) * q + c3) * q + c4) * q + c5) * q + c6) / ((((d1 * q + d2) * q + d3) * q + d4) * q + 1);
    }
    else if (p <= p_high)
    {
        q = p - 0.5;
        r = q * q;
        retVal = (((((a1 * r + a2) * r + a3) * r + a4) * r + a5) * r + a6) * q / (((((b1 * r + b2) * r + b3) * r + b4) * r + b5) * r + 1);
    }
    else
    {
        q = Math.sqrt(-2 * Math.log(1 - p));
        retVal = -(((((c1 * q + c2) * q + c3) * q + c4) * q + c5) * q + c6) / ((((d1 * q + d2) * q + d3) * q + d4) * q + 1);
    }

    return retVal;
}

// See https://mathworld.wolfram.com/RandomNumber.html
function powerDistnSample(min,max,power){
	return Math.pow((Math.pow(max,power + 1) - Math.pow(min,power + 1)) * Math.random() + Math.pow(min,power + 1),(1/(power + 1)));
}

// function nextIteration(){
// 	console.log(getRandomTrade());
// 	setTimeout(nextIteration,1000);
// }

// nextIteration();

const http = require('http')

const options = {
  hostname: 'localhost',
  port: 8080,
  path: '/',
  method: 'POST',
  headers: {
    'Content-Type': 'application/json'
  }
}

function nextIteration(){
	data = JSON.stringify(getRandomTrade());
	options.headers["Content-Length"] = data.length;
	req = http.request(options);
	req.write(data)
	req.end()	
	setTimeout(nextIteration,1000)
}

nextIteration();