
// Imports
const stats = require("./modules/stats")
const tradeDataGen = require("./modules/tradeDataGen")
const util = require("./modules/utility")

// Constants
const ITERATIONS_PER_SECOND = util.getEnvironmentValue("ITERATIONS_PER_SECOND",1);
const TRADES_PER_ITERATION = util.getEnvironmentValue("TRADES_PER_ITERATION",1);
const DESTINATION_HOST = util.getEnvironmentValue("DESTINATION_HOST","localhost")
const DESTINATION_PORT = util.getEnvironmentValue("DESTINATION_PORT",8080)

// Stock Config
var stockConfigList=[];
stockConfigList[0] = stockConfig("A.COM",32,4,50,10000); 
stockConfigList[1] = stockConfig("B.COM",124,17,500,50000); 
stockConfigList[2] = stockConfig("C.COM",75,7,700,100000); 

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

var http = require('http')

const options = {
  hostname: DESTINATION_HOST,
  port: DESTINATION_PORT,
  path: '/',
  method: 'POST',
  headers: {
    'Content-Type': 'application/json'
  }
}

function nextIteration(){
	for(var i = 0;i<TRADES_PER_ITERATION;i++){
		var selectedStock = stockConfigList[stats.getRandomInt(stockConfigList.length)]
		var data = JSON.stringify(tradeDataGen.simulatedTrade(selectedStock));
		options.headers["Content-Length"] = data.length;
		req = http.request(options);
		req.write(data)
		req.end()	
	}
	setTimeout(nextIteration,Math.ceil(1000 / ITERATIONS_PER_SECOND))
}

nextIteration();


// function nextIteration(){
// 	console.log(getRandomTrade());
// 	setTimeout(nextIteration,1000);
// }

// nextIteration();
