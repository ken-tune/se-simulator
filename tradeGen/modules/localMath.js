// Round value to a specific number of decimal places
function round(value,places){
	return Math.round(value * Math.pow(10,places)) / Math.pow(10,places) ;
}

exports.round = round;